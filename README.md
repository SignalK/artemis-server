Artemis Server
==============

This is the replacement for the signalk-java-server which began long before signalk, and has become too difficult to support.

Its a drop in replacement for the signalk-java-server, missing functionality is a bug.

The primary reason for at least two implementations of a signalk server is to ensure we dont  create a node application instead of a generic communication standard in signalk.  This has already been avoided several times, as the node-server is quite RPi/web-browser oriented, resulting in node/npm/http dependencies creeping in to webapp deployments, IoT device requirements,  and node specialised server-side requirements. 

In a signalk world there should be no dependency except on signalk message and API proocols, and all devices should be equal participants. If we dont achieve this we will just create another specialised application framework.

The Artemis server has several architectural features of note:

1) Java is natively multi-threaded, Artemis uses Java 11+ non-blocking aysnc IO, lamdbas and streams processing to transprently spread workloads across available CPU's. Hence a slow, blocking or failed operation will only stall one CPU, with others able to repair things.  Node (javascript) is single threaded, so only one CPU does the work, and if one process blocks, the server can stall.

2) The underlying transport is Activemq Artemis (hence the name!). https://activemq.apache.org/artemis/  Its a state-of-the-art async messaging server, with full support for most common protocols (AMQP,JMS,MQTT.COAP,STOMP, etc) and provides a highly redundant message layer for processing and routing imessages. This can scale horizontally to massive workloads, aka marinetraffic.com. The difference between a messaging layer and a pipeline (node-server) is that the messaging layer buffers messages in a queue between the processing steps. So the steps can execute at their own speed, and slow steps can be parallelised. Intermittent connections or periodic processes are naturally handled by the queue. If you optionally make a queue persistent, then no message will be lost even on reboot.

3) Artemis uses the Java 11 Graal Javascript compiler to provide a Javascript compatibility layer. It already uses the signalk-parser-nmea0183 and n2k-signalk projects for NMEA conversion, and could use others if required. Where it differs from node is it runs javascript multi-threaded! So the NMEA processing is spread across CPU's, and does not block other tasks.

4) Artemis uses a Time Series Database (TSB) as native storage. Hence all data is persistent, and full data history is maintained. Exploring the use of history is one area of special interest for me, as its virtually unknown in the recreational marine world. See item2), storing history and running sophisticated diagnostics and explorations will create workloads and data transfers way beyond the little RPi. The underlying Artemis message server can be used to spread this workload across many servers, even when they are only intermittently available.

5) Artemis exposes very sophisticated remote diagnostics via JMX/jolokia. You can capture, watch and trace messages as they pass through the server, and get detailed jvm and application performance data. The data can also be stored in the TSB, so you can look back into it later.

Apart from keeping signalk on track :-) the Artemis server has some really interesting aspects and  I expect it will gain more interest over time. If you think it has merit you are very welcome to contribute.

Design
------
See https://github.com/SignalK/artemis-server/blob/master/design/design.md

Security
--------
The default install has user `admin`, password `admin` _This is obviously very insecure_, the first thing you should do is change passwords.

See https://github.com/SignalK/artemis-server/blob/master/SECURITY.md

