/**
 * 
 */
require(["ui/MemoryUI", "controller/MemoryController", "data/MemoryDataLayer"], function(Ui, Controller, Data) {
	var topicName= "memory";
	var data = new Data(topicName);
	var controller = new Controller(data);
	var ui = new Ui(controller, topicName);
	controller.startGame();
});