
define([
        'dojo/dom',
        'dojo/dom-construct',
        'dojo/query',
        'dojo/dom-class',
        'dojo/dom-style',
        'dojo/topic',
        'dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/NodeList-dom',
        'dojo/domReady!'
        ], function (dom, domConstruct, query, domClass, domStyle, topic, declare, lang) {
		
	/**
	 * Executes a function with args and within a context
	 */
	var executeFunctionByName = function (context, functionName  /*, args */) {
	    var args = Array.prototype.slice.call(arguments, 2);
	    var namespaces = functionName.split(".");
	    var func = namespaces.pop();
	    for (var i = 0; i < namespaces.length; i++) {
	        context = context[namespaces[i]];
	    }
	    return context[func].apply(context, args);
	};
	
	/**
	 * A queue implementation
	 */
	var messageQueue = function() {
		var queue = [];
		return {
			//object should be { context: this, functionName: functionName, args: args }
			enqueue: function(object) {
				queue.push(object);
			},
			dequeue: function() {
				return queue.shift();
			},
			isEmpty: function() {
				return queue.length <= 0;
			}
		};
	}();
	
	//Runs a loop that looks for new messages (from topic)
	var runTopicLoop = function(gameHandler) {
		if (!gameHandler.isGameOver()) {
			setTimeout(function() {
				if (!gameHandler.isWaiting() && !messageQueue.isEmpty()) {
					var object = messageQueue.dequeue();
					executeFunctionByName(object.context, object.functionName, object.args);
				}
				runTopicLoop(gameHandler);
			}, 300);
		}
	};
	
	
	return declare(null, {
		constructor: function(controller, topicName){
			this.controller = controller;
			this.topicName = topicName;
			this.firstCell = null;
			this.secondCell = null;
			this.removeHandler = null;
			this.infoDiv = dom.byId('infoDiv');
			this.showInfo("Loading game...");
			this.gameOver = false;
			this.waiting = false;
			runTopicLoop(this);
			topic.subscribe(topicName, lang.hitch(this, function(object) { //object is: {functionName: xx, args: yy}
//				executeFunctionByName(object.functionName, this, object.args);
				console.log("Topic message received: " + object);
				messageQueue.enqueue({ context: this, functionName: object.message, args: object.messageObject});
			}));
		},
		
		isGameOver: function() {
			return this.gameOver;
		},
	
		isWaiting: function() {
			return this.waiting;
		},
	
		gameInfo: function(gameData) {
			var plOne = dom.byId("playerOneName");
			domConstruct.empty(plOne);
			plOne.appendChild(document.createTextNode(gameData.player1));
			var plTwo = dom.byId("playerTwoName");
			domConstruct.empty(plTwo);
			plTwo.appendChild(document.createTextNode(gameData.player2));
	       	var tbody = domConstruct.create("tbody");
	       	var currentRow = null;
	       	var imArray = Object.keys(gameData.images);
	       	var imagesPerRow = Math.min(5, Math.floor(imArray.length / 2));
	       	imArray.forEach(function(value, index, array) {
	        	if (index % imagesPerRow == 0) {
	        		currentRow = domConstruct.create("tr");
	        	}
	        	domConstruct.place('<td id="' + value + '" class="available"><img class="gameImage hide" alt="1" src="' + gameData.images[value] + '"></td>', currentRow);
	        	if ((index + 1) % imagesPerRow == 0 || index == array.length - 1) {
	        		//place row in tbody
	        		domConstruct.place(currentRow, tbody);
	        	}
	         });
	       	domConstruct.place(tbody, "gameTable");	
		},
		
		moveResult: function(moveResultData) {
			if (moveResultData.score) this.showScore(moveResultData.score);
			if (moveResultData.previousMove) this.previousMove(moveResultData);
			//The below must be put on queue as previous move will hang for the user to see
			if (moveResultData.yourMove) {
				messageQueue.enqueue({ context: this, functionName: "yourMove", args: moveResultData.yourMove});
			}
			if (moveResultData.gameOver) {
				messageQueue.enqueue({ context: this, functionName: "showGameOver", args: moveResultData.gameOver});
			}
		},
		
		yourMove: function(isFirst) {
			this.showInfo('Your turn. Please select a square.');
			this.addClickHandling();
		},
		
		//handle showing previous move and then enqueing next action
		previousMove: function(move) {
			var prevMove = move.previousMove;
			this.waiting = true;
			this.firstCell = dom.byId(prevMove.id1);
			this.secondCell = dom.byId(prevMove.id2);
			domClass.remove(this.firstCell.children[0], "hide");
			domClass.remove(this.secondCell.children[0], "hide");
			this.showInfo("Other player made a move...");
			setTimeout(lang.hitch(this, function() {
				this.checkResult(this.firstCell, this.secondCell);
				this.showInfo("");
				this.waiting = false;
			}), 3000);
		},
		
		showInfo: function(message) {
			domConstruct.empty(this.infoDiv);
			domConstruct.place("<p>" + message + "</p>", this.infoDiv);
		},
		
		showGameOver: function() {
			this.gameOver = true;
	        var infoDiv = dom.byId('infoDiv');
	        infoDiv.innerHTML = "";
	        var resultString;
	        if (game.player1.points > game.player2.points) {
	        	resultString = game.player1.name + ' has won!';
	        }
	        else if (game.player1.points < game.player2.points) {
	        	resultString = game.player2.name + ' has won!';
	        }
	        else {
	        	resultString = 'It is a draw';
	        }
	        showInfo("Game over. " + resultString);
//	        domConstruct.place('<h3>Game over. ' + resultString + '</h3>', infoDiv);			
//	        dojoObj.domConstruct.place('h3>Game over.</h3>', infoDiv);			
		},
		
		showScore: function(score) {
			document.getElementById("playerOneScore").innerHTML = score.player1;
			document.getElementById("playerTwoScore").innerHTML = score.player2;
		},
		
		reset: function() {
			this.firstCell = null;
			this.secondCell = null;
			this.removeHandler = null;
		},
		
		addClickHandling: function() {
			var availableList = query("td.available");
			availableList.addClass("selectCursor");
			this.removeHandler = availableList.on("click", lang.hitch(this, function(event) {
				this.handleCellClick(event.target);
			}));
      	},
      	
		removeClickHandling: function() {
			if (this.removeHandler) {
				this.removeHandler.remove();
			}
			query("td.selectCursor").removeClass("selectCursor");
      	},
      	
		handleCellClick: function(tdCell) {
			domClass.remove(tdCell.children[0], "hide");
			domClass.remove(tdCell, "selectCursor");
			if (this.firstCell == null) {
				this.firstCell = tdCell;
				this.controller.firstCellSelected(this.firstCell);
				this.showInfo("Well done. Please select a second square.");
			}
			else {
				//Second cell, remove click listener, Wait a few secs and then check result 
				this.secondCell = tdCell;
				this.removeClickHandling();
				if (this.firstCell.children[0].src === this.secondCell.children[0].src) {
					this.showInfo("Well done...");
				}
				else {
					this.showInfo("Ouch, better luck next time...");
				}
				//Don't receive messages until waiting is done
				this.waiting = true;
				this.controller.secondCellSelected(this.firstCell, this.secondCell);
				//Make sure wait 3 seconds before receiving message again
				setTimeout(lang.hitch(this, function() {
					this.checkResult(this.firstCell, this.secondCell);
					this.showInfo("Please wait for second players move...");
					this.waiting = false;
				}), 3000);
			}
		},
		
		//Checks the result and takes action accordingly
		checkResult: function(firstCell, secondCell) {
			if (this.firstCell.children[0].src === this.secondCell.children[0].src) {
				domClass.remove(this.firstCell, "available");
				domClass.remove(this.secondCell, "available");
				domConstruct.empty(this.firstCell);
				domConstruct.empty(this.secondCell);
			}
			else {
				domClass.add(this.firstCell.children[0], "hide");
				domClass.add(this.secondCell.children[0], "hide");
			}
			this.reset();
//			this.controller.secondCellSelected(this.secondCell);
		}

	});
});

