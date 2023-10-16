package com.whl.quickjs.wrapper;

public class JSFunction extends JSObject {

    private final long objPointer;

    public JSFunction(QuickJSContext context, long objPointer, long pointer) {
        super(context, pointer);
        this.objPointer = objPointer;
    }

    public Object call(Object... args) {
        return getContext().call(this, objPointer, args);
    }
}
