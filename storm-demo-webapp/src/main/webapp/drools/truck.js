
var driver_alert_counts = {};


function ApplicationModel(stompClient, L) {
  var self = this;

  self.username = ko.observable();
  self.driverMontior = ko.observable(new DriverMonitorModel());
  self.notifications = ko.observableArray();
  self.leaf = L;
  self.truckSymbolSize = 0;


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

            self.driverMontior().initializeMap(lat, long, zoomLevel);
      });


      stompClient.subscribe("/topic/drools_events", function(message) {
          console.log('msg from /topic/drools_events: ' + message);
          self.driverMontior().renderOnMap(JSON.parse(message.body), self.truckSymbolSize);
          //setTimeout(self.driverMontior().renderOnMap(JSON.parse(message.body), self.truckSymbolSize), 2000);
      });

      //Update page with any new alerts
      stompClient.subscribe("/topic/driver_alert_notifications", function(message) {
      console.log('msg from /topic/driver_alert_notifications: ' + message);
          self.pushNotification(JSON.parse(message.body).alertNotification);
        });

      stompClient.heartbeat.outgoing = 20000;

    }, function(error) {
      console.log("STOMP protocol error " + error);
    });
  };


  self.pushNotification = function(text) {
    self.notifications.push({notification: text});
    if (self.notifications().length > 5) {
      self.notifications.shift();
    }
  };

  self.logout = function() {
    stompClient.disconnect();
    window.location.href = "../logout.html";
  };
}

function DriverMonitorModel() {
  var self = this;


  self.rows = ko.observableArray();

  var rowLookup = {};

  var driverOnMapLookup = {};
  var stopIncreasingRadius = false;



  self.initializeMap = function(lat, long, zoomLevel) {
      console.log("inside initialize map...");
      map = L.map('map').setView([lat, long], zoomLevel);

        // add an OpenStreetMap tile layer
      console.log('adding tile: http://{s}.tile.osm.org/{z}/{x}/{y}.png');
      L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(map);

      latlng = L.latLng(39.82,-98.57);
      map.panTo(latlng);
      map.zoomIn(1);

      console.log('added tile: http://{s}.tile.osm.org/{z}/{x}/{y}.png');

  };


  self.loadDangerousEvents = function(positions) {
      for ( var i = 0; i < positions.length; i++) {

        self.loadDangerousEvent(positions[i]);
    }
  };

  self.loadDangerousEvent = function (position) {
    var row = new DriverRow(position);
    self.rows.push(row);
    rowLookup[row.driverId] = row;
  };

  self.processDangerousEvent = function(driverEvent) {


        if (rowLookup.hasOwnProperty(driverEvent.driverId)) {


            if(driverEvent.eventType != 'Normal')
                rowLookup[driverEvent.driverId].highlight();


              rowLookup[driverEvent.driverId].updateEvent(driverEvent);


              if(driverEvent.eventType != 'Normal') {

                  setTimeout(function() {

                      rowLookup[driverEvent.driverId].unHighlight();

                  }, 2000);
              }


        }
        else {
            self.loadDangerousEvent(driverEvent);
        }
      };



  self.renderOnMap = function(driverEvent, truckSymbolSize) {



      if (driverOnMapLookup.hasOwnProperty(driverEvent.driverId)) {


          var driverOnMap = driverOnMapLookup[driverEvent.driverId].driverOnMap;

          // if its an alert, just change color to red and return
          if(driverEvent.raiseAlert) {

              driver_alert_counts[driverEvent.driverId] += 1;

              if(driverOnMap.getRadius() < 30000) {

                  var newRadius = driverOnMap.getRadius() * 1.1;
                  driverOnMap.setRadius(newRadius);
              }


              //console.log("New Radius is: " + newRadius);

              console.log("setting to red");
              driverOnMap.setStyle({fillColor:'red',color:'red',fillOpacity: 0.8});




          }


          else {
            var previousDriverEvent = driverOnMapLookup[driverEvent.driverId].driverEvent;


            driverOnMap.setLatLng([driverEvent.latitude, driverEvent.longitude]);

            var driverMsg;

            driverOnMapLookup[driverEvent.driverId].driverEvent = driverEvent;


            //driverOnMap.bindPopup(driverMsg);

            driverMsg = self.constructMessage(driverEvent.driverName, driverEvent.routeName, driver_alert_counts[driverEvent.driverId], alert);

            driverOnMap.bindPopup(driverMsg);
          }


      } else { // new driver

          driver_alert_counts[driverEvent.driverId] = 0;

          self.renderNewOnMap(driverEvent, truckSymbolSize);
      }

      self.processDangerousEvent(driverEvent);
  };



  self.renderNewOnMap = function (driverEvent, truckSymbolSize) {
        var color = "green";


        var driverOnMap = L.circle([driverEvent.latitude, driverEvent.longitude], truckSymbolSize, {
            color: color,
            fillColor: color,
            fillOpacity: 0.8
        }).addTo(map);

        var driverMsg = self.constructMessage(driverEvent.driverName, driverEvent.routeName, driver_alert_counts[driverEvent.driverId],false);
        driverOnMap.bindPopup(driverMsg);

        var driverDetails = {driverEvent:driverEvent, driverOnMap:driverOnMap};
        driverOnMapLookup[driverEvent.driverId] = driverDetails;



  };

  self.constructMessage = function(driverName, routeName, alertsCount, alertDriver) {

      var coreMessage =
        "<b>Driver Name: </b> " + driverName +
        "</br>" +
        "<b>Route Name: </b> " + routeName +
        "</br>" +
        "<b>Total Drools Alerts: </b>" + alertsCount +
        "</br>";

      if(alertDriver) {
          //console.log("alertDriver is true");
          var alertMsg = "<b><h5>Contact Driver Immediately </h5></b>";
          coreMessage = coreMessage + alertMsg;
      }

      var message= " <div> "+ coreMessage  + "</div>";
      return message;
    };

}


function DriverRow(data) {
  var self = this;

  self.truckDriverEventKey = data.truckDriverEventKey;
  self.driverId = data.driverId;
  self.driverName = data.driverName;


  self.routeName = ko.observable(data.routeName);
  self.timeStampString = ko.observable(data.eventTime);
  self.longitude = ko.observable(data.longitude);
  self.latitude = ko.observable(data.latitude);



  self.alertCount = ko.observable(0);


  self.rowClass=ko.observable("");

  self.updateEvent = function(driverEvent) {

      self.timeStampString(driverEvent.eventTime);
      self.routeName(driverEvent.routeName);
      self.longitude(driverEvent.longitude);
      self.latitude(driverEvent.latitude);
      self.alertCount(driver_alert_counts[driverEvent.driverId]);
  };

  self.highlight = function() {
      self.rowClass("highlight");
  };

  self.unHighlight = function() {
      self.rowClass("");
  };

}




