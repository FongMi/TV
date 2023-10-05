package com.whl.quickjs.wrapper;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class JSObject {

    private final QuickJSContext context;
    private final long pointer;

    private boolean isReleased;

    public JSObject(QuickJSContext context, long pointer) {
        this.context = context;
        this.pointer = pointer;
    }

    public long getPointer() {
        return pointer;
    }

    public QuickJSContext getContext() {
        return context;
    }

    public Object getProperty(String name) {
        checkReleased();
        return context.getProperty(this, name);
    }

    public void setProperty(String name, String value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, int value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, long value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, JSObject value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, boolean value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, double value) {
        context.setProperty(this, name, value);
    }

    public void setProperty(String name, JSCallFunction value) {
        context.setProperty(this, name, value);
    }

    /**
     * Class 添加 {@link JSMethod} 的方法会被注入到 JSContext 中
     * 注意：该方法暂不支持匿名内部类的注册，因为匿名内部类构造参数不是无参的，newInstance 时会报错
     *
     * @param name
     * @param clazz
     */
    public void setProperty(String name, Class<?> clazz) {
        Object javaObj = null;
        try {
            javaObj = clazz.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        if (javaObj == null) {
            throw new NullPointerException("The JavaObj cannot be null. An error occurred in newInstance!");
        }

        JSObject jsObj = context.createNewJSObject();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(JSMethod.class)) {
                Object finalJavaObj = javaObj;
                jsObj.setProperty(method.getName(), args -> {
                    try {
                        return method.invoke(finalJavaObj, args);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }
        }

        setProperty(name, jsObj);
    }

    public String getString(String name) {
        Object value = getProperty(name);
        return value instanceof String ? (String) value : null;
    }

    public Integer getInteger(String name) {
        Object value = getProperty(name);
        return value instanceof Integer ? (Integer) value : null;
    }

    public Boolean getBoolean(String name) {
        Object value = getProperty(name);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    public Double getDouble(String name) {
        Object value = getProperty(name);
        return value instanceof Double ? (Double) value : null;
    }

    public Long getLong(String name) {
        Object value = getProperty(name);
        return value instanceof Long ? (Long) value : null;
    }

    public JSObject getJSObject(String name) {
        Object value = getProperty(name);
        return value instanceof JSObject ? (JSObject) value : null;
    }

    public JSFunction getJSFunction(String name) {
        Object value = getProperty(name);
        return value instanceof JSFunction ? (JSFunction) value : null;
    }

    public JSArray getJSArray(String name) {
        Object value = getProperty(name);
        return value instanceof JSArray ? (JSArray) value : null;
    }

    public JSArray getNames() {
        JSFunction getOwnPropertyNames = (JSFunction) context.evaluate("Object.getOwnPropertyNames");
        return (JSArray) getOwnPropertyNames.call(this);
    }

    /**
     * JSObject 确定不再使用后，调用该方法可主动释放对 JS 对象的引用。
     * 注意：该方法不能调用多次以及释放后不能再被使用对应的 JS 对象。
     */
    public void release() {
        checkReleased();
        context.freeValue(this);
        isReleased = true;
    }

    public void hold() {
        context.hold(this);
    }

    /**
     * 这里与 JavaScript 的 toString 方法保持一致
     * 返回结果参考：https://262.ecma-international.org/14.0/#sec-tostring
     *
     * @return toString in JavaScript.
     */
    @NonNull
    @Override
    public String toString() {
        checkReleased();
        JSFunction toString = getJSFunction("toString");
        return (String) toString.call();
    }

    public String stringify() {
        return context.stringify(this);
    }

    final void checkReleased() {
        if (isReleased) {
            throw new NullPointerException("This JSObject was Released, Can not call this!");
        }
    }

    public boolean isAlive() {
        return context.isLiveObject(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSObject jsObject = (JSObject) o;
        return pointer == jsObject.pointer;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new long[]{pointer});
    }
}
