'use strict';

/**
 * Copyright 2016 Signal K and Fabian Tollenaar <fabian@signalk.org>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var getTagBlock = require('./getTagBlock');
var transformSource = require('./transformSource');
var utils = require('@signalk/nmea0183-utilities');
var hooks = require('../hooks');
var pkg = require('../package.json');

var Parser = function () {
  function Parser(opts) {
    _classCallCheck(this, Parser);

    this.options = (typeof opts === 'undefined' ? 'undefined' : _typeof(opts)) === 'object' && opts !== null ? opts : {};
    if (!Object.keys(this.options).includes('validateChecksum')) {
      this.options.validateChecksum = true;
    }
    this.session = {};

    this.name = pkg.name;
    this.version = pkg.version;
    this.author = pkg.author;
    this.license = pkg.license;
  }

  _createClass(Parser, [{
    key: 'parse',
    value: function parse(sentence) {
      var tags = getTagBlock(sentence);
      if (tags !== false) {
        sentence = tags.sentence;
        tags = tags.tags;
      } else {
        tags = {};
      }

      if (typeof tags.timestamp === 'undefined') {
        tags.timestamp = new Date().toISOString();
      }

      var valid = utils.valid(sentence, this.options.validateChecksum);
      if (valid === false) {
        throw new Error('Sentence "' + sentence.trim() + '" is invalid');
      }

      if (sentence.charCodeAt(sentence.length - 1) == 10) {
        //in case there's a newline
        sentence = sentence.substr(0, sentence.length - 1);
      }

      var data = sentence.split('*')[0];
      var dataParts = data.split(',');
      var id = void 0,
          talker = void 0,
          internalId = '';
      if (dataParts[0].charAt(1).toUpperCase() === 'P') {
        // proprietary sentence
        id = dataParts[0].substr(-3, dataParts[0].length).toUpperCase();
        talker = dataParts[0].substr(1, 2).toUpperCase();
        internalId = dataParts[0].substr(1, dataParts[0].length);
      } else {
        id = dataParts[0].substr(3, 3).toUpperCase();
        talker = dataParts[0].substr(1, 2);
        internalId = id;
      }
      var split = dataParts.slice(1, dataParts.length);

      if (typeof tags.source === 'undefined') {
        tags.source = ':';
      } else {
        tags.source = tags.source + ':' + id;
      }

      if (typeof hooks[internalId] === 'function') {
        var result = hooks[internalId]({
          id: id,
          sentence: sentence,
          parts: split,
          tags: tags
        }, this.session);
        return transformSource(result, id, talker);
      } else {
        return null;
      }
    }
  }]);

  return Parser;
}();

module.exports = Parser;