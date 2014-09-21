var app = angular.module("SinkShipApp",[]);

var size = 6;

app.service('SinkShipService', function($http) {
	
    this.getMessages = function(callback) {
//    	console.log("url: " + $location.url);
//    	console.log("path: " + $location.path);
    	return $http.get(location.pathname + "/messages");
    };
 
    this.getShipPositions = function() {
//    	console.log("url: " + $location.url());
//    	console.log("path: " + $location.path());
    	console.log("location.pathname: " + location.pathname);
    	return $http.get(location.pathname + "/ships");
    };
    
    this.cellClicked = function(pos) {
    	$http.post(location.pathname + "/messages", pos);
    };
});

app.controller("SinkShipController", function($scope, SinkShipService) {
	//Initial parameters
	$scope.playerOneName = "Loading...";
	$scope.playerTwoName = "Test...";
	
	$scope.playerOneScore = 0;
	$scope.playerTwoScore = 0;
	$scope.info = "Loading game, please wait..."
	
	$scope.gameStarted = false;
	$scope.gameOver = false;
	$scope.yourTurn = true;
	
	$scope.myTable = {};
	$scope.oppTable = {};
	
	$scope.cellClicked = function(pos) {
		console.log("Clicked: " + pos);	
		$scope.yourTurn = false;
		document.getElementById(pos).innerHTML = "X";
		SinkShipService.cellClicked({message: "cellSelected", position: pos});
		$scope.info = "Checking if hit, please wait...";
	};
	
	$scope.handleMessage = function(data) {
		if (data.message == "yourMove" && !data.prevMove) {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			$scope.info = "Your turn. Click a cell in opponents table.";
			$scope.yourTurn = true; 
		}
		else if (data.message == "yourMove") {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			var pos = data.prevMove.pos;
			document.getElementById("myTable-" + pos).innerHTML = "X";
			var isHit = !!data.prevMove.isHit;
			var isSunk = !!data.prevMove.isSunk;
			var infoMessage = "";
			if (isHit) {
				if (isSunk) {
					infoMessage = "Opponent sunk one of your ships. Your turn.";
				}
				else {
					infoMessage = "Opponent hit one of your ships. Your turn.";
				}
			}
			else {
				infoMessage = "Opponent missed. Your turn.";
			}
			$scope.info = infoMessage;
			$scope.yourTurn = true; 
			$scope.oppTable[pos].ship = isHit;
			if (isSunk) {
				for (i = 0; i < data.prevMove.ship.length; i++) {
					var posString = "pos-" + data.prevMove.ship[i].x + "-" + data.prevMove.ship[i].y
					$scope.oppTable[posString].sunk = true;
				}	
			}
		}
		else if (data.message == "oppMove" && !data.prevMove && !$scope.gameStarted) {
			$scope.gameStarted = true;
			$scope.info = "Opponent starts, please wait...";
		}
		else if (data.message == "oppMove") {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			var pos = data.prevMove.pos;
			var isHit = !!data.prevMove.isHit;
			var isSunk = !!data.prevMove.isSunk;
			var infoMessage = "";
			if (isHit) {
				if (isSunk) {
					infoMessage = "Well done, you sank a ship. Wait for opponents turn...";
				}
				else {
					infoMessage = "Well done, you hit a ship. Wait for opponents turn...";
				}
			}
			else {
				infoMessage = "Oops, you missed. Wait for opponents turn...";
			}
			$scope.info = infoMessage; 
			$scope.oppTable[pos].ship = isHit;
			if (isSunk) {
				for (i = 0; i < data.prevMove.ship.length; i++) {
					var posString = "pos-" + data.prevMove.ship[i].x + "-" + data.prevMove.ship[i].y
					$scope.oppTable[posString].sunk = true;
				}	
			}		
		}
		else if (data.message == "gameOver") {
		
		}
		else console.log("Unknown message: " + data.message);
	};

	//Set up object for each table cell to 'idle'
	(function() {
		for (i = 1; i <= size; i++) {
			for (j = 1; j <= size; j++) {
				var posString = "pos-" + j + "-" + i;
				$scope.myTable[posString] = {};
				$scope.oppTable[posString] = {};
				$scope.oppTable[posString].idle = true;
//				$scope.oppTable[posString].ship = false;
//				$scope.oppTable[posString].tail = false;
//				$scope.oppTable[posString].body = false;
//				$scope.oppTable[posString].head = false;
//				$scope.oppTable[posString].hor = false;
//				$scope.oppTable[posString].ver = false;
//				$scope.oppTable[posString].sunk = false;
				
//				$scope.myTable[posString].mode = "idle";
//				$scope.oppTable[posString].mode = "idle";
			}
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
	        		$scope.handleMessage(data);
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
         	console.log("ships: " + JSON.stringify(data));
        	for (i = 0; i < data.length; i++) {
        		var ship = data[i];
        		for (j = 0; j < ship.length; j++) {
        			var pos = ship[j];
        			var posString = "pos-" + pos.x + "-" + pos.y
        			console.log("posString: " + posString);
        			$scope.myTable[posString].idle = false;
        			$scope.myTable[posString].ship = true;
        			if (j == 0) $scope.myTable[posString].tail = true;
        			else if (j == ship.length - 1) $scope.myTable[posString].head = true;
        			else $scope.myTable[posString].body = true;
        			if (ship[0].x == ship[1].x) $scope.myTable[posString].ver = true;
        			else $scope.myTable[posString].hor = true;
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