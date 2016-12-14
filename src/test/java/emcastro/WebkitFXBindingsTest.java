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

            it("encapsulates JSObject into @JSInterface enabled proxy objects", () -> {
                Rectangle copy = rect.copy();
                expect(copy.width()).toEqual(5.);
            });


            it("calls JS methods", () -> {
                Rectangle copy = rect.copy();
                expect(copy.surface()).toEqual(50.);

                copy.enlarge(2.);

                expect(copy.surface()).toEqual(200.);
                expect(copy.surfaceAsInt()).toEqual(200);
            });

            it("writes JS properties through setters", () -> {
                Rectangle copy = rect.copy();
                copy.width(.5);
                copy.setHeight(2.);
                expect(copy.surface()).toEqual(1.);
            });

            it("stores Proxy classes into its internal classloader", () -> {
                expect(rect.getClass().getClassLoader()).toEqual(webkitFXBindings.loader);
            });


            it("decapsulates @JSInterface enabled proxy objects to JSObject when used as argments", () -> {
                expect(rect.copy().equals(rect)).toBeTrue(); // Note: equals is a Javascript implementation
            });

            it("invokes function when retrieved from property", () -> {
                expect(rect.surface()).toEqual(50.);
                Rectangle rect2 = rect.copy();
                JSFunction2<Rectangle, Double, Void> enlarge = rect2.enlarge();
                enlarge.call(rect2, .1);
                expect(rect2.surface()).toEqual(.5);
            });

            it("handle JS arrays", () -> {
                JSArray<Double> array = rect.toArray();
                expect(array.get(0)).toEqual(5.);
                expect(array.get(1)).toEqual(10.);

                expect(array.length()).toEqual(2);

                array.set(1, 20.);
                expect(array.get(1)).toEqual(20.);
            });

            it("can return raw JSObject", () -> {
                JSObject rawCopy = rect.rawCopy();
                expect(rawCopy.getMember("width")).toEqual(5);
                expect(rawCopy.getMember("height")).toEqual(10);
            });

            it("encapsulates Java callback parameters", () -> {

                rect.transform((rect2, value) -> rect2.surface() * value);

            });
        });

        describe("Java to JS function transformation", () -> {
            it("behaves correctly", () -> {
                WebView webView = new WebView();
                WebEngine engine = webView.getEngine();

                WebkitFXBindings webkitFXBindings = new WebkitFXBindings(engine);

                JSObject js = (JSObject) webkitFXBindings.java2js.call("call", null, new FunctionPublisher());
                expect(js.toString()).toStartWith("function javaCall()");
                expect(js.call("call", js, 42, 24)).toEqual(66);
            });
        });
    }

    public static class FunctionPublisher {

        public Object invoke(Object self, JSObject args) {
            expect(self.toString()).toStartWith("function javaCall()");
            expect(args.getMember("length")).toEqual(2);
            return (int) args.getSlot(0) + (int) args.getSlot(1);
        }

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

        @JSName("surface")
        int surfaceAsInt();


        void enlarge(Double factor);

        Rectangle copy();

        // getting the raw JSObject
        @JSName("copy")
        JSObject rawCopy();

        // getting a function object
        @Getter
        JSFunction2<Rectangle, Double, Void> enlarge();

        // injecting JSInterface as argument
        boolean equals(Rectangle other);

        // array
        JSArray<Double> toArray();

        // callback
        Rectangle transform(@This JSFunction2<Rectangle, Double, Double> transformer);

        @JSName("arrayTransform")
        Rectangle transform(JSFunction1<JSArray<Double>, JSArray<Double>> transformer);
    }

}
