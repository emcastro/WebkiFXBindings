package emcastro;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.net.URL;

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

    private final JSObject function;
    private final Object[] empty = new Object[0];
    private final String undefined;
    private final WebEngine engine;

    public WebkitFXBindings(WebEngine engine) {
        function = (JSObject) engine.executeScript("Function");
        undefined = (String) engine.executeScript("undefined");
        this.engine = engine;
    }

    public <T> T executeScript(Class<T> type, URL script) throws IOException {
        return executeScript(type, script.openStream());
    }

    public <T> T executeScript(Class<T> type, InputStream script) throws IOException {
        StringBuilder b = new StringBuilder();

        char[] buffer = new char[1000];
        InputStreamReader reader = new InputStreamReader(script);
        int sz;
        while ((sz = reader.read(buffer)) != -1) {
            b.append(buffer, 0, sz);
        }
        reader.close();

        return executeScript(type, b.toString());
    }

    public <T> T executeScript(Class<T> type, String script) {
        return proxy(type, (JSObject) engine.executeScript(script));
    }

    public <T> T proxy(Class<T> type, JSObject o) {
        return proxy((Type) type, o);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Type type, JSObject o) {
        Class<?> clazz = getClass(type);

        if (clazz.isAnnotationPresent(JSInterface.class)) {
            return (T) Proxy.newProxyInstance(loader, new Class[]{clazz}, new JSInvocationHandler(o, type));
        } else {
            throw new IllegalArgumentException("The type argument must be an interface with the @JSInterface annotation: " + type);
        }
    }

    private static Class<?> getClass(Type type) {
        Class<?> clazz;
        if (type instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) type).getRawType();
        } else {
            clazz = (Class) type;
        }
        return clazz;
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


    private Object convertResult(Type returnType, Object value) {
        Class<?> returnClass = getClass(returnType);

        if (value == undefined // identity comparison
                && returnClass != JSObject.class
                && returnClass != void.class
                && returnClass != Void.class)
            throw new JSException("Undefined value return by Javascript");

        if (returnClass.isAnnotationPresent(JSInterface.class)) {
            return proxy(returnType, (JSObject) value);
        } else if (returnClass.isAssignableFrom(Double.class)) {
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
        private final Type type;

        public JSInvocationHandler(JSObject jsObject, Type type) {
            this.jsObject = jsObject;
            this.type = type;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            Type returnType = method.getGenericReturnType();

            if (returnType instanceof TypeVariable) {
                // Resolve the type variable
                TypeVariable[] typeParameters = ((Class) ((ParameterizedType) type).getRawType()).getTypeParameters();
                int i;
                for (i = 0; i < typeParameters.length; i++) {
                    if (typeParameters[i].equals(returnType)) break;
                }
                if (i == typeParameters.length) throw new AssertionError("Type parameter not found");

                returnType = ((ParameterizedType) type).getActualTypeArguments()[i];
            }


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
