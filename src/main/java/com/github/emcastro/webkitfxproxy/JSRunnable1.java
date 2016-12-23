package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable1<A> extends JSRunnable {

    void call(A a);

    @Override
    default int arity() {
        return 1;
    }

    @Override
    default void invoke(Object[] arguments) {
        call((A) arguments[0]);
    }
}
