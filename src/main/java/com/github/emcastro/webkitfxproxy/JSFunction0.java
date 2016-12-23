package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction0<R> extends JSFunction {

    R call();

    @Override
    default int arity() {
        return 0;
    }

    @Override
    default Object invoke(Object[] arguments) {
        return call();
    }
}
