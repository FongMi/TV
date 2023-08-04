package com.xunlei.downloadlib.parameter;

public class ErrorCode {

    public static String get(int code) {
        switch (code) {
            case 9125:
            case 111120:
                return "檔案名稱太長";
            case 9301:
            case 111085:
                return "儲存空間不足";
            case 9304:
            case 114001:
            case 114004:
            case 114005:
            case 114006:
            case 114007:
            case 114011:
            case 111154:
                return "版權限制";
            case 114101:
                return "已失效";
            default:
                return "ErrorCode=" + code;
        }
    }
}
