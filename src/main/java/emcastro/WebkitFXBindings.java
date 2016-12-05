package emcastro;

import javafx.scene.web.WebEngine;
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

    public WebkitFXBindings(WebEngine engine) {
        function = (JSObject) engine.executeScript("Function");
    }

    public <T> T proxy(Class<T> type, Object o) {
        return proxy(type, (JSObject) o);
    }

    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> type, JSObject o) {
        return (T) Proxy.newProxyInstance(loader, new Class[]{type}, new MyInvocationHandler(o));
    }

    private boolean needConvert(Object arg) {
        return arg.getClass().getClassLoader() == loader;
    }

    private Object convertArgument(Object arg) {
        if (arg.getClass().getClassLoader() == loader) {
            MyInvocationHandler handler = (MyInvocationHandler) Proxy.getInvocationHandler(arg);
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

    private class MyInvocationHandler implements InvocationHandler {
        private final JSObject jsObject;

        public MyInvocationHandler(JSObject jsObject) {
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

                Object o1 = WebkitFXBindings.this.convertResult(returnType, jsObject.getMember(name));
                return o1;
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
                return convertResult(returnType, jsObject.call(methodName, convertArguments(args)));
            }
        }
    }
}
