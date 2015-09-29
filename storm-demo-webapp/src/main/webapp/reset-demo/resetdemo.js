
function ApplicationModel(stompClient) {
  var self = this;

  self.username = ko.observable();
  self.resetDemo = ko.observable(new ResetDemoModel(stompClient));

  self.logout = function() {
	    stompClient.disconnect();
	    window.location.href = "../logout.html";
  }  
}



function ResetDemoModel(stompClient) {
	  var self = this;

	  self.truncateHbaseTables = ko.observable("");

	  self.resetDemo = function() {

		  var resetDemoData = {
	        "truncateHbaseTables" : self.truncateHbaseTables()
		  };
	    
	   	  console.log(resetDemoData);
	   	  stompClient.send("/app/resetDemo", {}, JSON.stringify(resetDemoData));
	  }
}