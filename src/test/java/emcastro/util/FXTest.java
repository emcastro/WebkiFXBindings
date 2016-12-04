package emcastro.util;

import com.mscharhag.oleaster.runner.Invokable;
import com.mscharhag.oleaster.runner.StaticRunnerSupport;
import com.sun.javafx.application.PlatformImpl;

/**
 * Gives access to Oleaster {@code describe} and {@code it} methods with Java FX support.
 */
public class FXTest {
    public static void describe(String text, Invokable block) {

        StaticRunnerSupport.describe(text, () -> {
            PlatformImpl.startup(() -> {
            });
            PlatformImpl.runAndWait(() -> {
                try {
                    block.invoke();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    public static void it(String text, Invokable block) {
        StaticRunnerSupport.it(text, () -> {
            PlatformImpl.runAndWait(() -> {
                try {
                    block.invoke();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }
}
