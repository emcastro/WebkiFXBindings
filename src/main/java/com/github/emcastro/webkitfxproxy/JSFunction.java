package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 12/12/16.
 */
public interface JSFunction {

    Object invoke(Object[] arguments);

    default void checkArity(Object[] arguments, int arity) {
        if (arguments.length != arity) {
            throw new IllegalArgumentException("Actual argument count (" + arguments.length + ") " +
                    "doesn't match function arity (" + arity + ")");
        }
    }

}
