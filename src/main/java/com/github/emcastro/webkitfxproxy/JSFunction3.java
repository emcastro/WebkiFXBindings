package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction3<A, B, C, R> extends JSFunction {

    R call(A a, B b, C c);

    @Override
    default int arity() {
        return 3;
    }

    @Override
    default Object invoke(Object[] arguments) {
        return call((A) arguments[0], (B) arguments[1], (C) arguments[2]);
    }
}
