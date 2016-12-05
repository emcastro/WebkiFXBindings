package emcastro;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by ecastro on 04/12/16.
 */
public class WebkitFXBindings {

    /**
     * Classloader to store the proxy class
     */
    ClassLoader loader = new ClassLoader() {
        @Override
        public String toString() {
            return "ClassLoader for " + WebkitFXBindings.this;
        }
    };

    JSObject function;
    Object[] empty = new Object[0];
    String undefined;

    public WebkitFXBindings(WebEngine engine) {
        function = (JSObject) engine.executeScript("Function");
        undefined = (String) engine.executeScript("undefined");
    }

    public <T> T proxy(Class<T> type, Object o) {
        return proxy(type, (JSObject) o);
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> type, JSObject o) {
        return (T) Proxy.newProxyInstance(loader, new Class[]{type}, new JSInvocationHandler(o));
    }

    private boolean needConvert(Object arg) {
        return arg.getClass().getClassLoader() == loader;
    }

    private Object convertArgument(Object arg) {
        if (arg.getClass().getClassLoader() == loader) {
            JSInvocationHandler handler = (JSInvocationHandler) Proxy.getInvocationHandler(arg);
            return handler.jsObject;
        } else {
            return arg;
        }
    }

    private Object[] convertArguments(Object[] args) {
        if (args == null) {
            args = empty;
        }

        boolean needConvert = false;
        for (Object arg : args) {
            if (needConvert(arg)) {
                needConvert = true;
                break;
            }
        }

        if (!needConvert) return args;

        Object[] convertedArgs = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            convertedArgs[i] = convertArgument(args[i]);
        }

        return convertedArgs;
    }

    private Object convertResult(Class<?> returnType, Object value) {
        if (value == undefined // identity comparison
                && returnType != JSObject.class
                && returnType != void.class)
            throw new JSException("Undefined value return by Javascript");

        if (returnType.isAnnotationPresent(JSInterface.class)) {
            return proxy(returnType, (JSObject) value);
        } else if (returnType.isAssignableFrom(Double.class)) {
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

    public boolean FAST_CALL = false;

    private class JSInvocationHandler implements InvocationHandler {
        private final JSObject jsObject;

        public JSInvocationHandler(JSObject jsObject) {
            this.jsObject = jsObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();

            if (method.isAnnotationPresent(Getter.class)) {
                checkArity(args, 0);

                String name;
                if (methodName.startsWith("get")) {
                    name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                } else if (methodName.startsWith("is")) {
                    name = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                } else {
                    name = methodName;
                }

                return convertResult(returnType, jsObject.getMember(name));
            } else if (method.isAnnotationPresent(Setter.class)) {
                checkArity(args, 1);

                String name;
                if (methodName.startsWith("set")) {
                    name = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                } else {
                    name = methodName;
                }
                jsObject.setMember(name, convertArgument(args[0]));

                return null;
            } else {
                // Method call
                Object[] convertArguments = convertArguments(args);
                Object jsResult;
                if (FAST_CALL) {
                    jsResult = jsObject.call(methodName, convertArguments);
                } else {
                    Object jsMethod = jsObject.getMember(methodName);
                    if (jsMethod == undefined) {// identity comparison
                        throw new JSException("Method " + methodName + " not found");
                    } else {
                        Object[] extendedArgs = new Object[convertArguments.length + 1];

                        extendedArgs[0] = jsObject;
                        int i = 1;
                        for (Object arg : convertArguments) {
                            extendedArgs[i++] = arg;
                        }

                        jsResult = ((JSObject) jsMethod).call("call", extendedArgs);
                    }
                }
                return convertResult(returnType, jsResult);
            }
        }
    }
}
