var app = angular.module("SinkShipApp",[]);

app.controller("SinkShipController", function($scope) {

});

app.controller("ScoreController", function($scope) {
	$scope.playerOneName = "Loading...";
	$scope.playerTwoName = "Test...";
	
	$scope.playerOneScore = 0;
	$scope.playerTwoScore = 0;
});