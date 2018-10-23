'use strict';

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

var Transform = require('stream').Transform;
var ParentParser = require('../');

console.log('ParentParser', ParentParser);

var CompatParser = function CompatParser(opts) {
  var _this = this;

  if (!(this instanceof CompatParser)) {
    return new CompatParser(opts);
  }

  var options = Object.assign({}, opts);

  if (_typeof(options.stream) !== 'object' || options.stream === null) {
    options.stream = {};
  }

  options.stream.objectMode = true;
  Transform.call(this, options.stream);

  this.parser = new ParentParser(opts);
  this.stream = this.parser.stream();

  this.stream.on('data', function (delta) {
    _this.emit('delta', delta);
    _this.push(delta);
  });

  this.stream.on('nmea0183', function (sentence) {
    _this.emit('nmea0183', sentence);
  });
};

require('util').inherits(CompatParser, Transform);
module.exports = CompatParser;

CompatParser.prototype._transform = function (chunk, encoding, done) {
  this.stream.write(chunk);
  done();
};