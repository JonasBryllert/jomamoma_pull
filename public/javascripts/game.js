var symbol; //default O will be X if first player
var oppSymbol;
var userId;
var gameId = location.pathname.match(/\/game\/(.*)/)[1];

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
	symbol = $("#symbolSpan").text();
	if (symbol == "X") oppSymbol = "O"
	else {
		oppSymbol = "X";
	}
	console.log("userId: " + userId + ", symbol: " + symbol + ", oppSymbol: " + oppSymbol + ", gameId: " + gameId);
	
	//Start message checking
	setInterval(function() {getMessages()}, 5000);
});

function getMessages(){
	$.getJSON("/game/getMessages", function( data ) {
		console.log("getMessages: " + data + "  " + JSON.stringify(data));
		if (data.prevMove) {
			$("#" + data.prevMove).removeClass("gamePos").html(oppSymbol);	
		}
		if ("type" in data && data.type == "yourMove") {
			handleYourTurn(data);
		}
		else if ("type" in data && data.type == "gameOver") {
			handleGameOver(data.result);
		}
//		startTimeOut();
	});
}

function handleYourTurn(data) {
	$("#messageDiv").text("Your turn. Please click in selected square.")
	$(".gamePos").on("click", clickListener);
}

function handleGameOver(result) {
	$("#messageDiv").text("Game over. "+ result);
	$("#returnDiv").show();
}

function sendClientMessage(json) {
	$.ajax({
        url: "/game/clientMessage",
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
