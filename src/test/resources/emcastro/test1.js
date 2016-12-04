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

new Rectangle(5,10)