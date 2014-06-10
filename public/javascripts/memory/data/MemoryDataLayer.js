/**
 * 
 */
//var memory = memory || {};
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
////Store images locally temp
//var images = ["images/memory/panda.jpg", "images/memory/frog.jpg", "images/memory/cheetah.jpg", "images/memory/elephant.jpg"];
////The shuffled array of images containing two of each
//var shuffledImages = memory.ArrayFunctions.shuffle(memory.ArrayFunctions.duplicate(images));
//
//var imageWithIdArray = [];
//for (var i = 0; i < shuffledImages.length; i++) {
//	var id = i + 1;
//	imageWithIdArray.push({id: "pos-" + id, image: shuffledImages[i]});
//}
//

define(["dojo/_base/declare", "dojo/topic", "dojo/request" , "dojo/_base/lang",], function(declare, topic, request, lang) {
	
	return declare(null, {
		constructor: function(topicName){
			this.topicName = topicName;
			this.gameId = window.location.pathname.match(/\/memory\/(.*)/)[1];
			this.relativeUrl = "/memory/" + this.gameId + "/";
		},
    
		startGame: function() {	
			//Start loop for getting messages
			this.getMessages();
			
//			//Gameinfo
//			topic.publish(this.topicName, {
//				functionName: "gameInfo",
//				args : {
//					images: imageWithIdArray,
//					player1: "Jonas",
//					player2: "Moya"	
//				}
//			});
//			
//			//Your turn
//			topic.publish(this.topicName, {
//				functionName: "yourMove",
//				args : true
//			});
	    },
	    
	    firstCellSelected: function(firstCell) {
	    	this.sendClientMessage({
	    		message: "firstCellSelected",
	    		messageObject: {
	    			firstCell: firstCell
	    		}
	    	});
	    },
	    
	    secondCellSelected: function(firstCell, secondCell) {
	    	this.sendClientMessage({
	    		message: "secondCellSelected",
	    		messageObject: {
	    			firstCell: firstCell,
	    			secondCell: secondCell
	    		}
	    	});
//	    	var self = this;
//	    	window.setTimeout(function() {
//	    		topic.publish(self.topicName, { 
//	    			functionName: "moveResult",
//	    			args: {
//		    			score: { 
//		    				player1: 2,
//							player2: 2
//						}
//	    			}
//	    		});
//	    	}, 2000);
//	    	window.setTimeout(function() {
//	    		topic.publish(self.topicName, { 
//	    			functionName: "moveResult",
//	    			args: {
//		    			score: { 
//		    				player1: 2,
//							player2: 3
//						},
//						previousMove: {
//							id1: "pos-1",
//							id2: "pos-3"
//						},
//						yourMove: true
//	    			}
//	    		}	
//	    		);
//	    	}, 4000);
	    },
	    
	    //Private methods
	    
	    //Retrieve messages periodically from server
	    getMessages: function (){
	        // Request the JSON data from the server every second
	    	setTimeout(lang.hitch(this, function() {
		        request.get(this.relativeUrl + "getMessages", {
		            // Parse data from JSON to a JavaScript object
		            handleAs: "json"
		        }).then(lang.hitch(this, function(data){
		        	if (data.message !== "empty") {
		        		console.log("Message received: " + JSON.stringify(data));
		        		topic.publish(this.topicName, data);	  
		        	}
		        }),function(error) {
		        	console.error("Error message received: " + error);	        	
		        });
		        //keep the loop going
		        this.getMessages();
	    	}), 2000);
	    },
	    
	    //Send JSON message to server
	    sendClientMessage: function(json) {
	    	$.ajax({
	            url: this.relativeUrl + "clientMessage",
	            type: "POST",
	            data: json,
	            contentType: 'application/json; charset=utf-8',
	            dataType: "json",
	            async: false,
	            success: function(msg) {
	                alert(msg);
	            },
	            error: function(xhr, text, error) {
	            	console.log("Error: " + text + " " + error);
	            }
	        });	

//	        var xhrArgs = {
//	        	      url: this.relativeUrl + "clientMessage",
//	        	      postData: dojo.toJson(json),
//	        	      handleAs: "text",
//	        	      load: function(data){
//	        	    	  console.log("clientMessage: ", JSON.stringify(json));
//	        	      },
//	        	      error: function(error){
//	        	    	  console.log("Error clientMessage: (" + JSON.stringify(json) + ")" , error, e);
//	        	      }
//	        	    }
//	        	    // Call the asynchronous xhrPost
//	        	    var deferred = dojo.xhrPost(xhrArgs);

//	    	request.post(this.relativeUrl + "clientMessage", {
//	    		data: json,
//	    		handleAs: "json",
//	    	}).then(function(text){
//	    		console.log("clientMessage: ", JSON.stringify(json));
//	    	}, function(error) {
//	    		console.log("Error clientMessage: (" + json + ")" , error, e);
//	    	});
	    }

	//END DECLARE
	});

});