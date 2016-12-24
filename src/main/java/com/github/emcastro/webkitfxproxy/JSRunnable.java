package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 12/12/16.
 */
interface JSRunnable extends WithArity {

    void invoke(Object[] arguments);

}
