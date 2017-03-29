(function(angular) {
	angular.module("MemoryApp").component("memoryInfo", {
		bindings: {
			message: "<",
			gameover: "<"
		},
		templateUrl: "/assets/javascripts/memory-ang/memoryInfo.component.html",
		controllerAs: "vm",
		controller: MemoryInfoController
	});
	
	function MemoryInfoController() {
		var vm = this;
		
	}
	
})(window.angular);