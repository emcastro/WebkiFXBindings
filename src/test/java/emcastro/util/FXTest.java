package emcastro.util;

import com.mscharhag.oleaster.runner.Invokable;
import com.mscharhag.oleaster.runner.StaticRunnerSupport;
import com.sun.javafx.application.PlatformImpl;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Gives access to Oleaster {@code describe} and {@code it} methods with Java FX support.
 */
public class FXTest {
    public static void describe(String text, Invokable block) {

        StaticRunnerSupport.describe(text, () -> {
            PlatformImpl.startup(() -> {
            });
            invoke(block);
        });
    }

    public static void it(String text, Invokable block) {
        StaticRunnerSupport.it(text, () -> invoke(block));
    }

    private static void invoke(Invokable block) throws Exception {
        AtomicReference<Exception> ex = new AtomicReference<>();
        AtomicReference<Error> er = new AtomicReference<>();
        PlatformImpl.runAndWait(() -> {
            try {
                block.invoke();
            } catch (Exception e) {
                ex.set(e);
            } catch (Error e) {
                er.set(e);
            }
        });
        if (ex.get() != null) throw ex.get();
        if (er.get() != null) throw er.get();
    }
}
