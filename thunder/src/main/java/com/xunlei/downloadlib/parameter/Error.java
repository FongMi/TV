package com.xunlei.downloadlib.parameter;

public class Error {

    public static String get(int code) {
        switch (code) {
            case 9125:
                return "檔案名稱太長";
            case 9301:
                return "緩衝區不足";
            case 111085:
                return "硬碟空間不足";
            case 111120:
                return "檔案路徑太長";
            case 111142:
                return "檔案大小太小";
            case 111171:
                return "網路連線失敗";
            case 9304:
            case 114001:
            case 114004:
            case 114005:
            case 114006:
            case 114007:
            case 114011:
            case 111154:
                return "版權限制：無法下載";
            case 114101:
                return "地址失效";
            default:
                return "ErrorCode=" + code;
        }
    }
}