Functionality
-------------

	Inputs:
		Formats:
			Signalk delta
			Signalk full
			NMEA 0183 (STALK not enabled)
			Canboat format N2K
		Transports:	
			HTTP
			Websockets
			TCP
			UDP
			Serial connections
	Supports:
		Zeroconf/mDNS/BonJour
		Calculates declination from location
		Meta data on values
		Request/response semantics
		Events
			True wind calcs on apparent wind
			Anchor distance calcs on anchor watch
			Alarms
		Message types (on all transports):
			UPDATES
			GET
			PUT
			POST
			LIST
			SUBSCRIBE
			UNSUBSCRIBE
		Resources:
			tracks
			routes
			waypoints
			charts
	Persistence:
		Uses Time-series database to store all data and history
	Security:
		SSL/TLS (https, wss)
		Supports Users and Roles. Token based.
		Fine grain access control to the signalk key level
	REST APIs:
		/signalk/authenticate
		/signalk/v1/api
		/signalk/v1/stream
		/signalk/v1/snapshot
		/signalk/v1/playback
		/signalk/v1/history(prototye)
		/signalk/v1/webapps(prototype)
	REST API docs:
		/docs/ (Swagger/OpenAPI3)
	Charts:
		Openstreetmap (online) 
		OSM (Open Sea Map) (online)
		World base map. (offline)
		Upload processed BSB/KAP API (UI via signalk-java page) (offline)
		Maptiles?
	Management:
		Install apps (UI via signalk-java page)
		Server config API (UI via signalk-java page)
		Users/Groups API (UI via signalk-java page)
		Logging management API (UI via signalk-java page)
		View Logs API (UI via signalk-java page)
		Shutdown/Restart API (UI via signalk-java page)
		Runtime inspection: JMX, jolokia, hawtio (also supports remote access)
	TODO:
		Test, test, more tests...
		Events
			route following calcs XTE etc
		apis:
			/signalk/v1/access/requests
		Security:
			Add full RBAC rules based filtering
		NMEA output
		Enable MQTT/STOMP/COAP
		Better server UI

It assumes influx db is running on localhost:8086

REST API docs
-------------

Swagger REST API docs and live testing is available at https://localhost:8443/docs/


Installation
============

The artemis server is normally installed as the server part of signalk-java (https://github.com/SignalK/signalk-java) project, which includes the supporting web interface and misc config UI etc.

The default signalk-java installs the old java server, to get this new version see signalk-java README

Using JMX/Jolokia Diagnostics (via Hawtio)
==========================================

Download the stand-alone hawtio.jar file from https://hawt.io and save to a local dir ([HAWTIO_DIR]).
Start the hawtio-app with port 8888. eg `java -jar [HAWTIO_DIR]/hawtio-app-1.4.65.jar -p 8888`
Open web browser to `http://localhost:8888/hawtio/`

Look in the aretmis server start.log for the line `Jolokia: Agent started with URL http://192.168.43.246:8780/jolokia/`
Enter connection data to the artemis server: `http://192.168.43.246:8780/jolokia/` and connect.


Development
===========

Clone this project and signalk-java from github in the normal way. The artemis project uses maven to build, if you use an IDE like eclipse, netbeans, 
or intelliJ it should build automatically.

If you build from cli, then set JAVA_HOME to your chosen jdk, either jdk8 or jdk11. 

Setup
-----
Running under JDK11 with the Graal compiler requires java options set in eclipse:
```
-Xmx256M -XX:+HeapDumpOnOutOfMemoryError -Dio.netty.leakDetection.level=ADVANCED -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --module-path=./target/compiler/graal-sdk.jar:./target/compiler/truffle-api.jar --upgrade-module-path=./target/compiler/compiler.jar
```
There are similar params commented out in the pom, if you start from the cli, uncomment for JDK11


NMEA
====

Artemis server uses the signalk-parser-nmea0183 project modified to run under java11 graal, and to be useable directly from the java jar file without the full npm install process. This means the src/main/resources/dist/bundle.js file is commited to git, but as its not expected to change often, and greatly simplifies deployment that disadvantage is accepted for now.

To merge future changes from the signalk-parser-nmea0183 project, clone the signalk-parser-nmea0183 project separately, and run the following to create an es5 transpiled version:

```
  rm -rf node_modules/
  npm install
  npm install -D babel-cli
  npm install -D babel-preset-es2015
  nano ./package.json 
  	add tasks:
  		"build-es5-hooks": "babel hooks --out-dir hooks-es5",
    	"build-es5-lib": "babel lib --out-dir lib-es5",
  npm run build-es5-hooks
  npm run build-es5-lib
  
```
  The hooks-es5 dir and the lib-es5 dir were then copied to artemis-server/src/main/resources/signalk-parser-nmea0183/ and modified to suit. future changes to signalk-parser-nmea0183 will have to be copied over as required, and the webpack re-run to create the dist/bundle.js file.
  
  
