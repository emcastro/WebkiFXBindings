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

            if (method.isAnnotationPresent(Getter.class)) {
                checkArity(args, 0);

                String name;
                if (method.getName().startsWith("get")) {
                    name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                } else if (method.getName().startsWith("is")) {
                    name = Character.toLowerCase(method.getName().charAt(2)) + method.getName().substring(3);
                } else {
                    name = method.getName();
                }

                Object o1 = convertResult(method.getReturnType(), o.getMember(name));
                return o1;
            } else if (method.isAnnotationPresent(Setter.class)) {
                checkArity(args, 1);

                String name;
                if (method.getName().startsWith("set")) {
                    name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                } else {
                    name = method.getName();
                }
                o.setMember(name, convertArguments(args)[0]);

                return null;
            } else {
                if (args == null) {
                    args = empty;
                }
                // Method call
                return convertResult(method.getReturnType(), o.call(method.getName(), convertArguments(args)));
            }
        });

    }

    private Object[] convertArguments(Object[] args) {
        return args; // décapsule le JSObject
    }

    private Object convertResult(Class<?> returnType, Object value) {
        if (returnType.isAnnotationPresent(JSInterface.class)) {
            return null;
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

}
