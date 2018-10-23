
require('core-js')

var Buffer = require('buffer/').Buffer
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
	//print("JS:Parser:"+sentence);
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
	 //print("JS:NMEA: unsupported sentence "+sentence);
	  return "Error:NMEA: unsupported sentence "+sentence;
  }
  //print('JS:Debug:'+JSON.stringify(mappings[id]));
  if (typeof mappings[id] === 'function') {
      var result = mappings[id]({
        id: id,
        sentence: sentence,
        parts: split,
        tags: tags
      });
      //print('JS:Debug:'+JSON.stringify(result));
      //print('JS:Debug:'+JSON.stringify(transformSource(data, id, talker)))
      return JSON.stringify(transformSource(result, id, talker));
    } else {
      return null;
    }
  
  
 // global.nashornEventLoop.process();

  return result
}

module.exports = {
		loadHook : loadHook,
		parse : parse
}
