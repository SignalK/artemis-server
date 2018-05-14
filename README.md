Artemis Server
==============

Assumes influx db is running on localhost:8086


NMEA
====

Uses the signalk-parser-nmea0183 project modified to run under java8 nashorn

Mods as per code and the following commands in dir signalk-parser-nmea0183/:
  284  rm -rf node_modules/
  285  npm install
  287  npm install -D babel-cli
  288  npm install -D babel-preset-es2015
  290  nano ./package.json 
  	add tasks:
  		"build-es5-hooks": "babel hooks --out-dir hooks-es5",
    	"build-es5-lib": "babel lib --out-dir lib-es5",
  291  npm run build-es5-hooks
  293  npm run build-es5-lib
