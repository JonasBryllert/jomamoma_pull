(function(angular) {
	angular.module("MemoryApp").component("memoryScores", {
		bindings: {
			playerNames: "<",
			playerScores: "<"
		},
		templateUrl: "/assets/javascripts/memory-ang/memoryScores.component.html",
		controllerAs: "vm",
		controller: MemoryScoresController
	});
	
	function MemoryScoresController() {
		var vm = this;

		vm.$onInit = function() {
			
		}
		
	}
	
})(window.angular);