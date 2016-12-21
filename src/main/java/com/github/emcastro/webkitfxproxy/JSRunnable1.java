package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable1<A> extends JSFunction {

    void call(A a);

    @Override
    default Object invoke(Object[] arguments) {
        checkArity(arguments, 1);
        call((A) arguments[0]);
        return 0;
    }
}
