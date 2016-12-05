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

Rectangle.prototype.equals = function(other) {
    if (other instanceof Rectangle) {
        return other.width === this.width
            && other.height === this.height;
    }
    return false;
}

new Rectangle(5,10)