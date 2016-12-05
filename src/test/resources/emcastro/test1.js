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
    return new Rectangle(width, height);
}

new Rectangle(5,10)