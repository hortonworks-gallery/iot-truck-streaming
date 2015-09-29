
function ApplicationModel(stompClient, L) {
  var self = this;

  self.username = ko.observable();
  self.driverMontior = ko.observable(new DriverMonitorModel());
  self.notifications = ko.observableArray();
  self.leaf = L;
  self.truckSymbolSize;
  

  self.connect = function() {
    stompClient.connect('', '', function(frame) {
      console.log('Connected XXXX' + frame);
      self.username(frame.headers['user-name']);

      // Loads all the dangerous events for all drivers on page load
      stompClient.subscribe("/app/driverEvents", function(message) {
	console.log('msg from /app/driverEvents: ' + message);
    	var jsonResponse = JSON.parse(message.body);
    	var lat = jsonResponse.startLat;
    	var long = jsonResponse.startLong;
    	var zoomLevel = jsonResponse.zoomLevel;

    	self.truckSymbolSize=jsonResponse.truckSymbolSize;
    	
        self.driverMontior().loadDangerousEvents(jsonResponse.violationEvents);
        self.driverMontior().initializeMap(lat, long, zoomLevel);
      });
      
      //Update page with any new dangerous event that came in
      //stompClient.subscribe("/topic/driver_infraction_events", function(message) {
      stompClient.subscribe("/topic/driver_infraction_events", function(message) {
	   console.log('msg from /topic/driver_infraction_events: ' + message);
           self.driverMontior().processDangerousEvent(JSON.parse(message.body));
       });
      
      stompClient.subscribe("/topic/driver_events", function(message) {
    	  //console.log(message);
	  console.log('msg from /topic/driver_events: ' + message);
    	  self.driverMontior().renderOnMap(JSON.parse(message.body), self.truckSymbolSize);
    	  //setTimeout(self.driverMontior().renderOnMap(JSON.parse(message.body), self.truckSymbolSize), 2000);
      });      
      
      //Update page with any new alerts
      stompClient.subscribe("/topic/driver_alert_notifications", function(message) {
	  console.log('msg from /topic/driver_alert_notifications: ' + message);
          self.pushNotification(JSON.parse(message.body).alertNotification);
        });
      
    }, function(error) {
      console.log("STOMP protocol error " + error);
    });
  }

  self.pushNotification = function(text) {
    self.notifications.push({notification: text});
    if (self.notifications().length > 5) {
      self.notifications.shift();
    }
  }

  self.logout = function() {
    stompClient.disconnect();
    window.location.href = "../logout.html";
  }
}

