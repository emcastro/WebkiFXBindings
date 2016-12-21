package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable3<A, B, C> extends JSFunction {

    void call(A a, B b, C c);

    @Override
    default Object invoke(Object[] arguments) {
        checkArity(arguments, 1);
        call((A) arguments[0], (B) arguments[1], (C) arguments[2]);
        return null;
    }
}
