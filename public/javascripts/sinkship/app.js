var app = angular.module("SinkShipApp",[]);

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
	$scope.userName = document.getElementById("userSpan").innerHTML;
	$scope.oppName = document.getElementById("oppSpan").innerHTML;
	$scope.size = parseInt(document.getElementById("sizeSpan").innerHTML);
	
	$scope.userScore = 0;
	$scope.oppScore = 0;
	
	$scope.showGameInfo = false;
	$scope.info = "Loading game, please wait..."
	
	$scope.gameStarted = false;
	$scope.gameOver = false;
	$scope.yourTurn = false;
	
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
		
		//inner closure method to insert a move into my (own) table. Opponents move end up here!
		function insertMoveInMyTable(data) {
			var pos = data.prevMove.pos;
			document.getElementById("myTable-" + pos).innerHTML = "X";
			$scope.myTable[pos].idle = false;
			$scope.myTable[pos].ship = !!data.prevMove.isHit;
			if (!!data.prevMove.isSunk) {
				$scope.oppScore += 1;
				for (i = 0; i < data.prevMove.shipPositions.length; i++) {
					var posString = "pos-" + data.prevMove.shipPositions[i].x + "-" + data.prevMove.shipPositions[i].y
					$scope.myTable[posString].sunk = true;					
				}	
			}		
		}
		
		//inner closure method to insert move into opponents table. User moves end up here!
		function insertMoveInOppTable(data) {
			var pos = data.prevMove.pos;
			var isHit = !!data.prevMove.isHit;
			var isSunk = !!data.prevMove.isSunk;
			
			$scope.oppTable[pos].idle = false;
			$scope.oppTable[pos].ship = isHit;
			if (isSunk) {
				$scope.userScore += 1;
				for (i = 0; i < data.prevMove.shipPositions.length; i++) {
					var posString = "pos-" + data.prevMove.shipPositions[i].x + "-" + data.prevMove.shipPositions[i].y
					$scope.oppTable[posString].sunk = true;
					//Add body part and vertical or horizontal for looks
					if (i == 0) $scope.oppTable[posString].tail = true;
        			else if (i == data.prevMove.shipPositions.length - 1) $scope.oppTable[posString].head = true;
        			else $scope.oppTable[posString].body = true;
        			if (data.prevMove.shipPositions[0].x == data.prevMove.shipPositions[1].x) $scope.oppTable[posString].ver = true;
        			else $scope.oppTable[posString].hor = true;
				}	
			}		
		}
		
		if (data.message == "yourMove" && !data.prevMove) {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			$scope.info = "You start. Click a cell in " + $scope.oppName + "'s table to try to hit a ship.";
			$scope.yourTurn = true; 
		}
		else if (data.message == "yourMove") {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			
			//Insert prev move to myTable and change according to hit/sunk ship
			insertMoveInMyTable(data);
			var infoMessage = "";
			if (!!data.prevMove.isHit) {
				if (!!data.prevMove.isSunk) {
					infoMessage = data.prevMove.user + " sunk one of your ships. Your turn.";
				}
				else {
					infoMessage = data.prevMove.user + " hit one of your ships. Your turn.";
				}
			}
			else {
				infoMessage = data.prevMove.user + " missed. Your turn.";
			}
			$scope.info = infoMessage;
			$scope.yourTurn = true; 
		}
		else if (data.message == "oppMove" && !data.prevMove && !$scope.gameStarted) {
			$scope.gameStarted = true;
			$scope.info = $scope.oppName + " starts, please wait...";
		}
		else if (data.message == "oppMove") {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			
			insertMoveInOppTable(data);
			var pos = data.prevMove.pos;
			var isHit = !!data.prevMove.isHit;
			var isSunk = !!data.prevMove.isSunk;
			var infoMessage = "";
			if (isHit) {
				if (isSunk) {
					infoMessage = "Well done, you sank a ship. Wait for " + $scope.oppName + "'s  turn...";
				}
				else {
					infoMessage = "Well done, you hit a ship. Wait for " + $scope.oppName + "'s turn...";
				}
			}
			else {
				infoMessage = "Oops, you missed. Wait for " + $scope.oppName + "'s turn...";
			}
			$scope.info = infoMessage; 
		}
		else if (data.message == "gameOver") {
			$scope.info = "Game over. " + data.winner + " has won!";
			$scope.gameOver = true;
			if (data.user == $scope.userName) insertMoveInOppTable(data);
			else insertMoveInMyTable(data);
		}
		else console.log("Unknown message: " + data.message);
	};

	//Set up object for each table cell to 'idle'
	(function() {
		for (i = 1; i <= $scope.size; i++) {
			for (j = 1; j <= $scope.size; j++) {
				var posString = "pos-" + j + "-" + i;
				$scope.myTable[posString] = {};
				$scope.oppTable[posString] = {};
				$scope.oppTable[posString].idle = true;
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
    	}, 2000);
    })();
    	
    (function getShips() {
		var promise = SinkShipService.getShipPositions();
		promise.success(function(data, status, headers, config) {
         	console.log("ships: " + JSON.stringify(data));
        	for (i = 0; i < data.length; i++) {
        		var shipPositions = data[i];
        		for (j = 0; j < shipPositions.length; j++) {
        			var pos = shipPositions[j];
        			var posString = "pos-" + pos.x + "-" + pos.y
        			console.log("posString: " + posString);
        			$scope.myTable[posString].idle = false;
        			$scope.myTable[posString].ship = true;
        			if (j == 0) $scope.myTable[posString].tail = true;
        			else if (j == shipPositions.length - 1) $scope.myTable[posString].head = true;
        			else $scope.myTable[posString].body = true;
        			if (shipPositions[0].x == shipPositions[1].x) $scope.myTable[posString].ver = true;
        			else $scope.myTable[posString].hor = true;
        		}
        	}
		});
		promise.error(function(data, status, headers, config) {
			console.log("ERROR!!!");
		});
    })();
		
});
