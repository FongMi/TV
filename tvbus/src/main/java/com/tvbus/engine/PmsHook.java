package com.tvbus.engine;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class PmsHook implements InvocationHandler {

    private byte[][] sign;
    private String name;
    private Object base;

    public static void inject() {
        new PmsHook().hook();
    }

    private Context getContext() throws Throwable {
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private byte[][] getSign(Context context) throws Throwable {
        DataInputStream is = new DataInputStream(new ByteArrayInputStream(Base64.decode(context.getString(R.string.data), Base64.DEFAULT)));
        byte[][] sign = new byte[is.read() & 0xFF][];
        for (int i = 0; i < sign.length; i++) is.readFully(sign[i] = new byte[is.readInt()]);
        return sign;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!method.getName().equals("getPackageInfo")) return method.invoke(base, args);
        String pkg = (String) args[0];
        int flag = (Integer) args[1];
        if (flag != 64 || !name.equals(pkg)) return method.invoke(base, args);
        PackageInfo info = (PackageInfo) method.invoke(base, args);
        info.signatures = new Signature[this.sign.length];
        for (int i = 0; i < info.signatures.length; i++) info.signatures[i] = new Signature(this.sign[i]);
        return info;
    }
}
