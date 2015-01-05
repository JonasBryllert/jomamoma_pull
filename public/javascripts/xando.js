var symbol; //default O will be X if first player
var oppSymbol;
var userId;
var oppId;
var gameId = location.pathname.match(/\/xando\/(.*)/)[1];

var clickListener = function(e) {
	$(".gamePos").off("click");
	$(this).removeClass("gamePos").html(symbol);		
	jsonObject = {
		type: "click",
		gameId: gameId,
		position: $(this).attr('id')
	}
	jsonString = JSON.stringify(jsonObject)
	console.log(jsonString);
	$("#messageDiv").text("Please wait...")
	sendClientMessage(jsonString)	
}


$( document ).ready(function() {
	//store the user id
	userId = $("#userIdSpan").text();
	oppId = $("#oppIdSpan").text();
	symbol = $("#symbolSpan").text();
	if (symbol == "X") oppSymbol = "O"
	else {
		oppSymbol = "X";
	}
	console.log("userId: " + userId + ", symbol: " + symbol + ", oppSymbol: " + oppSymbol + ", gameId: " + gameId);
	
	//Start message checking
	setInterval(function() {getMessages()}, 4000);
});

function getMessages(){
	$.getJSON("/xando/getMessages", function( data ) {
		console.log("getMessages: " + data + "  " + JSON.stringify(data));
		if (data.prevMove) {
			$("#" + data.prevMove).removeClass("gamePos").html(oppSymbol);	
		}
		if ("type" in data && data.type == "yourMove") {
			handleYourTurn(data);
		}
		else if ("type" in data && data.type == "oppMove") {
			handleOpponentTurn(data);
		}
		else if ("type" in data && data.type == "gameOver") {
			handleGameOver(data.result);
		}
//		startTimeOut();
	});
}

function handleYourTurn(data) {
	$("#messageDiv").text("Your turn " + userId + ". Please select a square.");
	$(".gamePos").on("click", clickListener);
}

function handleOpponentTurn(data) {
	var hyphon;
	var lastChar = oppId.charAt(oppId.length - 1);
	if (lastChar == 's' || lastChar == 'S') hyphon = "'";
	else hyphon = "'s";
	$("#messageDiv").text(oppId + hyphon + " turn. Please wait...")
}

function handleGameOver(result) {
	$("#messageDiv").text("Game over. "+ result);
	$("#returnDiv").removeClass("hide");
}

function sendClientMessage(json) {
	$.ajax({
        url: "/xando/clientMessage",
        type: "POST",
        data: json,
        contentType: 'application/json; charset=utf-8',
        dataType: "json",
        async: false,
        success: function(msg) {
            alert(msg);
        }
    });	
}
