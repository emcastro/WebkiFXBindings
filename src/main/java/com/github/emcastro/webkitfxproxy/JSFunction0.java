package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction0<R> extends JSFunction {

    R call();

    @Override
    default Object invoke(Object[] arguments) {
        checkArity(arguments, 0);
        return call();
    }
}
