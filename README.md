# WebkitFXProxy

# What is it?
WebkitFXProxy provides easy binding for Javascript objects residing in the Webkit bundled with JavaFX.

# Sample use

Consider the following Javascript class:
```javascript
"use strict";

function Rectangle(width, height) {
    this.width = width;
    this.height = height;
}
 
Rectangle.prototype.surface = function() {
    return this.width * this.height;
}

Rectangle.prototype.enlarge = function(factor) {
    this.width *= factor;
    this.height *= factor;
}

Rectangle.prototype.transform = function(transformer) {
    return new Rectangle(transformer(this.width), transformer(this.height));
}
```

We can define the following Java interface to operate onto JSObject of the Webkit (Webview) from Java, with type checking:

```java
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

    void enlarge(Double factor);

    Rectangle copy();

    // callback
    Rectangle transform(JSFunction1<Double, Double> transformer);
}
```

Creating a Java proxy from a JSObject is easy:
```Java
void foo(Webview webview) {
    WebEngine engine = webView.getEngine();

    // Proxy creation
    WebkitFXProxy proxy = new WebkitFXProxy(engine);

    // We build a new Rectangle() in Javascript
    String javascript="new Rectangle()";
    Rectangle rect = proxy.executeScript(Rectangle.class, javascript);

    System.out.println(rect.surface());
}    
```

## For more details
See `WebkitFXProxyTest.java` in the tests.

## Use WebkitFXProxy in your project

### Stable release

Current stable release is 1.0.0.

#### Maven coordinates

| Group ID            | Artifact ID   | Version |
| :-----------------: | :---------:   | :-----: |
| com.github.emcastro | webkitfxproxy | 1.0.0   |

#### Gradle example

```groovy
dependencies {
    compile 'com.github.emcastro:webkitfxproxy:1.0.0'
}
```

