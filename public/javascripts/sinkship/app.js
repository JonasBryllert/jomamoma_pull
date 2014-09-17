var app = angular.module("SinkShipApp",[]);

app.service('SinkShipService', function($http) {



    this.getMessages = function(callback) {
    	return $http.get("getMessages");
    }
 
    this.method2 = function() {
            //..
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

	//Set up object for each table cell to 'idle'
	(function() {
		for (i = 1; i <= 6*6; i++) {
			$scope["pos-" + i] = "idle";
		}
	})();
	
	//Start read messages from server
    (function getMessages (){
    	if ($scope.gameOver) return;
        // Request the JSON data from the server every second
    	setTimeout(function() {
    		var promise = SinkShipService.getMessages();
    		promise.success(function(data, status, headers, config) {
	        	if (data.message !== "empty") {
	        		console.log("Message received: " + JSON.stringify(data));
	        		//Handle message here.. 
	        	}
    			
    		});
    		promise.error(function(data, status, headers, config) {
    			console.log("ERROR!!!"
    		});
    		
   	        //keep the loop going
	        getMessages();
    	}, 2000);
    },
	
	
});

app.controller("ScoreController", function($scope) {
	$scope.playerOneName = "Loading...";
	$scope.playerTwoName = "Test...";
	
	$scope.playerOneScore = 0;
	$scope.playerTwoScore = 0;
});