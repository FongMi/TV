package com.whl.quickjs.wrapper;

public class JSArray extends JSObject {

    public JSArray(QuickJSContext context, long pointer) {
        super(context, pointer);
    }

    public int length() {
        checkReleased();
        return getContext().length(this);
    }

    public Object get(int index) {
        checkReleased();
        return getContext().get(this, index);
    }

    public void set(Object value, int index) {
        checkReleased();
        getContext().set(this, value, index);
    }
}
