package com.github.emcastro.webkitfxproxy;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.net.URL;

/**
 * Created by ecastro on 04/12/16.
 */
public class WebkitFXProxy {

    /**
     * Classloader to store the proxy class
     */
    ClassLoader loader = new ClassLoader() {
        @Override
        public String toString() {
            return "ClassLoader for " + WebkitFXProxy.this;
        }
    };

    private final Object[] empty = new Object[0];
    private final ParameterType[] emptyParamTypes = new ParameterType[0];
    private final String undefined;
    final JSObject java2js;
    final JSObject newArray;
    final WebEngine engine;

    public WebkitFXProxy(WebEngine engine) {
        undefined = (String) engine.executeScript("undefined");
        java2js = (JSObject) engine.executeScript("" +
                "function WebkitFXBinding_java2js(javaFunction) {" +
                "   function javaCall() {" +
                "       return javaFunction.invoke(this, arguments);" +
                "   }" +
                "   return javaCall;" +
                "}" +
                "" +
                "WebkitFXBinding_java2js");
        newArray = (JSObject) engine.executeScript("" +
                "function WebkitFXBinding_newArray() {" +
                "   return [];" +
                "}" +
                "" +
                "WebkitFXBinding_newArray");
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

    @SuppressWarnings("unchecked")
    public <T> T executeScript(Class<T> type, String script) {
        return (T) convertToJava(type, engine.executeScript(script));
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

    private Object convertFromJava(ParameterType type, Object value) {
        if (value == null) return null;

        if (value.getClass().getClassLoader() == loader) {
            JSInvocationHandler handler = (JSInvocationHandler) Proxy.getInvocationHandler(value);
            return handler.jsObject;
        } else if (value instanceof JSFunction) {
            JSFunction jsFunction = (JSFunction) value;

            boolean hasThisAnnotation = false;
            boolean rawArguments = false;
            for (Annotation annotation : type.annotations) {
                if (annotation instanceof This) {
                    hasThisAnnotation = true;
                } else if (annotation instanceof RawArguments) {
                    rawArguments = true;
                }
            }

            Object publisher = new FunctionPublisher(hasThisAnnotation, rawArguments, (ParameterizedType) type.type, jsFunction);

            return java2js.call("call", null, publisher);
        } else {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> JSArray<T> newArray(Class<T> type) {
        return (JSArray<T>) convertToJava(new JSArraySyntheticType(type), newArray.call("call"));
    }

    @SafeVarargs
    public final <T> JSArray<T> newArray(Class<T> type, T... elements) {
        JSArray<T> array = newArray(type);
        int i = 0;
        for (T e : elements) {
            array.set(i, e);
            i++;
        }
        return array;
    }

    private static class JSArraySyntheticType implements ParameterizedType {
        Type[] types;

        public JSArraySyntheticType(Class type) {
            types = new Type[]{type};
        }

        @Override
        public Type[] getActualTypeArguments() {
            return types;
        }

        @Override
        public Type getRawType() {
            return JSArray.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    public class FunctionPublisher {
        public FunctionPublisher(boolean hasThis, boolean rawArgument, ParameterizedType type, JSFunction function) {
            this.hasThis = hasThis;
            this.rawArgument = rawArgument;
            this.type = type;
            this.function = function;
        }

        final boolean hasThis;
        final boolean rawArgument;
        final ParameterizedType type;
        final JSFunction function;

        public Object invoke(Object self, JSObject args) {
            int length = (int) args.getMember("length");
            if (rawArgument) {
                Object[] argArray = new Object[length];
                for (int i = 0; i < length; i++) {
                    argArray[i] = args.getSlot(i);
                }

                if (hasThis) {
                    Type thisType = type.getActualTypeArguments()[0];
                    if (function instanceof JSRunnable2) {
                        ((JSRunnable2) function).call(convertToJava(thisType, self), argArray);
                        return null;
                    } else {
                        return convertFromJava(
                                new ParameterType(
                                        type.getActualTypeArguments()[length - 1]),
                                ((JSFunction2) function).call(convertToJava(thisType, self), argArray));
                    }
                } else {
                    if (function instanceof JSRunnable1) {
                        ((JSRunnable1) function).call(argArray);
                        return null;
                    } else {
                        return convertFromJava(
                                new ParameterType(
                                        type.getActualTypeArguments()[length - 1]),
                                ((JSFunction1) function).call(argArray));
                    }
                }
            } else {
                if (hasThis) length += 1;

                Object[] converted = new Object[length];
                if (hasThis) {
                    Type argType = type.getActualTypeArguments()[0];
                    converted[0] = convertToJava(argType, self);
                }
                for (int i = hasThis ? 1 : 0; i < length; i++) {
                    Type argType = type.getActualTypeArguments()[i];
                    converted[i] = convertToJava(argType, args.getSlot(i));
                }
                if (function instanceof JSRunnable) {
                    return function.invoke(converted); // always return null
                } else {
                    return convertFromJava(
                            new ParameterType(
                                    type.getActualTypeArguments()[length - 1]),
                            function.invoke(converted));
                }
            }
        }
    }

    private Object[] convertFromJava(ParameterType[] types, Object[] values) {
        Object[] convertedArgs = new Object[values.length];

        boolean valueChanged = false;
        for (int i = 0; i < values.length; i++) {
            convertedArgs[i] = convertFromJava(types[i], values[i]);
            if (convertedArgs[i] != values[i]) {
                valueChanged = true;
            }
        }

        if (valueChanged)
            return convertedArgs;
        else
            return values; // Don't let escape unnecessary new Object[]
    }

    public <T> T convertToJava(Class<T> returnType, Object value) {
        return (T) convertToJava((Type) returnType, value);
    }

    private Object convertToJava(Type returnType, Object value) {
        Class<?> returnClass = getClass(returnType);

        if (value == undefined // identity comparison
                && returnClass != JSObject.class
                && returnClass != void.class
                && returnClass != Void.class)
            throw new JSException("Undefined value returned by Javascript");

        if (returnClass.isAnnotationPresent(JSInterface.class)) {
            Class<?> clazz = getClass(returnType);
            return Proxy.newProxyInstance(loader, new Class[]{clazz}, new JSInvocationHandler((JSObject) value, returnType));
        } else if (returnClass.isAssignableFrom(Double.class) || returnClass.isAssignableFrom(double.class)) {
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

    static Annotation[] emptyAnnotation = new Annotation[0];

    static class ParameterType {
        final Annotation[] annotations;
        final Type type;

        ParameterType(Type type) {
            this.annotations = emptyAnnotation;
            this.type = type;
        }

        ParameterType(Annotation[] annotations, Type type) {
            this.annotations = annotations;
            this.type = type;
        }
    }

    static String jsName(Method method, String... javaPrefix) {
        if (method.isAnnotationPresent(JSName.class)) {
            return method.getAnnotation(JSName.class).value();
        }

        String methodName = method.getName();
        for (String p : javaPrefix) {
            if (methodName.length() > p.length() && methodName.startsWith(p)) {
                return Character.toLowerCase(methodName.charAt(p.length())) + methodName.substring(p.length() + 1);
            }
        }

        return methodName;
    }

    static ParameterType[] parameterTypes(Method method) {
        Type[] parameterTypes = method.getGenericParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        if (parameterTypes == null) return null;
        ParameterType[] types = new ParameterType[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            types[i] = new ParameterType(parameterAnnotations[i], parameterTypes[i]);
        }

        return types;
    }


    private class JSInvocationHandler implements InvocationHandler {
        private final JSObject jsObject;
        private final Type type;

        JSInvocationHandler(JSObject jsObject, Type type) {
            this.jsObject = jsObject;
            this.type = type;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.isDefault()) {
                return constructor.newInstance(method.getDeclaringClass(), MethodHandles.Lookup.PRIVATE)
                        .unreflectSpecial(method, method.getDeclaringClass())
                        .bindTo(proxy)
                        .invokeWithArguments(args);
            }

            ParameterType[] paramTypes = parameterTypes(method);
            if (args == null) {
                args = empty;
            }
            if (paramTypes == null) {
                paramTypes = emptyParamTypes;
            }

            Type returnType = resolveType(method.getGenericReturnType());

            if (method.isAnnotationPresent(Getter.class)) {
                checkArity(args, 0);

                String name = jsName(method, "is", "get");
                return convertToJava(returnType, jsObject.getMember(name));

            } else if (method.isAnnotationPresent(Setter.class)) {
                checkArity(args, 1);

                String name = jsName(method, "set");
                jsObject.setMember(name, convertFromJava(paramTypes[0], args[0]));
                return null;

            } else if (method.isAnnotationPresent(ArrayGetter.class)) {
                checkArity(args, 1);

                return convertToJava(returnType, jsObject.getSlot((int) args[0]));

            } else if (method.isAnnotationPresent(ArraySetter.class)) {
                checkArity(args, 2);

                jsObject.setSlot((int) args[0], convertFromJava(paramTypes[1], args[1]));

                return null;

            } else {
                // Method call
                String methodName = jsName(method);
                Object[] convertArguments = convertFromJava(paramTypes, args);
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
                return convertToJava(returnType, jsResult);
            }
        }

        private Type resolveType(Type returnType) {
            if (returnType instanceof TypeVariable) {
                // Resolve the type variable
                ParameterizedType pType;
                if (type instanceof ParameterizedType) {
                    pType = (ParameterizedType) type;
                } else {
                    // The user defined a non parametrized interface to encapsulate type parameters in a Class object?
                    pType = (ParameterizedType) ((Class) type).getGenericInterfaces()[0];
                }
                TypeVariable[] typeParameters = ((Class) pType.getRawType()).getTypeParameters();
                int i;
                for (i = 0; i < typeParameters.length; i++) {
                    if (typeParameters[i].equals(returnType)) break;
                }
                if (i == typeParameters.length) throw new AssertionError("Type parameter not found");

                returnType = pType.getActualTypeArguments()[i];
            }

            if (returnType instanceof Class && ((Class) returnType).getTypeParameters().length != 0) {
                throw new IllegalStateException("Parametrized class " + ((Class) returnType).getName() +
                        " used without its type parameters");
            }
            return returnType;
        }

    }


    private static final Constructor<MethodHandles.Lookup> constructor;

    static {
        try {
            constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


}
