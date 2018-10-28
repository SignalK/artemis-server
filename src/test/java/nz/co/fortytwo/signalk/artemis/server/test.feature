
Feature: Core Signalk API's
  Check the server is up

Background: 
* url 'http://localhost:8080'

		Scenario: Testing valid signalk endpoint
		* def endpointStructure =
		"""
		{
			  server: {
			    id: '#string',
			    version: '#string',
			  },
			  endpoints: {
			    v1: {
			      signalk-http: '#string',
			      signalk-ws: '#string',
			      version: '#string',
			      signalk-tcp: '##string',
			      signalk-udp: '##string',
			      nmea-tcp: '##string',
			      nmea-udp: '##string',
			      mqtt: '##string',
			      stomp: '##string'
			    }
			  }
			}
			"""
		
			Given path '/signalk'
			When method GET
			Then status 200
			And match response == endpointStructure
			And match header Content-Type contains 'application/json'
			And match header Content-Type contains 'charset=UTF-8'

		
	  
	    
 