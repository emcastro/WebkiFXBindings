package emcastro;

import com.mscharhag.oleaster.runner.OleasterRunner;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.junit.runner.RunWith;

import java.nio.file.Files;
import java.nio.file.Paths;

import static emcastro.util.FXTest.describe;
import static emcastro.util.FXTest.it;


/**
 * Created by ecastro on 04/12/16.
 */
@RunWith(OleasterRunner.class)
public class WebkitFXBindingsTest {
    {
        describe("WebView", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            it("runs", () -> {
                String script = new String(Files.readAllBytes(Paths.get(WebkitFXBindings.class.getResource("test1.js").toURI())));
                Object o = engine.executeScript(script);
                System.out.println(o);
                JSObject o2 = (JSObject) o;
                JSObject surface = (JSObject) o2.getMember("surface");
                System.out.println(surface);
                System.out.println(((JSObject) o).call("surface"));
                System.out.println(surface.call("call", o2));

                System.out.println("=====================");

                WebkitFXBindings webkitFXBindings = new WebkitFXBindings(engine);

                Rectangle proxy = webkitFXBindings.proxy(Rectangle.class, o);

                System.out.println(proxy.getHeight());
                System.out.println(proxy.width());
                System.out.println(proxy.surface());
                proxy.enlarge(2.);
                System.out.println(proxy.surface());
                System.out.println(proxy.enlarge());
                proxy.setHeight(0.);
                System.out.println(proxy.surface());
            });
        });

    }

    @JSInterface
    public interface Rectangle {
        @Getter
        Double width();

        @Getter
        Double getHeight();

        @Setter
        void setHeight(double height);

        Double surface();

        void enlarge(Double factor);

        @Getter
        JSObject enlarge();

        Rectangle copy();
    }

}
