package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 12/12/16.
 */
interface JSRunnable {

    void invoke(Object[] arguments);

    int arity();

}
