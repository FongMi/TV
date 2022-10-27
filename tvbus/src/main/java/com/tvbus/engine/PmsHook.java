package com.tvbus.engine;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class PmsHook implements InvocationHandler {

    private static final int GET_SIGNATURES = 0x00000040;

    private byte[][] sign;
    private String name;
    private Object base;

    public static void inject() {
        new PmsHook().hook();
    }

    private Context getContext() throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Method method = activityThreadClass.getMethod("currentApplication");
        return (Context) method.invoke(null);
    }

    private void hook() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);
            sPackageManagerField.setAccessible(true);
            this.sign = getSign(getContext());
            this.name = getContext().getPackageName();
            this.base = sPackageManagerField.get(currentActivityThread);
            Object proxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader(), new Class<?>[]{iPackageManagerInterface}, this);
            sPackageManagerField.set(currentActivityThread, proxy);
            PackageManager pm = getContext().getPackageManager();
            Field mPmField = pm.getClass().getDeclaredField("mPM");
            mPmField.setAccessible(true);
            mPmField.set(pm, proxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[][] getSign(Context context) throws IOException {
        DataInputStream is = new DataInputStream(new ByteArrayInputStream(Base64.decode(context.getString(R.string.data), Base64.DEFAULT)));
        byte[][] sign = new byte[is.read() & 0xFF][];
        for (int i = 0; i < sign.length; i++) {
            sign[i] = new byte[is.readInt()];
            is.readFully(sign[i]);
        }
        return sign;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getPackageInfo")) {
            String pkgName = (String) args[0];
            Integer flag = (Integer) args[1];
            if ((flag & GET_SIGNATURES) != 0 && this.name.equals(pkgName)) {
                PackageInfo info = (PackageInfo) method.invoke(base, args);
                info.signatures = new Signature[this.sign.length];
                for (int i = 0; i < info.signatures.length; i++) {
                    info.signatures[i] = new Signature(this.sign[i]);
                }
                return info;
            }
        }
        return method.invoke(base, args);
    }
}
