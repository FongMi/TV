package com.fongmi.quickjs.method;

import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;

import java.util.concurrent.Callable;

public class Function implements Callable<Object[]> {

    private final JSObject jsObject;
    private final Object[] args;
    private final String name;
    private Object result;

    public static Function call(JSObject jsObject, String name, Object[] args) {
        return new Function(jsObject, name, args);
    }

    private Function(JSObject jsObject, String name, Object[] args) {
        this.jsObject = jsObject;
        this.name = name;
        this.args = args;
    }

    @Override
    public Object[] call() throws Exception {
        Object object = jsObject.getJSFunction(name).call(args);
        boolean jsObj = object instanceof JSObject;
        if (!jsObj) return new Object[]{object};
        JSObject promise = (JSObject) object;
        JSFunction then = promise.getJSFunction("then");
        if (then != null) then.call(jsCallFunction);
        return new Object[]{result};
    }

    private final JSCallFunction jsCallFunction = new JSCallFunction() {
        @Override
        public Object call(Object... args) {
            return result = args[0];
        }
    };
}
