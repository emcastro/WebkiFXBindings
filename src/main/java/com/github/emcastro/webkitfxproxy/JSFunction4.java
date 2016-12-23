package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction4<A, B, C, D, R> extends JSFunction {

    R call(A a, B b, C c, D d);

    @Override
    default int arity() {
        return 4;
    }

    @Override
    default Object invoke(Object[] arguments) {
        return call((A) arguments[0], (B) arguments[1], (C) arguments[2], (D) arguments[3]);
    }
}
