
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
		if (!gameHandler._isGameOver()) {
			setTimeout(function() {
				if (!gameHandler._isWaiting() && !messageQueue.isEmpty()) {
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
			this._showInfo("Loading game...");
			this.gameOverIndicator = false;
			this.waitingIndicator = false;
			this.userName = dom.byId("userSpan").innerHTML
			this.otherUserName = ""
			runTopicLoop(this);
			topic.subscribe(topicName, lang.hitch(this, function(object) { //object is: {functionName: xx, args: yy}
//				executeFunctionByName(object.functionName, this, object.args);
				console.log("Topic message received: " + object);
				messageQueue.enqueue({ context: this, functionName: object.message, args: object.messageObject});
			}));
		},
		
		_isGameOver: function() {
			return this.gameOverIndicator;
		},
	
		_isWaiting: function() {
			return this.waitingIndicator;
		},
	
		gameInfo: function(gameData) {
			//Initialise other user name
			if (gameData.player1 === this.userName) this.otherUserName = gameData.player2
			else this.otherUserName = gameData.player1

			//Insert names in score table
			var plOne = dom.byId("playerOneName");
			domConstruct.empty(plOne);
			plOne.appendChild(document.createTextNode(gameData.player1));
			var plTwo = dom.byId("playerTwoName");
			domConstruct.empty(plTwo);
			plTwo.appendChild(document.createTextNode(gameData.player2));
			
			//Create game table with images 
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
	       	
	       	//Initialise first move
	       	if (gameData.yourMove === true) {
	       		this.yourMove(true);
	       	} 
	       	else {
	       		this._showInfo(this.otherUserName + " turn. Please wait...");
	       	}
		},
		
		//Called when other player selects first cell
		firstCellSelected: function(object) {
			var tdCell = dom.byId(object.firstCell);
			domClass.remove(tdCell.children[0], "hide");
			this.firstCell = tdCell;
		},
		
		//Called when other player selects second cell
		secondCellSelected: function(object) {
			var tdCell = dom.byId(object.secondCell);
			domClass.remove(tdCell.children[0], "hide");
			this.secondCell = tdCell;
			//Don't receive messages until waiting is done
			this.waitingIndicator = true;
			//Make sure wait 3 seconds before receiving message again
			setTimeout(lang.hitch(this, function() {
				this._checkResult(this.firstCell, this.secondCell);
				this.waitingIndicator = false;
			}), 3000);
		},
		
		yourMove: function(object) {
			if (object.isNotFirst && object.isNotFirst === true) {
				this._showInfo('You scored! Please select another two squares.');
			}
			else this._showInfo('Your turn. Please select two squares.');
			this._addClickHandling();
		},
				
		_showInfo: function(message) {
			domConstruct.empty(this.infoDiv);
			domConstruct.place("<p>" + message + "</p>", this.infoDiv);
		},
		
		gameOver: function(object) {
			this.controller.gameOver();
			this.gameOverIndicator = true;
//	        var infoDiv = dom.byId('infoDiv');
//	        infoDiv.innerHTML = "";
	        var resultString;
	        if (object.winner) {
	        	resultString = object.winner + ' has won!';
	        	if (object.winner === this.userName) domClass.add(this.infoDiv, "winner-color");
	        	else domClass.add(this.infoDiv, "looser-color");
	        }
	        else resultString = 'It is a draw';
	        
	        this._showInfo("Game over. " + resultString);
	        domClass.remove(dom.byId("returnDiv"), "hide");
	        
//	        domConstruct.place('<h3>Game over. ' + resultString + '</h3>', infoDiv);			
//	        dojoObj.domConstruct.place('h3>Game over.</h3>', infoDiv);			
		},
		
		showScore: function(score) {
			document.getElementById("playerOneScore").innerHTML = score.player1;
			document.getElementById("playerTwoScore").innerHTML = score.player2;
		},
		
		_reset: function() {
			this.firstCell = null;
			this.secondCell = null;
			this.removeHandler = null;
		},
		
		_addClickHandling: function() {
			var availableList = query("td.available");
			availableList.addClass("selectCursor");
			this.removeHandler = availableList.on("click", lang.hitch(this, function(event) {
				this._handleCellClick(event.target);
			}));
      	},
      	
        _removeClickHandling: function() {
            if (this.removeHandler) {
                    this.removeHandler.remove();
            }
            query("td.selectCursor").removeClass("selectCursor");
        },

		_handleCellClick: function(tdCell) {
			domClass.remove(tdCell.children[0], "hide");
			domClass.remove(tdCell, "selectCursor");
			if (this.firstCell == null) {
				this.firstCell = tdCell;
				this.controller.firstCellSelected(this.firstCell.id);
				this._showInfo("Well done. Please select a second square.");
			}
			else {
				//Second cell, remove click listener, Wait a few secs and then check result 
				this.secondCell = tdCell;
				this._removeClickHandling();
				var isScore = this.firstCell.children[0].src === this.secondCell.children[0].src
				if (isScore) {
					this._showInfo("Well done!");
				}
				else {
					this._showInfo("Ouch, better luck next time...");
				}
				//Don't receive messages until waiting is done
				this.waitingIndicator = true;
				this.controller.secondCellSelected(this.firstCell.id, this.secondCell.id);
				//Make sure wait 3 seconds before receiving message again
				setTimeout(lang.hitch(this, function() {
					this._checkResult(this.firstCell, this.secondCell);
					if (!isScore) {
						this._showInfo(this.otherUserName + " turn. Please wait...");
					}
					this.waitingIndicator = false;
				}), 3000);
			}
		},
		
		//Checks the result and takes action accordingly
		_checkResult: function(firstCell, secondCell) {
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
			this._reset();
		}

	});
});

