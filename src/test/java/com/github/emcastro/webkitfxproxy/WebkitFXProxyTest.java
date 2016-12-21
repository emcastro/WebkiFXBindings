package com.github.emcastro.webkitfxproxy;

import com.mscharhag.oleaster.runner.OleasterRunner;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.github.emcastro.webkitfxproxy.FXTest.describe;
import static com.github.emcastro.webkitfxproxy.FXTest.it;


/**
 * Created by ecastro on 04/12/16.
 */
@RunWith(OleasterRunner.class)
public class WebkitFXProxyTest {
    {
        describe("WebkitFXProxy", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            WebkitFXProxy proxy = new WebkitFXProxy(engine);

            Rectangle rect = proxy.executeScript(Rectangle.class, WebkitFXProxy.class.getResource("Rectangle.js"));

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
                expect(rect.getClass().getClassLoader()).toEqual(proxy.loader);
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

            it("handles JS arrays", () -> {
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
                Rectangle copy = rect.copy();
                Rectangle transform = copy.transform(value -> copy.surface() * value);
                expect(transform.width()).toEqual(250.);
                expect(transform.getHeight()).toEqual(500.);

                Rectangle rectangle = rect.arrayTransform(value -> proxy.newArray(Double.class, value.get(1), value.get(0)));
                expect(rectangle.width()).toEqual(10.);
                expect(rectangle.getHeight()).toEqual(5.);

                StringBuilder b = new StringBuilder();
                rect.forEach((s, d) -> b.append(s + ": " + d + "; "));
                expect(b.toString()).toEqual("width: 5.0; height: 10.0; ");
            });

            it("injects methods into objects", () -> {

                Rectangle copy = rect.copy();
                expect(copy.prettyPrint()).toEqual("<5,10>");
                copy.setFormatter(self -> "[" + self.width() + ":" + self.getHeight() + "]");
                expect(copy.prettyPrint()).toEqual("[5.0:10.0]");

                copy.setFormatter(Rectangle::simpleFormatter);
                expect(copy.prettyPrint()).toEqual("rect:5.0×10.0");
            });

            it("does not confuse undefined keyword and undefined String", () -> {
                expect(proxy.executeScript(String.class, "'undefined'")).toEqual("undefined");
                boolean crashed = false;
                try {
                    proxy.executeScript(String.class, "undefined");
                } catch (JSException e) {
                    expect(e.getMessage()).toEqual("Undefined value returned by Javascript");
                    crashed = true;
                }
                expect(crashed).toBeTrue();
            });

            it("has useful array support", () -> {
                List<Double> result = Arrays.asList(1.0, 2.0, 3.0, 4.0);
                JSArray<Rectangle> array = proxy.executeScript(JSArray_Rectangle.class, "[ new Rectangle(1,2), new Rectangle(3,4) ]");

                expect(array.stream().flatMap(r -> r.toArray().stream()).collect(Collectors.toList())).toEqual(result);

                ArrayList<Object> objects = new ArrayList<>();
                array.toList().forEach(r -> r.toArray().forEach(objects::add));
                expect(objects).toEqual(result);
            });
        });

        describe("Java to JS function transformation", () -> {
            it("behaves correctly", () -> {
                WebView webView = new WebView();
                WebEngine engine = webView.getEngine();

                WebkitFXProxy webkitFXProxy = new WebkitFXProxy(engine);

                JSObject obj = (JSObject) webkitFXProxy.engine.executeScript("r={value: 22}; r");

                JSObject js = (JSObject) webkitFXProxy.java2js.call("call", null, new FunctionPublisher());
                expect(js.toString()).toStartWith("function javaCall()");
                expect(js.call("call", obj, 42, 24)).toEqual(88); // 42+24+22
            });
        });
    }

    public static class FunctionPublisher {

        public Object invoke(JSObject self, JSObject args) {
            expect(args.getMember("length")).toEqual(2);
            return (int) args.getSlot(0) + (int) args.getSlot(1) + (int) self.getMember("value");
        }

    }

    @JSInterface
    public interface JSArray_Rectangle extends JSArray<Rectangle> {
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
        Rectangle transform(JSFunction1<Double, Double> transformer);

        Rectangle arrayTransform(JSFunction1<JSArray<Double>, JSArray<Double>> transformer);

        @Setter
        void setFormatter(@This JSFunction1<Rectangle, String> function);

        String prettyPrint();

        default String simpleFormatter() {
            return "rect:" + width() + "×" + getHeight();
        }

        void forEach(JSRunnable2<String, Double> action);
    }

}
