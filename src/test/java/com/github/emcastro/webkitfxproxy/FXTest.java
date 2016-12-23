package com.github.emcastro.webkitfxproxy;

import com.mscharhag.oleaster.runner.Invokable;
import com.mscharhag.oleaster.runner.StaticRunnerSupport;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gives access to Oleaster {@code describe} and {@code it} methods with Java FX support.
 */
public class FXTest {
    static {
        PlatformImpl.startup(() -> {});
    }

    public static void describe(String text, Invokable block) {

        StaticRunnerSupport.describe(text, () -> {
            try {
                invoke(block);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        });
    }

    public static void it(String text, Invokable block) {
        StaticRunnerSupport.it(text, () -> {
            invoke(block);
        });
    }

    private static void invoke(Invokable block) throws Exception {
        AtomicReference<Exception> ex = new AtomicReference<>();
        AtomicReference<Error> er = new AtomicReference<>();
        CompletableFuture<Void> f = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                block.invoke();
            } catch (Exception e) {
                ex.set(e);
            } catch (Error e) {
                er.set(e);
            }
            f.complete(null);
        });
        f.get();
        if (ex.get() != null) throw ex.get();
        if (er.get() != null) throw er.get();
    }
}
