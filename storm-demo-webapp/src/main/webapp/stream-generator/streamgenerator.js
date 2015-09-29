
function ApplicationModel(stompClient) {
  var self = this;

  self.username = ko.observable();
  self.streamGenerator = ko.observable(new StreamGeneratorModel(stompClient));

  self.logout = function() {
	    stompClient.disconnect();
	    window.location.href = "../logout.html";
  }  
}



function StreamGeneratorModel(stompClient) {
	  var self = this;
	  
	  self.eventEmitterClass2 = ko.observable("com.hortonworks.streaming.impl.domain.transport.Truck");
	  self.eventCollectorClass2 = ko.observable("com.hortonworks.streaming.impl.collectors.KafkaEventCollector");
	  self.events2 = ko.observable(200);
	  //self.routeDirectory = ko.observable("/mnt/workspaces/storm-demo-webapp/storm-demo-webapp/routes/midwest");
	  self.routeDirectory = ko.observable("/etc/storm_demo/routes/midwest");
	  self.centerCoordinatesLat = ko.observable("38.523884");
	  self.centerCoordinatesLong = ko.observable("-92.159845");
	  self.zoomLevel=ko.observable("7");
	  self.truckSymbolSize = ko.observable("10000");
	  self.delayBetweenEvents = ko.observable("1000");

	  
	  
	  self.mapRoutes = function() {

		    var streamGeneratorData = {
		        "eventEmitterClassName" : self.eventEmitterClass2(),
		        "eventCollectorClassName": self.eventCollectorClass2(),
		        "numberOfEvents": self.events2(),
		        "routeDirectory": self.routeDirectory(),
		    	"centerCoordinatesLat": self.centerCoordinatesLat(),
		    	"centerCoordinatesLong": self.centerCoordinatesLong(),
		    	"zoomLevel": self.zoomLevel(),
		    	"truckSymbolSize": self.truckSymbolSize(),
		    	"delayBetweenEvents": self.delayBetweenEvents()
		      };
		    console.log(streamGeneratorData);
		    stompClient.send("/app/mapTruckRoutes", {}, JSON.stringify(streamGeneratorData));
		  }	  
}
