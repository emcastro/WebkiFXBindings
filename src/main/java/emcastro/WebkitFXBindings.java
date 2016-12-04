package emcastro;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import java.lang.reflect.Proxy;

/**
 * Created by ecastro on 04/12/16.
 */
public class WebkitFXBindings {

    /**
     * Classloader to store the porxy classe
     */
    ClassLoader loader = new ClassLoader() {

    };

    JSObject function;
    Object[] empty = new Object[0];

    public WebkitFXBindings(WebEngine engine) {
        function = (JSObject) engine.executeScript("Function");
    }

    public <T> T proxy(Class<T> type, Object o) {
        return proxy(type, (JSObject) o);
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> type, JSObject o) {

        return (T) Proxy.newProxyInstance(loader, new Class[]{type}, (proxy, method, args) -> {

            Class<?> returnType = method.getReturnType();

            Object value = o.getMember(method.getName());

            if (returnType.isAnnotationPresent(JSInterface.class)) {
                return null;
            } else {
                if (value instanceof JSObject) {
                    if (returnType.isAssignableFrom(JSObject.class)) {
                        return value;
                    }
                    // We suppose that it is a function
                    if (args == null) {
                        args = empty;
                    }

                    return convertResult(returnType, o.call(method.getName(), args));
                } else {
                    checkArity(args, 0);
                    return convertResult(returnType, value);
                }
            }

        });

    }

    private Object convertResult(Class<?> returnType, Object value) {
        if (returnType.isAssignableFrom(Double.class)) {
            // convert integer to Double
            if (value instanceof Integer) {
                return ((Integer) value).doubleValue();
            } else return value;
        } else {
            return value;
        }
    }

    public static void checkArity(Object[] args, int arity) {
        if (args == null) {
            if (arity != 0) {
                throw new IllegalArgumentException();
            }
            return;
        }

        if (args.length != arity) {
            throw new IllegalArgumentException();
        }
    }

}
