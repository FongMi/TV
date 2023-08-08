package com.fongmi.hook;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class Handler implements InvocationHandler {

    private String sign;
    private Object base;
    private String pkg;

    public Handler(Object base, String sign, String pkg) {
        try {
            this.base = base;
            this.sign = sign;
            this.pkg = pkg;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("getPackageInfo".equals(method.getName())) {
            String pkgName = (String) args[0];
            Number flag = (Number) args[1];
            if (flag.intValue() == 64 && this.pkg.equals(pkgName)) {
                PackageInfo info = (PackageInfo) method.invoke(this.base, args);
                info.signatures[0] = new Signature(sign);
                return info;
            }
        }
        return method.invoke(this.base, args);
    }
}