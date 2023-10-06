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
        JSFunction function = object.getJSFunction(name);
        if (function == null) return null;
        result = function.call(args);
        if (result instanceof JSObject) then(result);
        return result;
    }

    private void then(Object result) {
        JSObject promise = (JSObject) result;
        JSFunction then = promise.getJSFunction("then");
        if (then != null) then.call(callback);
    }

    private final JSCallFunction callback = new JSCallFunction() {
        @Override
        public Object call(Object... args) {
            return result = args[0];
        }
    };
}
