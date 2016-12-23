package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 12/12/16.
 */
interface JSFunction {

    Object invoke(Object[] arguments);

    int arity();

}
