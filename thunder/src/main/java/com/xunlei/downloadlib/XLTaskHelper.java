package com.xunlei.downloadlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
import com.xunlei.downloadlib.parameter.GetDownloadLibVersion;
import com.xunlei.downloadlib.parameter.GetFileName;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.InitParam;
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLConstant;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oceanzhang on 2017/7/27.
 */

public class XLTaskHelper {
    @SuppressLint("StaticFieldLeak")
    private static XLDownloadManager mXlDownloadManager;

    public static void init(Context context, String a, String b) {
        if(mXlDownloadManager != null){
            return;
        }
        mXlDownloadManager = XLDownloadManager.getInstance();
        InitParam initParam = new InitParam();
        initParam.mAppKey = a;
        initParam.mAppVersion = b;

        initParam.mStatSavePath = context.getFilesDir().getPath();
        initParam.mStatCfgSavePath = context.getFilesDir().getPath();
        initParam.mPermissionLevel = 1;
        initParam.mQueryConfOnInit = 0;
        mXlDownloadManager.init(context, initParam);
        mXlDownloadManager.setStatReportSwitch(false);

        GetDownloadLibVersion getDownloadLibVersion = new GetDownloadLibVersion();
        mXlDownloadManager.getDownloadLibVersion(getDownloadLibVersion);

        mXlDownloadManager.setOSVersion(Build.VERSION.INCREMENTAL + "_alpha");
        mXlDownloadManager.setSpeedLimit(-1, -1);

    }


    private AtomicInteger seq = new AtomicInteger(0);

    private XLTaskHelper() {
    }

    private static volatile XLTaskHelper instance = null;

    public static XLTaskHelper instance() {
        if (instance == null) {
            synchronized (XLTaskHelper.class) {
                if (instance == null) {
                    instance = new XLTaskHelper();
                }
            }

        }
        return instance;
    }


    /**
     * 添加迅雷链接任务 支持thunder:// ftp:// ed2k:// http:// https:// 协议
     *
     * @param url
     * @param savePath 下载文件保存路径
     * @param fileName 下载文件名 可以通过 getFileName(url) 获取到,为空默认为getFileName(url)的值
     * @return
     */
    public synchronized long addThunderTask(String url, String savePath, @Nullable String fileName) {
        if (url.startsWith("thunder://")) url = mXlDownloadManager.parserThunderUrl(url);
        final GetTaskId getTaskId = new GetTaskId();
        if (TextUtils.isEmpty(fileName)) {
            GetFileName getFileName = new GetFileName();
            int code = mXlDownloadManager.getFileNameFromUrl(url, getFileName);
            if (code != XLConstant.XLErrorCode.NO_ERROR) {
                return -1;
            }
            fileName = getFileName.getFileName();
        }
        if (url.startsWith("ftp://") || url.startsWith("http://") || url.startsWith("https://")) {
            P2spTaskParam taskParam = new P2spTaskParam();
            taskParam.setCreateMode(1);
            taskParam.setFileName(fileName);
            taskParam.setFilePath(savePath);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCookie("");
            taskParam.setRefUrl("");
            taskParam.setUser("");
            taskParam.setPass("");
            int code = mXlDownloadManager.createP2spTask(taskParam, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) {
                return -1;
            }
            mXlDownloadManager.setDownloadTaskOrigin(getTaskId.getTaskId(), "out_app/out_app_paste");
            mXlDownloadManager.setOriginUserAgent(getTaskId.getTaskId(), "AndroidDownloadManager/5.41.2.4980 (Linux; U; Android 4.4.4; Build/KTU84Q)");
            addRequestHeadersToXlEngine(getTaskId.getTaskId());

        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam taskParam = new EmuleTaskParam();
            taskParam.setFilePath(savePath);
            taskParam.setFileName(fileName);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCreateMode(1);
            int code = mXlDownloadManager.createEmuleTask(taskParam, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) {
                return -1;
            }
        }
        mXlDownloadManager.startTask(getTaskId.getTaskId());
        mXlDownloadManager.setTaskGsState(getTaskId.getTaskId(), 0, 2);
        return getTaskId.getTaskId();
    }

    private final List<Pair<String, String>> mRequestHeaders = new ArrayList<>();

    public void addHeader(String key, String str2) {
        mRequestHeaders.add(Pair.create(key, str2));
    }

    private Collection<Pair<String, String>> getHeaders() {
        return Collections.unmodifiableList(this.mRequestHeaders);
    }

    private void addRequestHeadersToXlEngine(long j) {
        if (mXlDownloadManager != null) {
            for (Pair<String, String> pair : this.getHeaders()) {
                if (!(pair.first == null || pair.second == null)) {
                    mXlDownloadManager.setHttpHeaderProperty(j, pair.first, pair.second);
                }
            }
        }
    }
    /**
     * 通过链接获取文件名
     *
     * @param url
     * @return
     */
    public synchronized String getFileName(String url) {
        if (url.startsWith("thunder://")) url = mXlDownloadManager.parserThunderUrl(url);
        GetFileName getFileName = new GetFileName();
        mXlDownloadManager.getFileNameFromUrl(url, getFileName);
        return getFileName.getFileName();
    }

