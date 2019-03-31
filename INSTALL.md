Complete fresh manual install
=============================

First get you need an SD card, 8GB or larger. Use one of the following methods to prepare it, depending on your skills:

1) Buy a preloaded NOOBS SD card with your RPi
2) Download NOOBS and create a fresh install of Rasbian Lite on your SD card. See https://www.raspberrypi.org/documentation/installation/noobs.md
3) If you are more skilled you may prefer to download the Raspbian Lite image and flash it yourself, see https://www.raspberrypi.org/documentation/installation/installing-images/README.md

Get the SD card ready as above, insert into the RPi, connect HDMI screen or TV, USB keyboard, and a USB charger (minimum 2Amps, ideally 2.5+). 
The RPi will boot with a lot of messages and you will see the `login:` prompt on the screen 

You will need a connection to the internet to proceed. If you have ethernet, plug in a cable. If you have WiFi, then configure it in raspi-config below

Log in to console, as user `pi`, default password is `raspberry`

At the command prompt (looks like this `pi@raspberrypi:~ $`) type the following and hit [Enter] to execute each.


```
pi@raspberrypi:~ $ raspi-config
```
In the menu that appears use the arrow keys to select, and the [TAB] key to jump to the <ok> <cancel> options
Set the following:
```
	Localization>
	       Timezone: select your timezone
    Interfacing options>
	       Enable SSH: yes, you want ssh to start at boot time
    Advanced Options>	
	       Expand filesystem: yes
	       Memory split: 16
	Network>
		N2 WiFi>
			Country: Select your country (NZ for me, this must be set to something even if you dont have WiFi access)
			SSID: the name of your home wifi network, or blank
 	        Passphrase: the password for your wifi network, or blank
	Exit
```
Reboot the pi and login again when the prompt appears
``` 
pi@raspberrypi:~ $ sudo reboot

```
	
Update to latest
----------------
```
pi@raspberrypi:~ $ sudo apt-get update
pi@raspberrypi:~ $ sudo apt-get upgrade
```

Install helpful things
----------------------
```
pi@raspberrypi:~ $ sudo apt-get install -y curl wget git build-essential dialog
```

Install extra package sources
--------------------------
```
pi@raspberrypi:~ $ curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
pi@raspberrypi:~ $ curl -sL https://repos.influxdata.com/influxdb.key | sudo apt-key add -
pi@raspberrypi:~ $ echo "deb https://repos.influxdata.com/debian stretch stable" | sudo tee /etc/apt/sources.list.d/influxdb.list
pi@raspberrypi:~ $ sudo apt update
```

Install essential packages
--------------------------
```
pi@raspberrypi:~ $ sudo apt install nodejs
pi@raspberrypi:~ $ sudo apt-get install libnss-mdns avahi-utils libavahi-compat-libdnssd-dev
pi@raspberrypi:~ $ sudo apt-get install oracle-java8-jdk
pi@raspberrypi:~ $ sudo apt-get install influxdb
pi@raspberrypi:~ $ sudo apt-get install maven
pi@raspberrypi:~ $ sudo apt-get install dnsmasq hostapd
```
Configure services
------------------
```
pi@raspberrypi:~ $ sudo systemctl stop dnsmasq
pi@raspberrypi:~ $ sudo systemctl stop hostapd

pi@raspberrypi:~ $ sudo nano /etc/dhcpcd.conf
```
	Enter:
```
		interface wlan0
				static ip_address=192.168.0.1/24
			nohook wpa_supplicant
```
	Cnrtl-X to save
```
pi@raspberrypi:~ $ sudo mv /etc/dnsmasq.conf /etc/dnsmasq.conf.orig  
pi@raspberrypi:~ $ sudo nano /etc/dnsmasq.conf
```
	Enter:
```
		interface=wlan0      # Use the require wireless interface - usually wlan0
			`dhcp-range=192.168.0.2,192.168.0.20,255.255.255.0,24h
```
	Cnrtl-X to save
```
pi@raspberrypi:~ $ sudo nano /etc/hostapd/hostapd.confq
```
	Enter:
```
		interface=wlan0
		driver=nl80211
		ssid=freeboard
		hw_mode=g
		channel=10
		wmm_enabled=0
		macaddr_acl=0
		auth_algs=1
		ignore_broadcast_ssid=0
		wpa=2
		wpa_passphrase=freeboard
		wpa_key_mgmt=WPA-PSK
		wpa_pairwise=TKIP
		rsn_pairwise=CCMP
```
	Cnrtl-X to save
```
pi@raspberrypi:~ $ sudo nano /etc/default/hostapd
```
	Find the line with #DAEMON_CONF, and replace it with this:
		`DAEMON_CONF="/etc/hostapd/hostapd.conf"`
	Cnrtl-X to save
```
pi@raspberrypi:~ $ sudo nano /etc/sysctl.conf 
```
	uncomment (remove #) for this line:
		`#net.ipv4.ip_forward=1`
	Cnrtl-X to save

Add a masquerade for outbound traffic on eth0:
```
pi@raspberrypi:~ $ sudo iptables -t nat -A  POSTROUTING -o eth0 -j MASQUERADE
```
Save the iptables rule.
```
pi@raspberrypi:~ $ sudo sh -c "iptables-save > /etc/iptables.ipv4.nat"
```
Make it permanant at boot time
```
pi@raspberrypi:~ $ sudo nano /etc/rc.local
	Add this just above "exit 0" to install these rules on boot.
		iptables-restore < /etc/iptables.ipv4.nat
```
Install signalk-java
--------------------
```
pi@raspberrypi:~ $ git clone https://github.com/SignalK/signalk-java.git
pi@raspberrypi:~ $ cd signalk-java
pi@raspberrypi:~ $ git checkout jdk11
pi@raspberrypi:~ $ mvn exec:exec
```
	If it fails,
  `pi@raspberrypi:~ $ rm -rf ~/.m2/repository/com/github/SignalK/artemis-server/`
	and try 'mvn exec:exec' again

Adding apps can be done via the ui at https://[rpi_ip_address]:8443