function DriverMonitorModel() {
  var self = this;

  
  self.rows = ko.observableArray();

  var rowLookup = {};
  
  var driverOnMapLookup = {};
  var stopIncreasingRadius = false;
  

  
  self.initializeMap = function(lat, long, zoomLevel) {
	  console.log("inside initialize map...");
	  //map = L.map('map').setView([lat, long], 4);
	  
	map = L.map('map', {
    		center: [lat, long],
    		zoom: 4
	});
	
 
	    // add an OpenStreetMap tile layer
	  console.log('adding tile: http://{s}.tile.osm.org/{z}/{x}/{y}.png');
	  L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
	        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
	  }).addTo(map);	  
	  
	  
	  latlng = L.latLng(39.82,-98.57);
          map.panTo(latlng);
	  map.zoomIn(1);	

	console.log('added tile: http://{s}.tile.osm.org/{z}/{x}/{y}.png');
  }
  
  
  self.loadDangerousEvents = function(positions) {
	  for ( var i = 0; i < positions.length; i++) {
    	
    	self.loadDangerousEvent(positions[i]);
    }
  };
  
  self.loadDangerousEvent = function (position) {
  	var row = new DriverRow(position);
	self.rows.push(row);
	rowLookup[row.driverId] = row;	  
  }
  
  self.processDangerousEvent = function(driverEvent) {
	 	if (rowLookup.hasOwnProperty(driverEvent.driverId)) {
	 		rowLookup[driverEvent.driverId].highlight();
	 		rowLookup[driverEvent.driverId].updateEvent(driverEvent);
	 		setTimeout(function() {
	 			
	 			rowLookup[driverEvent.driverId].unHighlight();
	 			
	 		}, 2000);
	 		
	 		
	    } else {
	    	self.loadDangerousEvent(driverEvent);
	    }
	  }; 
	  
	  

  self.renderOnMap = function(driverEvent, truckSymbolSize) {
	  if (driverOnMapLookup.hasOwnProperty(driverEvent.driverId)) {
		  var driverOnMap = driverOnMapLookup[driverEvent.driverId].driverOnMap;
		  var previousDriverEvent = driverOnMapLookup[driverEvent.driverId].driverEvent;
		  
		  driverOnMap.setLatLng([driverEvent.latitude, driverEvent.longitude]);
		  
		  var driverMsg;
		  var alert = driverOnMap.getRadius() > 40000;
		  if(driverEvent.numberOfInfractions == previousDriverEvent.numberOfInfractions) {
			  driverMsg = self.constructMessage(driverEvent.driverId, driverEvent.numberOfInfractions, previousDriverEvent.infractionEvent, driverEvent.driverName, driverEvent.routeId, driverEvent.routeName, alert);
		  
		  } else {
			  driverMsg = self.constructMessage(driverEvent.driverId, driverEvent.numberOfInfractions, driverEvent.infractionEvent, driverEvent.driverName, driverEvent.routeId, driverEvent.routeName, alert);
		  }
		  
		  driverOnMapLookup[driverEvent.driverId].driverEvent = driverEvent;
		  
		  //driverOnMap.bindPopup(driverMsg);
		  if(driverEvent.infractionEvent != 'Normal') {
			  driverOnMap.closePopup();
			  
			  
			  if(driverOnMap.getRadius() > 40000) {	
				  //console.log("Raidus is either greater than 40000 or all radisus increases have stopped " + newRadius);
				  stopIncreasingRadius = true; 				    			  
			  }
			  if(!stopIncreasingRadius) {
				  
				  var newRadius = driverOnMap.getRadius() * 1.1;
				  driverOnMap.setRadius(newRadius);
			  }
			  
			  //console.log("New Radius is: " + newRadius);
			  
			  driverOnMap.openPopup();
			  driverOnMap.bindPopup(driverMsg);
		  } else {
			  if(driverOnMap._popup._isOpen) {
				  driverOnMap.closePopup();
				  driverOnMap.bindPopup(driverMsg);
				  driverOnMap.openPopup();
			  }
			  
		  }
		  
			 
	  } else {
		  self.renderNewDriverOnMap(driverEvent, truckSymbolSize);
	  }
  }
  

  
  self.renderNewDriverOnMap = function (driverEvent, truckSymbolSize) {
	    var randomColor = '#' + (Math.random() * 0xFFFFFF << 0).toString(16);
	  	var driverOnMap = L.circle([driverEvent.latitude, driverEvent.longitude], truckSymbolSize, {
	        color: randomColor,
	        fillColor: randomColor,
	        fillOpacity: 0.8
	    }).addTo(map);   
	  	
		var driverMsg = self.constructMessage(driverEvent.driverId, driverEvent.numberOfInfractions, driverEvent.infractionEvent, driverEvent.driverName, driverEvent.routeId, driverEvent.routeName, false);
	  	driverOnMap.bindPopup(driverMsg);
	  	var driverDetails = {driverEvent:driverEvent, driverOnMap:driverOnMap};
	  	driverOnMapLookup[driverEvent.driverId] = driverDetails;	  
	    
  
  }; 
  
  self.constructMessage = function(driverId, numberOfInfractions, lastViolation, driverName, routeId, routeName, alertDriver) {
	  
	  var coreMessage = 	  	
	    "<b>Driver Name: </b> " + driverName +
	  	"</br>" + 
	  	"<b>Route Name: </b> " + routeName +
	  	"</br>" +  
	    "<b>Violation Count: </b>" + numberOfInfractions +
	    "</br>" +
	    "<b>Last Violation: </b>" + lastViolation +
	    "</br>";
	  
	  if(alertDriver) {
		  //console.log("alertDriver is true");
		  var alertMsg = "<b><h5>CONTACT DRIVER IMMEDIATELY</h5></b>";
		  coreMessage = coreMessage + alertMsg;
	  }
	  
	  var message= " <div> "+ coreMessage  + "</div>";
	  return message;
	};

};


function DriverRow(data) {
  var self = this;

  self.truckDriverEventKey = data.truckDriverEventKey;
  self.driverId = data.driverId;
  self.driverName = data.driverName;
  
 
  
  self.timeStampString = ko.observable(data.timeStampString);
  self.longitude = ko.observable(data.longitude);
  self.latitude = ko.observable(data.latitude);
  self.infractionEvent = ko.observable(data.infractionEvent);
  self.numberOfInfractions = ko.observable(data.numberOfInfractions);
  self.truckId = ko.observable(data.truckId);
  self.routeId = ko.observable(data.routeId);
  self.routeName = ko.observable(data.routeName);
  self.rowClass=ko.observable("");
  
  self.updateEvent = function(driverEvent) {
	  	
	    self.timeStampString(driverEvent.timeStampString);
	    self.longitude(driverEvent.longitude);
	    self.latitude(driverEvent.latitude);
	    self.infractionEvent(driverEvent.infractionEvent);
	    self.numberOfInfractions(driverEvent.numberOfInfractions);
	    self.routeId(driverEvent.routeId);
	    self.truckId(driverEvent.truckId);
	    self.routeName(driverEvent.routeName);

  };  
  
  self.highlight = function() {
	  self.rowClass("highlight");
  };
 
  self.unHighlight = function() {
	  self.rowClass("");
  };  

};




