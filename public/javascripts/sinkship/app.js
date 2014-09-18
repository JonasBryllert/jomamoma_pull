var app = angular.module("SinkShipApp",[]);

var size = 6;

app.service('SinkShipService', function($http) {
	
    this.getMessages = function(callback) {
//    	console.log("url: " + $location.url);
//    	console.log("path: " + $location.path);
    	return $http.get(location.pathname + "/messages");
    }
 
    this.getShipPositions = function() {
//    	console.log("url: " + $location.url());
//    	console.log("path: " + $location.path());
    	console.log("location.pathname: " + location.pathname);
    	return $http.get(location.pathname + "/ships");
    }
});

app.controller("SinkShipController", function($scope, SinkShipService) {
	//Initial parameters
	$scope.playerOneName = "Loading...";
	$scope.playerTwoName = "Test...";
	
	$scope.playerOneScore = 0;
	$scope.playerTwoScore = 0;
	$scope.info = "testing"
	$scope.gameOver = false;
	$scope.myTable = {};
	$scope.oppTable = {};

	//Set up object for each table cell to 'idle'
	(function() {
		for (i = 1; i <= size; i++) {
			for (j = 1; j <= size; j++) {
				$scope.myTable["pos-" + i + "-" + j] = "idle";
				$scope.oppTable["pos-" + i + "-" + j] = "idle";
			}
		}
	})();
	
	//Start read messages from server
    (function getMessages (){
    	if ($scope.gameOver) return;
    	var self = this;
        // Request the JSON data from the server every second
    	setTimeout(function() {
    		var promise = SinkShipService.getMessages();
    		promise.success(function(data, status, headers, config) {
	        	if (data.message !== "empty") {
	        		console.log("Message received: " + JSON.stringify(data));
	        		//Handle message here.. 
	        		self.handleMessage(data);
	        	}			
    		});
    		promise.error(function(data, status, headers, config) {
    			console.log("ERROR!!!");
    		});
    		
   	        //keep the loop going
	        getMessages();
    	}, 5000);
    })();
    	
    (function getShips() {
		var promise = SinkShipService.getShipPositions();
		promise.success(function(data, status, headers, config) {
        	console.log("ships: " + data);
        	console.log("ships -j: " + JSON.stringify(data));
        	for (i = 0; i < data.length; i++) {
        		var ship = data[i];
        		for (j = 0; j < ship.length; j++) {
        			var pos = ship[j];
        			$scope.myTable["pos-" + pos.x + "-" + pos.y] = "ship";
        		}
        	}
		});
		promise.error(function(data, status, headers, config) {
			console.log("ERROR!!!");
		});
    })();
	
	
});

app.controller("ScoreController", function($scope) {
	$scope.playerOneName = "Loading...";
	$scope.playerTwoName = "Test...";
	
	$scope.playerOneScore = 0;
	$scope.playerTwoScore = 0;
});