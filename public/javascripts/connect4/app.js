var app = angular.module("Connect4App",[]);

app.service('Connect4Service', function($http) {
	
    this.getMessages = function(callback) {
    	return $http.get(location.pathname + "/messages");
    };
     
    this.columnClicked = function(message) {
    	$http.post(location.pathname + "/messages", message);
    };
});

app.controller("Connect4Controller", function($scope, Connect4Service) {
	//Initial parameters
	$scope.userName = document.getElementById("userSpan").innerHTML;
	$scope.oppName = document.getElementById("oppSpan").innerHTML;
	$scope.color = document.getElementById("color").innerHTML;
	$scope.oppColor = ($scope.color == "red" ? "blue" : "red");
	$scope.oppNameWithS = /s$/.test($scope.oppName) ? $scope.oppName + "\'" : $scope.oppName + "\'s";
		
	$scope.showGameInfo = false;
	$scope.messageChanged = false;
	$scope.info = "Loading game, please wait..."
	
	$scope.gameStarted = false;
	$scope.gameOver = false;
	$scope.yourTurn = false;
	
	$scope.gameTable = {};
	//Set up object for each table cell to 'idle'
	(function() {
		for (i = 1; i <= 6; i++) {
			for (j = 1; j <= 8; j++) {
				var posString = "pos-" + i + "-" + j;
				$scope.gameTable[posString] = {};
				$scope.gameTable[posString].idle = true;
			}
		}
	})();

	$scope.isColumnIdle = function(col) {
		var posString = "pos-" + 1 + "-" + col;
		if ($scope.gameTable[posString].idle === false) return false
		else return true
	};
	
	$scope.columnClicked = function(column) {
		console.log("Clicked: " + column);	
		$scope.yourTurn = false;
		$scope.info = "Updating, please wait...";
		Connect4Service.columnClicked({message: "columnSelected", column: column});
		var row = findFirstFreeRow(column);
		var prevMove = {
			row: row,
			column: column
		}
		insertMoveInTable(prevMove, true);
	};
	
	function findFirstFreeRow(col) {
		for (var i = 6; i >= 1; i--) {
			var pos = "pos-" + i + "-" + col;
			if ($scope.gameTable[pos].idle === true) {
				return i;
			}
		}
		console.log("Error: No idle row found for column: " + col);
		return 0;
	}
	
	//inner closure method to insert a move into my (own) table. Opponents move end up here!
	function insertMoveInTable(prevMove, isMyMove/*boolean true if me*/) {
		var color = isMyMove ? $scope.color : $scope.oppColor;
		var pos = "pos-" + prevMove.row + "-" + prevMove.column;
		console.log("Mymove: " + isMyMove + ", pos: " + pos)
		$scope.gameTable[pos].idle = false;		
		$scope.gameTable[pos][color] = true;
	}
	
//	function changeMessage(newMessage) {
//		$scope.messageChanged = false;
//		$scope.info = newMessage;
////		$scope.info = "<span class='message-changed'>" + newMessage + "</span>";
//		$scope.messageChanged = true;		
//	}
	

	$scope.handleMessage = function(data) {		
		if (data.message == "yourMove" && !data.prevMove) {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			$scope.info = "You start. Click on an arrow above the game board to drop a marker in that column.";
			$scope.yourTurn = true; 
		}
		else if (data.message == "yourMove") {
			if (!$scope.gameStarted) $scope.gameStarted = true;
			
			//Insert prev move to myTable and change according to hit/sunk ship
			insertMoveInTable(data.prevMove, false);
			$scope.info = "Your turn.";
			$scope.yourTurn = true; 
		}
		else if (data.message == "oppMove" && !data.prevMove && !$scope.gameStarted) {
			$scope.gameStarted = true;
			$scope.info = $scope.oppName + " starts, please wait...";
		}
		else if (data.message == "oppMove") {
			if (!$scope.gameStarted) $scope.gameStarted = true;			
			var infoMessage = "Please wait for " + $scope.oppNameWithS + " turn...";
			$scope.info = infoMessage; 
		}
		else if (data.message == "gameOver") {
			if (data.prevMove) insertMoveInTable(data.prevMove, false);
			if (data.winner) {
				if (data.winner == $scope.userName) {
					$scope.info = "Game over. You have won!"
				}
				else {
					$scope.info = "Game over. " + data.winner + " has won!"
				}
			}
			else $scope.info = "Game over. It is a draw!" 
			$scope.gameOver = true;
		}
		else console.log("Unknown message: " + data.message);
	};
	
	//Start read messages from server
    (function getMessages (){
    	if ($scope.gameOver) return;
        // Request the JSON data from the server every second
    	setTimeout(function() {
    		var promise = Connect4Service.getMessages();
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
		
});
