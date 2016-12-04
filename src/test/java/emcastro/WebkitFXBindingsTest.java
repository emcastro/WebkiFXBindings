package emcastro;

import com.mscharhag.oleaster.runner.OleasterRunner;
import emcastro.util.FXTest;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.junit.runner.RunWith;


/**
 * Created by ecastro on 04/12/16.
 */
@RunWith(OleasterRunner.class)
public class WebkitFXBindingsTest {
    {
        FXTest.describe("WebView", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            FXTest.it("runs", () -> {
                Object o = engine.executeScript("1+1");
                System.out.println(o);
            });
        });
    }

}
