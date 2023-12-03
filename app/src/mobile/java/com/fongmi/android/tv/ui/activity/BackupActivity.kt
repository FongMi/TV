package com.fongmi.android.tv.ui.activity

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.Log
import androidx.viewbinding.ViewBinding
import com.fongmi.android.tv.Setting
import com.fongmi.android.tv.bean.RestoreEvent
import com.fongmi.android.tv.bean.TimeEvent
import com.fongmi.android.tv.databinding.ActivityBackupBinding
import com.fongmi.android.tv.db.AppDatabase
import com.fongmi.android.tv.model.BackupViewModel
import com.fongmi.android.tv.model.Resource
import com.fongmi.android.tv.model.onError
import com.fongmi.android.tv.model.onSuccess
import com.fongmi.android.tv.ui.base.BaseActivity
import com.github.catvod.Init
import com.github.catvod.utils.Path
import com.hua.webdav.Authorization
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

class BackupActivity: BaseActivity() {

    private val crash = CoroutineExceptionHandler { _, throwable ->
        Log.e("TAG", "throwable: $throwable")
        throwable.printStackTrace()
        throwable.message?.showToast()
    }

    private val viewModel: BackupViewModel by viewModels()

    private val binding: ActivityBackupBinding by lazy { ActivityBackupBinding.inflate(layoutInflater) }

    override fun getBinding(): ViewBinding {
        return binding
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        initWebDavText()
        initObserver()
        binding.btnBackup.setOnClickListener(this::backup)
        binding.btnRestore.setOnClickListener(this::restore)
        viewModel.getBackupTime(webDavUrl, authorization)
    }

    private fun initObserver() {
        viewModel.backupChannel.flowObserve(this) {
            Log.e("TAG", "backup: $it")
            switchBtnEnable(it !is Resource.Loading)
            it.onError { _, msg ->
                msg.showToast()
            }
            it.onSuccess {
                "备份成功".showToast()
            }
        }
        viewModel.restoreChannel.flowObserve(this) {
            Log.e("TAG", "restore: $it")
            switchBtnEnable(it !is Resource.Loading)
            it.onError { _, msg ->
                msg.showToast()
            }
            it.onSuccess {
                "恢复成功".showToast()
                EventBus.getDefault().post(RestoreEvent())
            }
        }

        viewModel.backupTimeFlow.flowObserve(this) { time ->
            binding.tvOnlineInfo.text = getOnlineBackupTime(time)
            binding.tvInfo.text = "本地最后备份时间为: ${AppDatabase.getDate()}"
            time?.let { EventBus.getDefault().post(TimeEvent(it))  }
        }
    }

    private fun backup(view: View?) {
        lifecycleScope.launch(crash) {
            if (canWrite().not()) {
                "请授予存储权限".showToast()
                return@launch
            }
            viewModel.backup(webDavUrl, authorization)
        }
    }

    private fun restore(view: View?) {
        lifecycleScope.launch(crash) {
            if (canWrite().not()) {
                "请授予存储权限".showToast()
                return@launch
            }
            if (Path.tv().exists().not()) Path.tv().mkdir()
            viewModel.restore(webDavUrl, authorization)
        }
    }

    private fun initWebDavText() {
        val (webDavUrl, user, password) = Setting.getWebWav()
        binding.etUrl.setText(webDavUrl.ifBlank { "https://dav.jianguoyun.com/dav/" })
        binding.etUser.setText(user)
        binding.etPassword.setText(password)
    }


    private val webDavUrl: String get() = binding.etUrl.text.toString()

    private val authorization get() = Authorization(binding.etUser.text.toString(), binding.etPassword.text.toString())

    private fun switchBtnEnable(enable: Boolean) {
        runOnUiThread {
            binding.btnBackup.isEnabled = enable
            binding.btnRestore.isEnabled = enable
        }
    }

    private suspend fun canWrite(): Boolean {
        return suspendCancellableCoroutine {
            runOnUiThread {
                PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .request { allGranted: Boolean, _: List<String?>?, _: List<String?>? ->
                        it.resume(allGranted)
                    }
            }
        }
    }

    private fun String.showToast() {
        runOnUiThread {
            Toast.makeText(Init.context(), this, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getOnlineBackupTime(time: Long?): String {
        val timeString = if (time == null)
            "None" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(time)
        return "在线备份: $timeString"
    }

}

fun <T> Flow<T>.flowObserve(
    lifecycle: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: (T) -> Unit
) {

    lifecycle.lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(state) {
            this@flowObserve.collect(block)
        }
    }

}