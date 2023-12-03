package com.fongmi.android.tv.model

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import com.fongmi.android.tv.Setting
import com.fongmi.android.tv.db.AppDatabase
import com.fongmi.android.tv.impl.Callback
import com.github.catvod.Init
import com.github.catvod.utils.Path
import com.hua.webdav.Authorization
import com.hua.webdav.WebDav
import com.hua.webdav.utils.ZipUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

class BackupViewModel : ViewModel() {

    private val job = SupervisorJob()

    private val globalScope = CoroutineScope(job)

    private val crash = CoroutineExceptionHandler { _, throwable ->
        Log.e("TAG", "throwable: $throwable")
        throwable.printStackTrace()
        throwable.message?.showToast()
    }

    private val _backupTimeFlow = MutableStateFlow<Long?>(null)
    val backupTimeFlow = _backupTimeFlow.asStateFlow()

    private val _backupChannel = Channel<Resource<Unit>>()
    val backupChannel = _backupChannel.receiveAsFlow()

    private val _restoreChannel = Channel<Resource<Unit>>()
    val restoreChannel = _restoreChannel.receiveAsFlow()


    private fun String.showToast() {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(Init.context(), this@showToast, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun backupWithResult(): Boolean {
        val result = withTimeoutOrNull(10 * 1000L) {
            suspendCancellableCoroutine {
                AppDatabase.backup(object : Callback() {
                    override fun success() {
                        super.success()
                        it.resume(true)
                    }
                })
            }
        }
        return result ?: false
    }

    fun backup(webDavUrl: String, authorization: Authorization) {
        globalScope.launch(crash) {
            val backupResult = backupWithResult()
            if (!backupResult) {
                _backupChannel.send(Resource.Error(errorMsg = "备份失败"))
                return@launch
            }
            checkRightAndRunnable(webDavUrl, authorization) {
                val fileList = Path.tv().listFiles()?.filter { !it.name.endsWith(".zip") }?.map {
                    it.absolutePath
                } ?: emptyList()
                if (fileList.isEmpty()) {
                    return@launch
                }
                ZipUtils.zipFiles(fileList, File(Path.tv(), backupName).absolutePath)
                val backupFile = File(Path.tv(), backupName)
                val webDav = WebDav("${webDavUrl.backupDirUrl}/$backupName", authorization)
                webDav.upload(backupFile.absolutePath)
                _backupChannel.send(Resource.Success(Unit))
                _backupTimeFlow.emit(backupFile.lastModified())
            }
        }
    }

    private val backupName = "backup.zip"

    fun restore(webDavUrl: String, authorization: Authorization) {
        globalScope.launch(crash) {
            if (Path.tv().exists().not()) Path.tv().mkdir()
            checkRightAndRunnable(webDavUrl, authorization) {
                val backupFile = WebDav("${webDavUrl.backupDirUrl}/$backupName", authorization)
                val time = backupFile.getWebDavFile()?.lastModify
                val fileName = "backup_file_${System.currentTimeMillis()}.zip"
                val zipFile = File(Path.tv(), fileName)
                backupFile.downloadTo(zipFile.absolutePath, true)
                Path.tv().listFiles()?.forEach {
                    if (!it.name.equals(fileName)) it.delete()
                }
                ZipUtils.unZipToPath(zipFile, Path.tv())
                val restoreResult = restoreWithResult()
                if (restoreResult) {
                    _backupTimeFlow.emit(time)
                    _restoreChannel.send(Resource.Success(Unit))
                } else {
                    _restoreChannel.send(Resource.Error(errorMsg = "恢复失败"))
                }
            }
        }
    }

    private suspend fun restoreWithResult(): Boolean {
        val result = withTimeoutOrNull(10 * 1000L) {
            suspendCancellableCoroutine {
                AppDatabase.restore(object : Callback() {
                    override fun success() {
                        super.success()
                        it.resume(true)
                    }
                })
            }
        }
        return result ?: false
    }

    private suspend inline fun checkRightAndRunnable(
        webDavUrl: String,
        authorization: Authorization,
        runnable: () -> Unit
    ) {
        val webDav = WebDav(webDavUrl.backupDirUrl, authorization)
        if (!webDav.exists()) {
            webDav.makeAsDir()
        }
        if (webDav.check()) {
            Setting.setWebDav(
                webDavUrl,
                authorization.username,
                authorization.password
            )
            runnable()
        } else {
            "请检查是否输入正确".showToast()
        }
    }

    private val String.backupDirUrl: String  get() = "${this}tv"


    fun getBackupTime(webDavUrl: String, authorization: Authorization) {
        globalScope.launch(crash) {
            val isBlank = authorization.username.isBlank() || authorization.password.isBlank()
            if (isBlank) {
                _backupTimeFlow.emit(null)
                return@launch
            }
            checkRightAndRunnable(webDavUrl, authorization) {
                val webDav = WebDav(webDavUrl.backupDirUrl, authorization)
                val backupFile = webDav.listFiles().firstOrNull() ?: kotlin.run {
                    _backupTimeFlow.emit(null)
                    return@launch
                }
                _backupTimeFlow.emit(backupFile.lastModify)
            }
        }
    }



}

sealed class Resource<T> {

    data class Success<T>(val data: T) : Resource<T>()

    data class Error<T>(val errorCode: Int = -1, val errorMsg: String = "") : Resource<T>()

    data class Loading<T>(val data: T? = null): Resource<T>()

}

inline fun <T> Resource<T>.onLoading(block: () -> Unit) {
    if (this is Resource.Loading) {
        block.invoke()
    }
}

inline fun <T> Resource<T>.onSuccess(block: (T) -> Unit) {
    if (this is Resource.Success) {
        block.invoke(this.data)
    }
}

inline fun <T> Resource<T>.onError(block: (code: Int, msg: String) -> Unit) {
    if (this is Resource.Error) {
        block.invoke(this.errorCode, this.errorMsg)
    }
}