    /**
     * 添加磁力链任务
     *
     * @param url      磁力链接 magnet:? 开头
     * @param savePath
     * @param fileName
     * @return
     * @throws Exception
     */
    public synchronized long addMagentTask(final String url, final String savePath, String fileName) throws Exception {
        if (url.startsWith("magnet:?")) {
            if (TextUtils.isEmpty(fileName)) {
                final GetFileName getFileName = new GetFileName();
                mXlDownloadManager.getFileNameFromUrl(url, getFileName);
                fileName = getFileName.getFileName();
            }
            MagnetTaskParam magnetTaskParam = new MagnetTaskParam();
            magnetTaskParam.setFileName(fileName);
            magnetTaskParam.setFilePath(savePath);
            magnetTaskParam.setUrl(url);
            final GetTaskId getTaskId = new GetTaskId();
            int code = mXlDownloadManager.createBtMagnetTask(magnetTaskParam, getTaskId);
            if (code != XLConstant.XLErrorCode.NO_ERROR) {
                return -1;
            }
            mXlDownloadManager.startTask(getTaskId.getTaskId());
            mXlDownloadManager.setTaskGsState(getTaskId.getTaskId(), 0, 2);
            return getTaskId.getTaskId();
        } else {
            throw new Exception("url illegal.");
        }
    }

    /**
     * 获取种子详情
     *
     * @param torrentPath
     * @return
     */
    public synchronized TorrentInfo getTorrentInfo(String torrentPath) {
        TorrentInfo torrentInfo = new TorrentInfo();
        mXlDownloadManager.getTorrentInfo(torrentPath, torrentInfo);
        return torrentInfo;
    }


    /**
     * 添加种子下载任务,如果是磁力链需要先通过addMagentTask将种子下载下来
     *
     * @param torrentPath 种子地址
     * @param savePath    保存路径
     * @param index       需要下载的文件索引
     * @return
     * @throws Exception
     */
    public synchronized long addTorrentTask(String torrentPath, String savePath, int index) {
        TorrentInfo torrentInfo = new TorrentInfo();
        mXlDownloadManager.getTorrentInfo(torrentPath, torrentInfo);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setFilePath(savePath);
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(seq.incrementAndGet());
        taskParam.setTorrentPath(torrentPath);
        GetTaskId getTaskId = new GetTaskId();
        int code = mXlDownloadManager.createBtTask(taskParam, getTaskId);
        if (code != XLConstant.XLErrorCode.NO_ERROR) {
            return -1;
        }
        if (fileInfos.length > 1) {
            List<Integer> list = new CopyOnWriteArrayList<>();
            for (TorrentFileInfo fileInfo : fileInfos) {
                if (fileInfo.mFileIndex != index) {
                    list.add(fileInfo.mFileIndex);
                }
            }
            BtIndexSet btIndexSet = new BtIndexSet(list.size());
            for (int i = 0; i < list.size(); i++) {
                btIndexSet.mIndexSet[i] = list.get(i);
            }
            mXlDownloadManager.deselectBtSubTask(getTaskId.getTaskId(), btIndexSet);//XLConstant.XLErrorCode
        }

        mXlDownloadManager.startTask(getTaskId.getTaskId());
        mXlDownloadManager.setTaskGsState(getTaskId.getTaskId(), index, 2);
        return getTaskId.getTaskId();
    }

    /**
     * 获取某个文件的本地proxy url,如果是音视频文件可以实现变下边播
     *
     * @param filePath
     * @return
     */
    public synchronized String getLoclUrl(String filePath) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        mXlDownloadManager.getLocalUrl(filePath, localUrl);
        return localUrl.mStrUrl;
    }


    /**
     * 删除一个任务，会把文件也删掉
     *
     * @param taskId
     * @param savePath
     */
    public synchronized void deleteTask(long taskId, final String savePath) {
        stopTask(taskId);
        if (TextUtils.isEmpty(savePath)) return;
        new Handler(Daemon.looper()).post(() -> {
            try {
                new LinuxFileCommand(Runtime.getRuntime()).deleteDirectory(savePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 停止任务 文件保留
     *
     * @param taskId
     */
    public synchronized void stopTask(long taskId) {
        mXlDownloadManager.stopTask(taskId);
        mXlDownloadManager.releaseTask(taskId);
    }


    /**
     * 获取任务详情， 包含当前状态，下载进度，下载速度，文件大小
     * mDownloadSize:已下载大小  mDownloadSpeed:下载速度 mFileSize:文件总大小 mTaskStatus:当前状态，0连接中1下载中 2下载完成 3失败 mAdditionalResDCDNSpeed DCDN加速 速度
     *
     * @param taskId
     * @return
     */
    public synchronized XLTaskInfo getTaskInfo(long taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        mXlDownloadManager.getTaskInfo(taskId, 1, taskInfo);
        return taskInfo;
    }

    /**
     * 获取种子文件子任务的详情
     *
     * @param taskId
     * @param fileIndex
     * @return
     */
    public synchronized BtSubTaskDetail getBtSubTaskInfo(long taskId, int fileIndex) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
         mXlDownloadManager.getBtSubTaskInfo(taskId, fileIndex, subTaskDetail);
        return subTaskDetail;
    }

}
