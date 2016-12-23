"use strict";

function JSBidule(value) {
    this.value = value
}

JSBidule.prototype.check = function(value) {
    return this.value == value;
}

var RunnableTester = {}

RunnableTester.run = function() {
    return {
        r0: this.r0(),
        r1: this.r1(new JSBidule(1)),
        r2: this.r2(new JSBidule(2), new JSBidule(3)),
        r3: this.r3(new JSBidule(4), new JSBidule(5), new JSBidule(6)),
        f0: this.f0(),
        f1: this.f1(new JSBidule(1)),
        f2: this.f2(new JSBidule(2), new JSBidule(3)),
        f3: this.f3(new JSBidule(4), new JSBidule(5), new JSBidule(6))
    };
}

RunnableTester
