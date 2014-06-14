/**
 * This class controls the memory game when played remote between two players
 */
/* Messages
{
	
 
 */
/**
 * Calls a function name (string) with context and arguments  
 */
//function executeFunctionByName(functionName, context /*, args */) {
//    var args = Array.prototype.slice.call(arguments, 2);
//    var namespaces = functionName.split(".");
//    var func = namespaces.pop();
//    for (var i = 0; i < namespaces.length; i++) {
//        context = context[namespaces[i]];
//    }
//    return context[func].apply(context, args);
//}
//
define(["dojo/_base/declare"], function(declare){
	
//	//The message handler is private and will talk to the UI handler so not to pollute
//	//the returned class with the message methods coming back on the topic
//	var MessageHandler = function(uiHandler) {
//		this.uiHandler = uiHandler;
//	};
//	MessageHandler.prototype = function() {
//		var imagesLoaded = function() {
//			
//		};
//		return {
//			imagesLoaded: imagesLoaded
//			//TODO more methods
//		};
//	}();
  
	return declare(null, {
		constructor: function(dataLayer){
			this.dataLayer = dataLayer;
//			this.uiHandler = new MessageHandler(uiHander);
//			this.topicName = topicName;
//			this.data = new DataLayer("memoryTopic");
//			topic.subscribe("memoryTopic", lang.hitch(this, function(object) {
//				executeFunctionByName(object.functionName, this.uiHandler, object.arguments);
//			}));
		},
    
	    startGame: function() {
	    	this.dataLayer.startGame();
	    },
	    
	    firstCellSelected: function(firstCell) {
	    	this.dataLayer.firstCellSelected(firstCell);
	    },
	    
	    secondCellSelected: function(firstCell, secondCell) {
	    	this.dataLayer.secondCellSelected(firstCell, secondCell);
	    },
	    
	    gameOver: function() {
	    	this.dataLayer.gameOver();
	    }
    
	});
});