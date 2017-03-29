(function(angular) {
	var app = angular.module("MemoryApp");
	var gameOver = false;
	
	app.service('MessageService', function ($http, $timeout, $location) {
	    function startGetMessageLoop(url, callback){
	    	if (gameOver === true) return;
	        // Request the JSON data from the server every second
	    	$timeout(function() {
	    		if (gameOver === true) return;
		        $http({
		        	method: 'GET',
		        	url: url
		        }).then(function(response){
		        	if (!gameOver &&response.data && response.data.message !== "empty") {
		        		console.log("Message received: " + JSON.stringify(response.data));
		        		callback(response.data);	  
		        	}
		        },function(error) {
		        	console.error("Error message received: " + error);	        	
		        });
		        //keep the loop going
		        startGetMessageLoop(url, callback);
	    	}, 1000);
	    }
	    
	    function sendMessage(message, callback) {
			var path = $location.absUrl();
			console.log('sendMessage: ' + path);
	        $http({
	        	method: 'POST',
	        	url: path + "/clientMessage",
	        	data: message
	        }).then(function(response){
	        	console.log("Message sent: " + JSON.stringify(message));
	        	if (callback && !gameOver) callback(response);
	        },function(error) {
	        	
	        	console.error("Message sending failed: " + error);	        	
	        });	    	
	    }
	    

		return {
			sendMessage: sendMessage,
		    startGetMessageLoop: startGetMessageLoop,
		    setGameOver: function() {
		    	gameOver = true;
		    }

		}
		
	});	
})(window.angular);
