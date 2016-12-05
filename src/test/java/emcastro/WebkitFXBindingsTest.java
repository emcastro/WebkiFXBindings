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
                Object rect = engine.executeScript(script);
                System.out.println(rect);
                JSObject o2 = (JSObject) rect;
                JSObject surface = (JSObject) o2.getMember("surface");
                System.out.println(surface);
                System.out.println(((JSObject) rect).call("surface"));
                System.out.println(surface.call("call", o2));

                System.out.println("=====================");

                WebkitFXBindings webkitFXBindings = new WebkitFXBindings(engine);

                Rectangle javaRect = webkitFXBindings.proxy(Rectangle.class, rect);

                System.out.println(javaRect.getHeight());
                System.out.println(javaRect.width());
                System.out.println(javaRect.surface());
                javaRect.enlarge(2.);
                System.out.println(javaRect.surface());
                System.out.println(javaRect.enlarge());
                javaRect.setHeight(0.);
                System.out.println(javaRect.surface());
                System.out.println(javaRect.getClass().getClassLoader());

                Rectangle copy = javaRect.copy();
                System.out.println(copy.width());

                System.out.println(javaRect.compare(copy));
            });
        });

    }

    @JSInterface
    public interface Rectangle {
        @Getter
        Double width();

        @Setter
        void width(double width);

        @Getter
        Double getHeight();

        @Setter
        void setHeight(double height);

        Double surface();

        void enlarge(Double factor);

        @Getter
        JSObject enlarge();

        Rectangle copy();

        boolean compare(Rectangle other);
    }

}
