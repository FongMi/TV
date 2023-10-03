package com.fongmi.quickjs.method;

import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;

import java.util.concurrent.Callable;

public class Function implements Callable<Object> {

    private final JSObject object;
    private final Object[] args;
    private final String name;
    private Object result;

    public static Function call(JSObject object, String name, Object[] args) {
        return new Function(object, name, args);
    }

    private Function(JSObject object, String name, Object[] args) {
        this.object = object;
        this.name = name;
        this.args = args;
    }

    @Override
    public Object call() throws Exception {
        result = object.getJSFunction(name).call(args);
        if (!(result instanceof JSObject)) return result;
        JSObject promise = (JSObject) result;
        JSFunction then = promise.getJSFunction("then");
        if (then != null) then.call(func);
        return result;
    }

    private final JSCallFunction func = new JSCallFunction() {
        @Override
        public Object call(Object... args) {
            return result = args[0];
        }
    };
}
