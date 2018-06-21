Artemis Server
==============

This is the replacement for the signalk-java-server which began before signalk and has been forced into too many compromising refactorings over the years.

The artemis server uses a time-series database as the core storage for all data. This means all data is persistent, and it can recover the vessels entire signalk data at any point backwards through time. 

The intention is to provide a platform to implement a signalk history api that allows analysis over time, and against historic data.

Its a drop in replacement for the signalk-java-server, missing functionality is a bug.

It assumes influx db is running on localhost:8086

Installation
============



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
