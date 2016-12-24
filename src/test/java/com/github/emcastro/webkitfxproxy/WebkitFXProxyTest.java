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
import java.util.concurrent.atomic.AtomicInteger;
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
                    expect(e.getMessage()).toEqual("Unexpected undefined value returned by Javascript. Awaiting java.lang.String");
                    crashed = true;
                }
                expect(crashed).toBeTrue();
            });

            it("accepts undefined return value when told to", () -> {
                Rectangle r = proxy.executeScript(Rectangle.class, "new Rectangle(undefined, 10)");
                expect(r.widthOption()).toBeNull();
                expect(r.getHeight()).toEqual(10.);
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

        describe("Standard calls", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            WebkitFXProxy proxy = new WebkitFXProxy(engine);

            RunnableTester r = proxy.executeScript(RunnableTester.class, WebkitFXProxy.class.getResource("Runnable.js"));


            it("behaves correctly with JSRunnable", () -> {
                AtomicInteger i = new AtomicInteger(0);
                r.r0(() -> i.addAndGet(1));
                r.r1((b1) -> {
                    expect(b1.check(1)).toBeTrue();
                    i.addAndGet(2);
                });
                r.r2((b1, b2) -> {
                    expect(b1.check(2)).toBeTrue();
                    expect(b2.check(3)).toBeTrue();
                    i.addAndGet(4);
                });
                r.r3((b1, b2, b3) -> {
                    expect(b1.check(4)).toBeTrue();
                    expect(b2.check(5)).toBeTrue();
                    expect(b3.check(6)).toBeTrue();
                    i.addAndGet(8);
                });

                JSObject result = r.runR();
                expect(result.getMember("r0") == proxy.undefined).toBeTrue();
                expect(result.getMember("r1") == proxy.undefined).toBeTrue();
                expect(result.getMember("r2") == proxy.undefined).toBeTrue();
                expect(result.getMember("r3") == proxy.undefined).toBeTrue();

                expect(i.get()).toEqual(1 + 2 + 4 + 8);
            });


            it("bahaves correctly with JSFunction", () -> {
                AtomicInteger i = new AtomicInteger(0);

                r.f0(() -> {
                    i.addAndGet(16);
                    return null;
                });
                r.f1((b1) -> {
                    expect(b1.check(1)).toBeTrue();
                    i.addAndGet(32);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(11)");
                });
                r.f2((b1, b2) -> {
                    expect(b1.check(2)).toBeTrue();
                    expect(b2.check(3)).toBeTrue();
                    i.addAndGet(64);
                    return null;
                });
                r.f3((b1, b2, b3) -> {
                    expect(b1.check(4)).toBeTrue();
                    expect(b2.check(5)).toBeTrue();
                    expect(b3.check(6)).toBeTrue();
                    i.addAndGet(128);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(33)");
                });

                JSObject result = r.runF();
                expect(result.getMember("f0")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f1")).check(11)).toBeTrue();
                expect(result.getMember("f2")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f3")).check(33)).toBeTrue();

                expect(i.get()).toEqual(16 + 32 + 64 + 128);
            });
        });

        describe("Calls with @This", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            WebkitFXProxy proxy = new WebkitFXProxy(engine);

            RunnableTesterThis r = proxy.executeScript(RunnableTesterThis.class, WebkitFXProxy.class.getResource("Runnable.js"));


            it("behaves correctly with JSRunnable", () -> {
                AtomicInteger i = new AtomicInteger(0);
                r.r0((self) -> i.addAndGet(1));
                r.r1((self, b1) -> {
                    expect(b1.check(1)).toBeTrue();
                    i.addAndGet(2);
                });
                r.r2((self, b1, b2) -> {
                    expect(b1.check(2)).toBeTrue();
                    expect(b2.check(3)).toBeTrue();
                    i.addAndGet(4);
                });
                r.r3((self, b1, b2, b3) -> {
                    expect(b1.check(4)).toBeTrue();
                    expect(b2.check(5)).toBeTrue();
                    expect(b3.check(6)).toBeTrue();
                    i.addAndGet(8);
                });

                JSObject result = r.runR();
                expect(result.getMember("r0") == proxy.undefined).toBeTrue();
                expect(result.getMember("r1") == proxy.undefined).toBeTrue();
                expect(result.getMember("r2") == proxy.undefined).toBeTrue();
                expect(result.getMember("r3") == proxy.undefined).toBeTrue();

                expect(i.get()).toEqual(1 + 2 + 4 + 8);
            });


            it("bahaves correctly with JSFunction", () -> {
                AtomicInteger i = new AtomicInteger(0);

                r.f0((self) -> {
                    i.addAndGet(16);
                    return null;
                });
                r.f1((self, b1) -> {
                    expect(b1.check(1)).toBeTrue();
                    i.addAndGet(32);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(11)");
                });
                r.f2((self, b1, b2) -> {
                    expect(b1.check(2)).toBeTrue();
                    expect(b2.check(3)).toBeTrue();
                    i.addAndGet(64);
                    return null;
                });
                r.f3((self, b1, b2, b3) -> {
                    expect(b1.check(4)).toBeTrue();
                    expect(b2.check(5)).toBeTrue();
                    expect(b3.check(6)).toBeTrue();
                    i.addAndGet(128);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(33)");
                });

                JSObject result = r.runF();
                expect(result.getMember("f0")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f1")).check(11)).toBeTrue();
                expect(result.getMember("f2")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f3")).check(33)).toBeTrue();

                expect(i.get()).toEqual(16 + 32 + 64 + 128);
            });
        });


        describe("Calls with @RawArgument", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            WebkitFXProxy proxy = new WebkitFXProxy(engine);

            RunnableTesterRaw r = proxy.executeScript(RunnableTesterRaw.class, WebkitFXProxy.class.getResource("Runnable.js"));


            it("behaves correctly with JSRunnable", () -> {
                AtomicInteger i = new AtomicInteger(0);
                r.r0((args) -> {
                    i.addAndGet(1);
                    expect(args.length).toEqual(0);
                });
                r.r1((args) -> {
                    expect(((JSObject) args[0]).call("check", 1)).toEqual(true);
                    i.addAndGet(2);
                });
                r.r2((args) -> {
                    expect(((JSObject) args[0]).call("check", 2)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 3)).toEqual(true);
                    i.addAndGet(4);
                });
                r.r3((args) -> {
                    expect(((JSObject) args[0]).call("check", 4)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 5)).toEqual(true);
                    expect(((JSObject) args[2]).call("check", 6)).toEqual(true);

                    i.addAndGet(8);
                });

                JSObject result = r.runR();
                expect(result.getMember("r0") == proxy.undefined).toBeTrue();
                expect(result.getMember("r1") == proxy.undefined).toBeTrue();
                expect(result.getMember("r2") == proxy.undefined).toBeTrue();
                expect(result.getMember("r3") == proxy.undefined).toBeTrue();

                expect(i.get()).toEqual(1 + 2 + 4 + 8);
            });


            it("bahaves correctly with JSFunction", () -> {
                AtomicInteger i = new AtomicInteger(0);

                r.f0((args) -> {
                    i.addAndGet(16);
                    expect(args.length).toEqual(0);
                    return null;
                });
                r.f1((args) -> {
                    expect(((JSObject) args[0]).call("check", 1)).toEqual(true);
                    i.addAndGet(32);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(11)");
                });
                r.f2((args) -> {
                    expect(((JSObject) args[0]).call("check", 2)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 3)).toEqual(true);
                    i.addAndGet(64);
                    return null;
                });
                r.f3((args) -> {
                    expect(((JSObject) args[0]).call("check", 4)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 5)).toEqual(true);
                    expect(((JSObject) args[2]).call("check", 6)).toEqual(true);
                    i.addAndGet(128);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(33)");
                });

                JSObject result = r.runF();
                expect(result.getMember("f0")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f1")).check(11)).toBeTrue();
                expect(result.getMember("f2")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f3")).check(33)).toBeTrue();

                expect(i.get()).toEqual(16 + 32 + 64 + 128);
            });
        });


        describe("Calls with @RawArgument @This", () -> {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            WebkitFXProxy proxy = new WebkitFXProxy(engine);

            RunnableTesterRawThis r = proxy.executeScript(RunnableTesterRawThis.class, WebkitFXProxy.class.getResource("Runnable.js"));


            it("behaves correctly with JSRunnable", () -> {
                AtomicInteger i = new AtomicInteger(0);
                r.r0((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    i.addAndGet(1);
                    expect(args.length).toEqual(0);
                });
                r.r1((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    expect(((JSObject) args[0]).call("check", 1)).toEqual(true);
                    i.addAndGet(2);
                });
                r.r2((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    expect(((JSObject) args[0]).call("check", 2)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 3)).toEqual(true);
                    i.addAndGet(4);
                });
                r.r3((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    expect(((JSObject) args[0]).call("check", 4)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 5)).toEqual(true);
                    expect(((JSObject) args[2]).call("check", 6)).toEqual(true);

                    i.addAndGet(8);
                });

                JSObject result = r.runR();
                expect(result.getMember("r0") == proxy.undefined).toBeTrue();
                expect(result.getMember("r1") == proxy.undefined).toBeTrue();
                expect(result.getMember("r2") == proxy.undefined).toBeTrue();
                expect(result.getMember("r3") == proxy.undefined).toBeTrue();

                expect(i.get()).toEqual(1 + 2 + 4 + 8);
            });


            it("bahaves correctly with JSFunction", () -> {
                AtomicInteger i = new AtomicInteger(0);

                r.f0((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    i.addAndGet(16);
                    expect(args.length).toEqual(0);
                    return null;
                });
                r.f1((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    expect(((JSObject) args[0]).call("check", 1)).toEqual(true);
                    i.addAndGet(32);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(11)");
                });
                r.f2((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    expect(((JSObject) args[0]).call("check", 2)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 3)).toEqual(true);
                    i.addAndGet(64);
                    return null;
                });
                r.f3((self, args) -> {
                    expect(r.isSame(self)).toBeTrue();
                    expect(((JSObject) args[0]).call("check", 4)).toEqual(true);
                    expect(((JSObject) args[1]).call("check", 5)).toEqual(true);
                    expect(((JSObject) args[2]).call("check", 6)).toEqual(true);
                    i.addAndGet(128);
                    return proxy.executeScript(JSBidule.class, "new JSBidule(33)");
                });

                JSObject result = r.runF();
                expect(result.getMember("f0")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f1")).check(11)).toBeTrue();
                expect(result.getMember("f2")).toBeNull();
                expect(proxy.convertToJava(JSBidule.class, result.getMember("f3")).check(33)).toBeTrue();

                expect(i.get()).toEqual(16 + 32 + 64 + 128);
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
        double getHeight();

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

        // support for undefined return value
        @Getter
        @Undefinedable
        @JSName("width")
        Double widthOption();
    }

    @JSInterface
    public interface JSBidule {
        boolean check(double value);
    }

    @JSInterface
    public interface RunnableTester {
        @Setter
        void r0(JSRunnable0 r);

        @Setter
        void r1(JSRunnable1<JSBidule> r);

        @Setter
        void r2(JSRunnable2<JSBidule, JSBidule> r);

        @Setter
        void r3(JSRunnable3<JSBidule, JSBidule, JSBidule> r);

        @Setter
        void f0(JSFunction0<JSBidule> f);

        @Setter
        void f1(JSFunction1<JSBidule, JSBidule> f);

        @Setter
        void f2(JSFunction2<JSBidule, JSBidule, JSBidule> f);

        @Setter
        void f3(JSFunction3<JSBidule, JSBidule, JSBidule, JSBidule> f);

        JSObject runR();

        JSObject runF();

    }

    @JSInterface
    public interface RunnableTesterThis {
        @Setter
        void r0(@This JSRunnable1<RunnableTesterThis> r);

        @Setter
        void r1(@This JSRunnable2<RunnableTesterThis, JSBidule> r);

        @Setter
        void r2(@This JSRunnable3<RunnableTesterThis, JSBidule, JSBidule> r);

        @Setter
        void r3(@This JSRunnable4<RunnableTesterThis, JSBidule, JSBidule, JSBidule> r);

        @Setter
        void f0(@This JSFunction1<RunnableTesterThis, JSBidule> f);

        @Setter
        void f1(@This JSFunction2<RunnableTesterThis, JSBidule, JSBidule> f);

        @Setter
        void f2(@This JSFunction3<RunnableTesterThis, JSBidule, JSBidule, JSBidule> f);

        @Setter
        void f3(@This JSFunction4<RunnableTesterThis, JSBidule, JSBidule, JSBidule, JSBidule> f);

        boolean isSame(RunnableTesterThis other);

        JSObject runR();

        JSObject runF();

    }

    @JSInterface
    public interface RunnableTesterRawThis {
        @Setter
        void r0(@This @RawArguments JSRunnable2<RunnableTesterRawThis, Object[]> r);

        @Setter
        void r1(@This @RawArguments JSRunnable2<RunnableTesterRawThis, Object[]> r);

        @Setter
        void r2(@This @RawArguments JSRunnable2<RunnableTesterRawThis, Object[]> r);

        @Setter
        void r3(@This @RawArguments JSRunnable2<RunnableTesterRawThis, Object[]> r);

        @Setter
        void f0(@This @RawArguments JSFunction2<RunnableTesterRawThis, Object[], JSBidule> r);

        @Setter
        void f1(@This @RawArguments JSFunction2<RunnableTesterRawThis, Object[], JSBidule> r);

        @Setter
        void f2(@This @RawArguments JSFunction2<RunnableTesterRawThis, Object[], JSBidule> r);

        @Setter
        void f3(@This @RawArguments JSFunction2<RunnableTesterRawThis, Object[], JSBidule> r);

        boolean isSame(RunnableTesterRawThis other);

        JSObject runR();

        JSObject runF();

    }

    @JSInterface
    public interface RunnableTesterRaw {
        @Setter
        void r0(@RawArguments JSRunnable1<Object[]> r);

        @Setter
        void r1(@RawArguments JSRunnable1<Object[]> r);

        @Setter
        void r2(@RawArguments JSRunnable1<Object[]> r);

        @Setter
        void r3(@RawArguments JSRunnable1<Object[]> r);

        @Setter
        void f0(@RawArguments JSFunction1<Object[], JSBidule> r);

        @Setter
        void f1(@RawArguments JSFunction1<Object[], JSBidule> r);

        @Setter
        void f2(@RawArguments JSFunction1<Object[], JSBidule> r);

        @Setter
        void f3(@RawArguments JSFunction1<Object[], JSBidule> r);

        JSObject runR();

        JSObject runF();

    }


}
