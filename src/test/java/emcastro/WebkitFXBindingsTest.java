package emcastro;

import com.mscharhag.oleaster.runner.OleasterRunner;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static emcastro.util.FXTest.describe;
import static emcastro.util.FXTest.it;


/**
 * Created by ecastro on 04/12/16.
 */
@RunWith(OleasterRunner.class)
public class WebkitFXBindingsTest {
    {
        describe("WebkitFXBindings", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            WebkitFXBindings webkitFXBindings = new WebkitFXBindings(engine);

            Rectangle rect = webkitFXBindings.executeScript(Rectangle.class, WebkitFXBindings.class.getResource("Rectangle.js"));

            it("reads JS properties through getters", () -> {
                expect(rect.width()).toEqual(5.);
                expect(rect.getHeight()).toEqual(10.);
            });

            it("calls JS methods", () -> {
                expect(rect.surface()).toEqual(50.);

                rect.enlarge(2.);

                expect(rect.surface()).toEqual(200.);
            });

            it("writes JS properties through setters", () -> {
                rect.width(.5);
                rect.setHeight(2.);
                expect(rect.surface()).toEqual(1.);
            });

            it("stores Proxy classes into its internal classloader", () -> {
                expect(rect.getClass().getClassLoader()).toEqual(webkitFXBindings.loader);
            });

            it("encapsulates JSObject into @JSInterface enabled proxy objects", () -> {
                Rectangle copy = rect.copy();
                expect(copy.width()).toEqual(.5);
            });

            it("decapsulates @JSInterface enabled proxy objects to JSObject when used as argments", () -> {
                expect(rect.copy().equals(rect)).toBeTrue();
            });

            it("invokes function when retrieved from property", () -> {
                expect(rect.surface()).toEqual(1.);
                Rectangle rect2 = rect.copy();
                JSFunction2<Rectangle, Double, Void> enlarge = rect2.enlarge();
                enlarge.call(rect2, 10.);
                expect(rect2.surface()).toEqual(100.);
            });
        });
    }


    @JSInterface
    public interface Rectangle {

        // property in short syntax
        @Getter
        Double width();

        @Setter
        void width(double width);

        // property in Java syntax
        @Getter
        Double getHeight();

        @Setter
        void setHeight(double height);

        // method call
        Double surface();

        void enlarge(Double factor);

        Rectangle copy();

        // getting a function object
        @Getter
        JSFunction2<Rectangle, Double, Void> enlarge();

        // injecting JSInterface as argument
        boolean equals(Rectangle other);

        JSObject toArray();

    }

}
