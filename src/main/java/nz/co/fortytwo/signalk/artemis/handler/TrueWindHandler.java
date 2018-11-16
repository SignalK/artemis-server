package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_INFLUX_KEY;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.TWO_PI;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_angleApparent;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_angleTrueGround;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_angleTrueWater;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_directionMagnetic;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_directionTrue;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_speedApparent;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_wind_speedTrue;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_courseOverGroundMagnetic;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_courseOverGroundTrue;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_speedOverGround;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

/*
*
* Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
* Web: www.42.co.nz
* Email: robert@42.co.nz
* Author: R T Huitema
*
* This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
* WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
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
*
*/

/**
 * Read filtered key value pairs from Config.INTERNAL_KV and calculate true wind
 * 
 * @author robert
 * 
 */

public class TrueWindHandler extends BaseHandler{
	
	private static Logger logger = LogManager.getLogger(TrueWindHandler.class);
	
	private Double cog;
	private Double cogm;
	private Double vesselSpeed;
	private Double apparentDirection;
	private Double apparentWindSpeed;
	

	public TrueWindHandler() {
		super();
		try {
		
			if (logger.isDebugEnabled())
				logger.debug("Initialising for : {} ",uuid );
			//environment.wind.angleApparent
			
			initSession(AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+env_wind_speedApparent+"%' OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+env_wind_angleApparent+"%'OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_speedOverGround+"%'OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_courseOverGroundMagnetic+"%'OR "
						+AMQ_INFLUX_KEY+" LIKE '"+vessels+dot+uuid+dot+nav_courseOverGroundTrue+"%'");
		} catch (Exception e) {
			logger.error(e,e);
		}
	}

	
	public void consume(Message message) {
		
			
			String key = message.getStringProperty(AMQ_INFLUX_KEY);
			if(key.contains(dot+meta+dot))return;
			Json node = Util.readBodyBuffer(message.toCore());
			
			if (logger.isDebugEnabled())
				logger.debug("Found key: {} : {}", key, node);
			
			if(key.contains(env_wind_angleApparent)) {
				apparentDirection=node.at(value).asDouble();
				if(apparentDirection<0)apparentDirection=apparentDirection+TWO_PI;
			}
			if(key.contains(env_wind_speedApparent))apparentWindSpeed=node.at(value).asDouble();
			if(key.contains(nav_speedOverGround))vesselSpeed=node.at(value).asDouble();
			if(key.contains(nav_courseOverGroundTrue))cog=node.at(value).asDouble();
			if(key.contains(nav_courseOverGroundMagnetic))cogm=node.at(value).asDouble();
			
			if (logger.isDebugEnabled())
				logger.debug("Calculating on apparent angle: {}, apparentSpeed : {}, vesselSpeed: {}", apparentDirection, apparentWindSpeed, vesselSpeed);
            
			if (apparentWindSpeed != null && apparentDirection != null && vesselSpeed != null) {
            	execute(message);
            }
	}

