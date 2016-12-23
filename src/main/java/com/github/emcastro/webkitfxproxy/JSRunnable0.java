package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable0 extends JSRunnable {

    void call();

    @Override
    default int arity() {
        return 0;
    }

    @Override
    default void invoke(Object[] arguments) {
        call();
    }
}
