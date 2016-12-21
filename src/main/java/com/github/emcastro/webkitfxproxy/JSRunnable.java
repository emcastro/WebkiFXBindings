package com.github.emcastro.webkitfxproxy;

/**
 * Created by ecastro on 12/12/16.
 */
public interface JSRunnable extends JSFunction {

    Void invoke(Object[] arguments);

}
