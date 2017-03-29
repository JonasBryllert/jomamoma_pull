(function(angular) {
	var app = angular.module("MemoryApp",[])
	app.component("memoryApp", {
		bindings: {
			user: '@',
			size: '<'
		},
		templateUrl: "/assets/javascripts/memory-ang/app.component.html",
		controllerAs: "vm",
		controller: ['$scope', '$http', '$timeout', 'MessageService', AppController]
	});	
	
	function AppController($scope, $http, $timeout, MessageService) {
		var vm = this;
		console.log("AppController created");
		var wait = false;
		var queue = [];
		vm.isBootstrapped = false;
		vm.gameOver = false;
		vm.gameId = window.location.pathname.match(/\/memory\/(.*)/)[1];
		vm.relativeUrl = "/memory/" + this.gameId + "/";
		vm.playerNames = {
			player1: "",
			player2: ""
		};
		vm.playerScores = {
			player1: 0,
			player2: 0
		};
		vm.myTurn = false;
		vm.message = "Loading game, please wait";
		
		vm.$onInit = function() {
			MessageService.startGetMessageLoop(vm.relativeUrl+'getMessages', vm.messageCallback);
//			startGetMessageLoop($http, $timeout, vm.relativeUrl+'getMessages', vm.messageCallback);
			vm.isBootstrapped = true;
			console.log("AppController.init(), user: " + vm.user + ", size: " + vm.size);
		}
		
		vm.messageCallback = function(message) {
			if (wait) {
				queue.push(message);
				return;
			}
			if (message.message == "gameInfo") {
				vm.playerNames.player1 = message.messageObject.player1;
				vm.playerNames.player2 = message.messageObject.player2;
				if (vm.user == vm.playerNames.player1) {
					vm.opponent = vm.playerNames.player2;
				}
				else {
					vm.opponent = vm.playerNames.player1;
				}
				
				vm.images = message.messageObject.images;
				vm.myTurn = message.messageObject.yourMove;
				if (vm.myTurn) vm.message = "You start. Please select a cell."
				else vm.message = vm.opponent + " starts."
				
				//Add 's to opponent for coming messages..a bit dirty
				if (vm.opponent.endsWith("s")) vm.opponent += "'";
				else vm.opponent += "'s";
			}
			if (message.message == "yourMove") {
				vm.myTurn = true;
				if (message.messageObject.isFirstMove)
					vm.message = "Your turn. Please select a cell.";
				else
					vm.message = "Your turn again. Please select a cell.";
			}	
			if (message.message == "oppMove") {
				vm.myTurn = false;
				vm.message = vm.opponent + " turn.";
			}	
			if (message.message == "firstCellSelected") {
				$scope.$broadcast("oppFirstCellSelected", message.messageObject);
			}	
			if (message.message == "secondCellSelected") {
				$scope.$broadcast("oppSecondCellSelected", message.messageObject);
				//Wait so user can see picture and then continue
				wait = true;
				$timeout(function(){
					wait = false;
					$scope.$broadcast("reset");
					while (queue.length) {
						var message = queue.shift();
						vm.messageCallback(message);
					}					
				}, 2000);				
			}	
			if (message.message == "showScore") {
				vm.playerScores.player1 = message.messageObject.player1;
				vm.playerScores.player2 = message.messageObject.player2;
			}	
			if (message.message == "gameOver") {
				vm.gameOver = true;
				MessageService.setGameOver();
				if (message.messageObject.winner)
					vm.message = "Game Over. " + message.messageObject.winner + " has won!"
				else
					vm.message = "Game Over. It is a draw!"
			}	
		}
		
		$scope.$on('firstCellSelected', function (event, data) {
			vm.message = "Well done. Please select a second cell."
			MessageService.sendMessage({
		    	message: "firstCellSelected",
		    	messageObject: data
			});
		});
		
		$scope.$on('secondCellSelected', function (event, data) {
			vm.myTurn = false;
			wait = true;
			if (data.match) vm.message = "Well done. You got a pair!"
			else vm.message = "Ouch, better luck next time..."
			MessageService.sendMessage({
		    	message: "secondCellSelected",
		    	messageObject: data
			});
			$timeout(function() {
				wait = false;
				$scope.$broadcast("reset");
				while (queue.length) {
					var message = queue.shift();
					vm.messageCallback(message);
				}
			}, 2000);
		});
		
		
	
	}
})(window.angular);