    /**
     * Updates the true wind direction from the apparent wind direction in the
     * provided signalKModel.
     *
     * @param signalkModel
     */
    public void execute(Message message) {
        try {
        	
                // now calc and add to body
                // 0-360 from bow clockwise

                double[] windCalc = calcTrueWindDirection(apparentWindSpeed, apparentDirection, vesselSpeed);
                if(logger.isInfoEnabled()){
                	logger.info(String.format("App speed, dir, vesselSpeed: %3.1f %4f %2.1f",Util.msToKnts(apparentWindSpeed), apparentDirection*360./TWO_PI, Util.msToKnts(vesselSpeed)));
                	logger.info(String.format("                   windCalc: %f %f",windCalc[0], windCalc[1]));
                }
                if (windCalc != null) {
                	logger.debug("Sending: {}", windCalc);
                	
                    if (!Double.isNaN(windCalc[1])) {
                    	logger.debug("Sending TWD: {}", windCalc);
                    	double bowWind = windCalc[1]<Math.PI?windCalc[1]: -(TWO_PI - windCalc[1]) ;
                    	
                        //map.put(Constants.WIND_DIR_TRUE, round(trueDirection, 2));
                        send(message, vessels+dot+uuid+dot+ env_wind_angleTrueGround+".values.internal", bowWind);
                        //TODO: adjust for waterOverGround
                        send(message, vessels+dot+uuid+dot+ env_wind_angleTrueWater+".values.internal", bowWind);
                        if(cog!=null) {
                        	//adjust for vessel heading
                            send(message, vessels+dot+uuid+dot+ env_wind_directionTrue+".values.internal", (cog+windCalc[1]) % TWO_PI);
                        }
                        if(cogm!=null) {
                        	//adjust for vessel heading
                            send(message, vessels+dot+uuid+dot+ env_wind_directionMagnetic+".values.internal", (cogm+windCalc[1]) % TWO_PI);
                        }
                    }
                    if (!Double.isNaN(windCalc[0])) {
                        //map.put(Constants.WIND_SPEED_TRUE, round(trueWindSpeed, 2));
                    	send(message, vessels+dot+uuid+dot + env_wind_speedTrue+".values.internal", windCalc[0]);
                    }
                    
                }

            

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    


	/**
     * Calculates the true wind direction from apparent wind on vessel Result is
     * relative to bow
     *
     * @param apparentWnd in m/s
     * @param apparentDir 0 to 360 deg to the bow in radians
     * @param vesselSpd in m/s
     *
     * @return array of [true speed in m/s, trueDirection 0 to 360 deg to the
     * bow, in radians ]
     */
    public double[] calcTrueWindDirection(double apparentWnd, double apparentDir, double vesselSpd) {
        double trueDirection = 0.0;
        double trueWindSpeed = 0.0;
        double windCalc[] = {trueWindSpeed, trueDirection};
        /*
		 * Y = 90 - D
		 * a = AW * ( cos Y )
		 * bb = AW * ( sin Y )
		 * b = bb - BS
		 * True-Wind Speed = (( a * a ) + ( b * b )) 1/2
		 * True-Wind Angle = 90-arctangent ( b / a )
         */

        apparentDir = apparentDir % TWO_PI;
        boolean port = apparentDir > Math.PI;
        if (port) {
            apparentDir = TWO_PI - apparentDir;
        }

        /*
		 * // Calculate true heading diff and true wind speed - JAVASCRIPT
		 * tan_alpha = (Math.sin(angle) / (aspeed - Math.cos(angle)));
		 * alpha = Math.atan(tan_alpha);
		 * 
		 * tdiff = rad2deg(angle + alpha);
		 * tspeed = Math.sin(angle)/Math.sin(alpha);
         */
        double aspeed = Math.max(apparentWnd, vesselSpd);
        if (apparentWnd > 0 && vesselSpd > 0.0) {
            aspeed = apparentWnd / vesselSpd;
        }
        double angle = apparentDir;
        double tan_alpha = (Math.sin(angle) / (aspeed - Math.cos(angle)));
        double alpha = Math.atan(tan_alpha);
        double tAngle = alpha + angle;
        if (Double.valueOf(tAngle).isNaN() || Double.isInfinite(tAngle)) {
            return windCalc;
        }
        if (port) {
            trueDirection = (TWO_PI - tAngle);
        } else {
            trueDirection = tAngle;
        }
        windCalc[1] = trueDirection % TWO_PI;
//        windCalc[1] = tAngle % TWO_PI;

        if (apparentWnd < 0.1 || vesselSpd < 0.1) {
            trueWindSpeed = Math.max(apparentWnd, vesselSpd);
            windCalc[0] = trueWindSpeed;
            return windCalc;
        }
        double tspeed = Math.sin(angle) / Math.sin(alpha);
        if (Double.valueOf(tspeed).isNaN() || Double.isInfinite(tspeed)) {
            return windCalc;
        }
        trueWindSpeed = Math.abs(tspeed * vesselSpd);
        windCalc[0] = trueWindSpeed;
        return windCalc;
    }
    
    


}
