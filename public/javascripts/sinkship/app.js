var app = angular.module("SinkShipApp",[]);

app.controller("SinkShipController", function($scope) {
	$scope.info = "testing"
});

app.controller("ScoreController", function($scope) {
	$scope.playerOneName = "Loading...";
	$scope.playerTwoName = "Test...";
	
	$scope.playerOneScore = 0;
	$scope.playerTwoScore = 0;
});