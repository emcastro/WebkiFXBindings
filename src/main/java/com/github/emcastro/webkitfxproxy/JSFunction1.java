package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction1<A, R> extends JSFunction {

    R call(A a);

    @Override
    default Object invoke(Object[] arguments) {
        checkArity(arguments, 1);
        return call((A) arguments[0]);
    }
}
