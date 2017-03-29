(function(angular) {
	angular.module("MemoryApp").component("memoryRules", {
		templateUrl: "/assets/javascripts/memory-ang/memoryRules.component.html",
		controllerAs: "vm",
		controller: MemoryRulesController
	});
	
	function MemoryRulesController() {
		var vm = this;
		vm.rulesShowing = false;
		
		this.showRules = function() {
			vm.rulesShowing = true;
		}
		this.hideRules = function() {
			vm.rulesShowing = false;
		}
	}
	
})(window.angular);