//var memory = memory || {};
//
//memory.ArrayFunctions = (function() {
//	return {
//		shuffle: function(o) {
//		    for(var j, x, i = o.length; i; j = Math.floor(Math.random() * i), x = o[--i], o[i] = o[j], o[j] = x);
//		    return o;			
//		},
//		duplicate: function(a) {
//			var temp = [];
//			a.forEach(function(value) {
//				temp.push(value);
//				temp.push(value);
//			});
//			return temp;		
//		}
//	};
//})();
//
//memory.Game = function() {
//	this.player1 = new memory.Player('Player 1');
//	this.player2 = new memory.Player('Player 2');
//	this.currentPlayer = null;
//};
//
//memory.Game.prototype = function() {
//	var nextPlayer = function() {
//		if (this.currentPlayer === this.player1) {
//			this.currentPlayer = this.player2;
//		}
//		else {
//			this.currentPlayer = this.player1;
//		}
//	};
//
//	return {
//		nextPlayer: nextPlayer
//	};
//}();
//
//memory.Player = function(name) {
//	this.points = 0;
//	this.name = name;
//};
//
//memory.Player.prototype = function() {
//	var increment = function() {
//		this.points += 1;
//	};
//	var getPoints= function() {
//		return this.points;
//	};
//	return {
//		increment: increment,
//		getPoints: getPoints
//	};
//}();
//
//
//memory.clickManager = (function() {
//	var firstCell = null;
//	var secondCell = null;
//	var removeHandler = null;
//	var dojoObj = {};
//	var game = null;
//	return {
//		initialise: function(_dom, _domConstruct, _query, _domClass, _domStyle, _topic) {
//			dojoObj.dom = _dom;
//			dojoObj.domConstruct = _domConstruct;
//			dojoObj.query = _query;
//			dojoObj.domClass = _domClass;
//			dojoObj.domStyle = _domStyle;
//			dojoObj.topic = _topic;
//		},
//		startGame: function() {
//			game = new memory.Game();
//			game.nextPlayer();
//			this.showPlayerInfo();
//			this.addClickHandling();
//		},
//		showPlayerInfo: function(secondMove) {
//	        var infoDiv = dojoObj.dom.byId('infoDiv');
//	        infoDiv.innerHTML = "";
//	        if (secondMove) {
//	        	dojoObj.domConstruct.place('</strong>Well done ' + game.currentPlayer.name + ': Please make your another move.</strong>', infoDiv);
//	        }
//	        else {
//	        	dojoObj.domConstruct.place('</strong>' + game.currentPlayer.name + ': Please make your move.</strong>', infoDiv);
//	        }
//		},
//		showGoodMoveInfo: function() {
//	        var infoDiv = dojoObj.dom.byId('infoDiv');
//	        infoDiv.innerHTML = "";
//	        dojoObj.domConstruct.place('<i>Nice move...</i>', infoDiv);
//		},
//		showBadMoveInfo: function() {
//	        var infoDiv = dojoObj.dom.byId('infoDiv');
//	        infoDiv.innerHTML = "";
//	        dojoObj.domConstruct.place('<i>Tough luck...</i>', infoDiv);
//		},
//		showGameOver: function() {
//	        var infoDiv = dojoObj.dom.byId('infoDiv');
//	        infoDiv.innerHTML = "";
//	        var resultString;
//	        if (game.player1.points > game.player2.points) {
//	        	resultString = game.player1.name + ' has won!';
//	        }
//	        else if (game.player1.points < game.player2.points) {
//	        	resultString = game.player2.name + ' has won!';
//	        }
//	        else {
//	        	resultString = 'It is a draw';
//	        }
//	        dojoObj.domConstruct.place('<h3>Game over. ' + resultString + '</h3>', infoDiv);			
////	        dojoObj.domConstruct.place('h3>Game over.</h3>', infoDiv);			
//		},
//		showScore: function() {
//			document.getElementById("playerOneScore").innerHTML = game.player1.points;
//			document.getElementById("playerTwoScore").innerHTML = game.player2.points;
//		},
//		setRemoveHandler: function(rh) {
//			removeHandler = rh;
//		},
//		reset: function() {
//			firstCell = null;
//			secondCell = null;
//			removeHandler = null;
//		},
//		addClickHandling: function() {
//			removeHandler = dojoObj.query("td.available").on("click", function() {
//				memory.clickManager.handleCellClick(this);
//			});
//      	},
//		removeClickHandling: function() {
//			removeHandler.remove();
//      	},
//		handleCellClick: function(tdCell) {
//			dojoObj.domClass.remove(tdCell.children[0], "hide");
//			if (firstCell == null) {
//				firstCell = tdCell;
//			}
//			else {
//				//Second cell, remove click listener, Wait a few secs and then check result 
//				secondCell = tdCell;
//				this.removeClickHandling();
//				if (firstCell.children[0].src === secondCell.children[0].src) {
//					this.showGoodMoveInfo();
//				}
//				else {
//					this.showBadMoveInfo();
//				}
//				setTimeout(function() {
//					memory.clickManager.checkResult.call(memory.clickManager);
//				}, 3000);
//			}
//		},
//		//Checks the result and takes action accordingly
//		checkResult: function() {
//			if (firstCell.children[0].src === secondCell.children[0].src) {
//				game.currentPlayer.increment();
//				dojoObj.domClass.remove(firstCell, "available");
//				dojoObj.domClass.remove(secondCell, "available");
//				dojoObj.domConstruct.empty(firstCell);
//				dojoObj.domConstruct.empty(secondCell);
////				dojoObj.domStyle.set(firstCell, "backgroundImage", "");
////				dojoObj.domStyle.set(secondCell, "backgroundImage", "");
//				this.showScore();
//				this.reset();
//				if (dojoObj.query(".available").length <= 0) {
//					//None left so game over
//					this.showGameOver();
//				}
//				else {
//					this.showPlayerInfo(true);
//					this.addClickHandling();
//				}
//			}
//			else {
//				dojoObj.domClass.add(firstCell.children[0], "hide");
//				dojoObj.domClass.add(secondCell.children[0], "hide");
//				this.reset();
//				game.nextPlayer();
//				this.showPlayerInfo();
//				this.addClickHandling();
//			}
//		}
//	};
//})();
//
//
////Store images locally temp
//var images = ["images/panda.jpg", "images/frog.jpg", "images/cheetah.jpg"];
////The shuffled array of images containing two of each
//var shuffledImages = memory.ArrayFunctions.shuffle(memory.ArrayFunctions.duplicate(images));
//
//require([
//        'dojo/dom',
//        'dojo/dom-construct',
//        'dojo/query',
//        'dojo/dom-class',
//        'dojo/dom-style',
//        'dojo/topic',
//        'dojo/domReady!'
//    ], function (dom, domConstruct, query, domClass, domStyle, topic) {
//	
//		memory.clickManager.initialise(dom, domConstruct, query, domClass, domStyle, topic);
//		
//        var greetingNode = dom.byId('infoDiv');
//        domConstruct.place('<i> Dojo!</i>', greetingNode);
// 
//       	var tbody = domConstruct.create("tbody");
//       	var currentRow = null;
//       	var imagesPerRow = Math.min(5, Math.floor(shuffledImages.length / 2));
//       	shuffledImages.forEach(function(value, index, array) {
//        	if (index % imagesPerRow == 0) {
//        		currentRow = domConstruct.create("tr");
//        	}
//        	domConstruct.place("<td class='available'><img class='gameImage hide' alt='1' src=" + value + "></td>", currentRow);
//        	if ((index + 1) % imagesPerRow == 0 || index == array.length - 1) {
//        		//place row in tbody
//        		domConstruct.place(currentRow, tbody);
//        	}
//         });
//       	domConstruct.place(tbody, "gameTable");
//
//       	memory.clickManager.startGame();
//    });
//
