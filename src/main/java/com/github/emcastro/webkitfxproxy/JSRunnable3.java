package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable3<A, B, C> extends JSRunnable {

    void call(A a, B b, C c);

    @Override
    default int arity() {
        return 3;
    }

    @Override
    default void invoke(Object[] arguments) {
        call((A) arguments[0], (B) arguments[1], (C) arguments[2]);
    }
}
