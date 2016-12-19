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

Rectangle.prototype.copy = function() {
    return new Rectangle(this.width, this.height);
}

Rectangle.prototype.toArray = function() {
    return [this.width, this.height];
}

Rectangle.prototype.equals = function(other) {
    if (other instanceof Rectangle) {
        return other.width === this.width
            && other.height === this.height;
    }
    return false;
}

Rectangle.prototype.transform = function(transformer) {
    return new Rectangle(transformer(this.width), transformer(this.height));
}

Rectangle.prototype.arrayTransform = function(arrayTransformer) {
    var transformedArray = arrayTransformer(this.toArray())
    return new Rectangle(transformedArray[0], transformedArray[1]);
}

Rectangle.prototype.prettyPrint = function() {
    if (this.formatter === undefined) {
        return "<"+this.width+","+this.height+">";
    } else {
        return this.formatter()
    }
}


new Rectangle(5,10)