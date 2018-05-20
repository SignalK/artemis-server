require('core-js')
require('nashorn-polyfill/lib/timer-polyfill.js')
var Buffer = require('buffer/').Buffer
var global = this;

var console = {};
console.debug = print;
console.warn = print;
console.log = print;
console.error = print;
var process = {"argv":[]};
var mappings = {}

var getTagBlock = require('./lib-es5/getTagBlock');

var transformSource = require('./lib-es5/transformSource');


var loadHook = function(hook){
	//findHooks
	var subhook = require('./hooks-es5/'+hook);
	mappings[hook] = subhook
	print('JS:Loading NMEA sentence:'+hook)
}
print("JS:Parser instantiated!")

var parse = function(sentence){
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
	  //print(JSON.stringify(tags))
	 // print (sentence)
	 if (sentence.charCodeAt(sentence.length - 1) == 10) {
    //in case there's a newline
    sentence = sentence.substr(0, sentence.length - 1);
  }
  
  var data = sentence.split('*')[0];
  var dataParts = data.split(',');
  var id = dataParts[0].substr(3, 3).toUpperCase();
  var talker = dataParts[0].substr(1, 2);
  var split = dataParts.slice(1, dataParts.length);
 
  if (typeof tags.source === 'undefined') {
    tags.source = ':';
  } else {
    tags.source = tags.source + ':' + id;
  }
  this.session = 0;
  if(mappings[id]==undefined){
	  print("JS:NMEA: unsupported sentence "+sentence);
	  return null;
  }
  var parser = mappings[id](this, {
    id: id,
    sentence: sentence,
    parts: split,
    tags: tags
  });
  var result;
  parser.then(function (data) {
	  //print(JSON.stringify(transformSource(data, id, talker)))
	  	result= JSON.stringify(transformSource(data, id, talker))
	    
	  })
	  .catch(function (error) {
		  print('JS:Error:')
	  	result= JSON.stringify(error)
	    
	  })

  global.nashornEventLoop.process();

  return result
}
