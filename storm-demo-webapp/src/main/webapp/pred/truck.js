var driver_vprediction_counts = {};
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
            //var zoomLevel = jsonResponse.zoomLevel;
            var zoomLevel = 4;
            self.truckSymbolSize=jsonResponse.truckSymbolSize;
            self.driverMontior().initializeMap(lat, long, zoomLevel);
      });

      stompClient.subscribe("/topic/prediction_events", function(message) {
          console.log('msg from /topic/prediction_events: ' + message);
          self.driverMontior().renderPredictionOnMap(JSON.parse(message.body), self.truckSymbolSize);
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
  var successive_violation_predictions = {};
  var successive_violation_prediction_threshold = 3;

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
            if(driverEvent.prediction != 'normal')
                rowLookup[driverEvent.driverId].highlight();
            rowLookup[driverEvent.driverId].updateEvent(driverEvent);
            if(driverEvent.prediction != 'normal') {
                setTimeout(function() {
                    rowLookup[driverEvent.driverId].unHighlight();
                }, 2000);
            }
        }
        else {
            self.loadDangerousEvent(driverEvent);
        }
      };

  self.renderPredictionOnMap = function(driverEvent, truckSymbolSize) {
      if (driverOnMapLookup.hasOwnProperty(driverEvent.driverId)) {
          var driverOnMap = driverOnMapLookup[driverEvent.driverId].driverOnMap;
          var previousDriverEvent = driverOnMapLookup[driverEvent.driverId].driverEvent;
          driverOnMap.setLatLng([driverEvent.latitude, driverEvent.longitude]);
          var driverMsg;
          driverOnMapLookup[driverEvent.driverId].driverEvent = driverEvent;
          var alert = false;
          //driverOnMap.bindPopup(driverMsg);
          if(driverEvent.prediction != 'normal') {
                // increment violation prediction counters
                successive_violation_predictions[driverEvent.driverId] += 1;
                driver_vprediction_counts[driverEvent.driverId] += 1;
              if(driverOnMap.getRadius() > 40000) {
                  //console.log("Raidus is either greater than 40000 or all radisus increases have stopped " + newRadius);
                  stopIncreasingRadius = true;
              }
              if(!stopIncreasingRadius) {
                  var newRadius = driverOnMap.getRadius() * 1.1;
                  driverOnMap.setRadius(newRadius);
              }

              //console.log("New Radius is: " + newRadius);
              // if successive violation threshold reached for this driver, change color to red
              if(successive_violation_predictions[driverEvent.driverId] >= successive_violation_prediction_threshold) {
                  console.log("setting to red");
                  driverOnMap.setStyle({fillColor:'red',color:'red',fillOpacity: 0.8});
                  var predV = (driver_vprediction_counts[driverEvent.driverId] === undefined) ? 0 :
                  driver_vprediction_counts[driverEvent.driverId];
                  //alert = true;
              }

              else {
                  driverOnMap.setStyle({fillColor:'orange',color:'orange',fillOpacity: 0.8});
              }

          } else { // normal event for existing driver
              // set color back to green if threshold not reached
              if(successive_violation_predictions[driverEvent.driverId] < successive_violation_prediction_threshold) {
                 successive_violation_predictions[driverEvent.driverId] = 0;
                  console.log("setting to green");
                  driverOnMap.setStyle({fillColor:'green',color:'green',fillOpacity: 0.8});
              }
          }

          driverMsg = self.constructMessage(driverEvent.driverName, driverEvent.routeName,
                                            driverEvent.isFoggy, driverEvent.isRainy,
                                            driverEvent.certified, driver_vprediction_counts[driverEvent.driverId], alert);

          driverOnMap.bindPopup(driverMsg);
          if(alert) driverOnMap.openPopup();
      } else {
       // new driver
       // set prediction counters to zero
        successive_violation_predictions[driverEvent.driverId] = 0;
        driver_vprediction_counts[driverEvent.driverId] = 0;
        self.renderNewPredictionOnMap(driverEvent, truckSymbolSize);
      }
      self.processDangerousEvent(driverEvent);
  };

  self.renderNewPredictionOnMap = function (driverEvent, truckSymbolSize) {
        var color = "green";

        var driverOnMap = L.circle([driverEvent.latitude, driverEvent.longitude], truckSymbolSize, {
            color: color,
            fillColor: color,
            fillOpacity: 0.8
        }).addTo(map);

        var predV = (driver_vprediction_counts[driverEvent.driverId] === undefined) ? 0 : driver_vprediction_counts[driverEvent.driverId];

        var driverMsg = self.constructMessage(driverEvent.driverName, driverEvent.routeName, driverEvent.isFoggy,
                                              driverEvent.isRainy, driverEvent.certified,
                                              predV,
                                              false);
        driverOnMap.bindPopup(driverMsg);

        var driverDetails = {driverEvent:driverEvent, driverOnMap:driverOnMap};
        driverOnMapLookup[driverEvent.driverId] = driverDetails;


  };

  self.constructMessage = function(driverName, routeName, foggy, rainy, certified, vPredCount, alertDriver) {

      var coreMessage =
        "<b>Driver Name: </b> " + driverName +
        "</br>" +
        "<b>Route Name: </b> " + routeName +
        "</br>" +
        "<b>Foggy: </b>" + foggy +
        "</br>" +
        "<b>Rainy: </b>" + rainy +
        "</br>" +
        "<b>Certified: </b>" + certified +
        "</br>" +
        "<b>Predicted Violations: </b>" + vPredCount +
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
  self.timeStampString = ko.observable(data.timeStamp);
  self.hours_logged = ko.observable(data.hours_logged);
  self.miles_logged = ko.observable(data.miles_logged);
  self.routeName = ko.observable(data.routeName);
  self.isFoggy = ko.observable(data.isFoggy);
  self.isRainy = ko.observable(data.isRainy);
  self.certified = ko.observable(data.certified);
  // keep track of violation predictions for this driver
  self.vPredictionCnt = ko.observable(0);
  self.rowClass=ko.observable("");
  self.updateEvent = function(driverEvent) {
      self.timeStampString(driverEvent.timeStamp);
      self.hours_logged(driverEvent.hours_logged);
      self.miles_logged(driverEvent.miles_logged);
      self.routeName(driverEvent.routeName);
      self.isFoggy(driverEvent.isFoggy);
      self.isRainy(driverEvent.isRainy);
      self.certified(driverEvent.certified);
      self.vPredictionCnt(driver_vprediction_counts[driverEvent.driverId]);
  };
  self.highlight = function() {
      self.rowClass("highlight");
  };
  self.unHighlight = function() {
      self.rowClass("");
  };
}
