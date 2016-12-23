package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction2<A, B, R> extends JSFunction {

    R call(A a, B b);

    @Override
    default int arity() {
        return 2;
    }

    @Override
    default Object invoke(Object[] arguments) {
        return call((A) arguments[0], (B) arguments[1]);
    }
}
