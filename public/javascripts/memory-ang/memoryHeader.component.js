(function(angular) {
	angular.module("MemoryApp").component("memoryHeader", {
		bindings: {
			user: "<"
		},
		templateUrl: "/assets/javascripts/memory-ang/memoryHeader.component.html",
		controllerAs: "vm",
		controller: MemoryHeaderController
	});
	
	function MemoryHeaderController() {
		var vm = this;
		vm.$onInit = function() {
			console.log("MemoryHeaderController user: " + vm.user);
		}
		
	}
	
})(window.angular);