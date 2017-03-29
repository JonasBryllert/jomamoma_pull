(function(angular) {
	angular.module("MemoryApp").component("memoryGame", {
		bindings: {
			images: "<",
			myTurn: "<"
		},
		templateUrl: "/assets/javascripts/memory-ang/memoryGame.component.html",
		controllerAs: "vm",
		controller: ['$scope', 'MessageService', MemoryGameController]
	});
	
	function MemoryGameController($scope, MessageService) {
		var IMAGES_PER_ROW = 5;
		var imageProperties = [];
		var firstCell = null;

		var vm = this;
		vm.images = [];
		vm.myTurn = false;
		
		vm.$onInit = function() {
			imageProperties = new Array(vm.images.length);
			for (var i = 0; i < imageProperties.length; i++) {
				imageProperties[i] = {
					available: true,
					selected: false
				}
			}
		}
		
		vm.$onChanges = function(changes) {
			if (changes.myTurn) {
				console.log("Turn changed, my turn:" + changes.myTurn.currentValue);
				if (changes.myTurn.currentValue) resetCells();
			}
			
		}
		
		vm.getRows = function() {
			var rowArray = new Array(Math.ceil(vm.images.length / 5));
			for (var i = 0; i < rowArray.length; i++) {
				rowArray[i] = i;
			}
			return rowArray;
		}
		
		vm.getImagesForRow = function(r) {
			var start = r*IMAGES_PER_ROW;
			var end = start + IMAGES_PER_ROW;
			if (end > vm.images.length) end = vm.images.length;
			return vm.images.slice(start, end);
		}
		
		vm.isAvailable = function(row, col) {
			var pos = toPos(row, col);
			return imageProperties[pos].available;
		}
		
		vm.isSelected = function(row, col) {
			var pos = toPos(row, col);
			return imageProperties[pos].selected;
		}
		
		vm.isClickable = function(row, col) {
			return vm.myTurn && vm.isAvailable(row, col) && !vm.isSelected(row, col);
		}
				
		vm.cellClicked = function(row, col) {
			console.log("Cell clicked: " + row + ", " + col);
			var pos = toPos(row, col);
			imageProperties[pos].selected = true;
			if (firstCell == null) {
				firstCell = pos;
				$scope.$emit('firstCellSelected', {'pos': pos});
			}
			else {
				var data = {
					'firstCell': firstCell,
					'secondCell': pos
				}
				if (vm.images[firstCell] == vm.images[pos]) {
					data.match = true;
					imageProperties[firstCell].available = false;
					imageProperties[pos].available = false;
				}
				$scope.$emit('secondCellSelected', data);
			}			
		}
		
		$scope.$on("reset", function(event, data) {
			resetCells();
		});
		
		$scope.$on("oppFirstCellSelected", function(event, data) {
			imageProperties[data.pos].selected = true;
		});
		
		$scope.$on("oppSecondCellSelected", function(event, data) {
			imageProperties[data.secondCell].selected = true;
			if (vm.images[data.firstCell] == vm.images[data.secondCell]) {
				imageProperties[data.firstCell].available = false;
				imageProperties[data.secondCell].available = false;
			}
		});
		
		function toPos(row,col) {
			return IMAGES_PER_ROW*row+col;
		}

		function resetCells() {
			//Reset all variables handling state between the two clicks
			for (var i = 0; i < imageProperties.length; i++) {
				imageProperties[i].selected = false;
			}
			firstCell = null;
		}
		
	}
	
	
})(window.angular);