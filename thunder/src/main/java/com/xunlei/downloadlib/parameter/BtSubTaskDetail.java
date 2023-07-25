package com.xunlei.downloadlib.parameter;

public class BtSubTaskDetail {

    public int mFileIndex;
    public boolean mIsSelect;
    public XLTaskInfo mTaskInfo = new XLTaskInfo();

    public int getFileIndex() {
        return mFileIndex;
    }

    public boolean isSelect() {
        return mIsSelect;
    }

    public XLTaskInfo getTaskInfo() {
        return mTaskInfo;
    }
}
