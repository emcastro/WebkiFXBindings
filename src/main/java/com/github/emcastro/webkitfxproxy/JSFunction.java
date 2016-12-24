package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 12/12/16.
 */
interface JSFunction extends WithArity {

    Object invoke(Object[] arguments);

}
