package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable2<A, B> extends JSRunnable {

    void call(A a, B b);

    @Override
    default int arity() {
        return 2;
    }

    @Override
    default void invoke(Object[] arguments) {
        call((A) arguments[0], (B) arguments[1]);
    }
}
