//Javascript for memory
//CSS Concepts are:
//hidden - show , show for blocks currently selected, otherwise hidden
//available - removed , available blocks are still in the game, removed are gone

//Only global script var. Object holding all data needed
var memory = {
	gameId = location.pathname.match(/\/game\/(.*)/)[1];
};

$( document ).ready(function() {
	//store the user id
	memory.userId = $("#userIdSpan").text();
	memory.gameId =
	console.log("userId: " + userId + ", gameId: " + gameId);
	
	//Start message checking
	setInterval(function() {getMessages()}, 4000);
});


var clickListener = function(e) {
	$(".available").off("click");
	$(this).removeClass("available").html(symbol);		
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


function getMessages(){
	$.getJSON("/game/getMessages", function( data ) {
		console.log("getMessages: " + data + "  " + JSON.stringify(data));
		if (data.prevMove) {
			$("#" + data.prevMove).removeClass("available").html(oppSymbol);	
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
	$("#messageDiv").text("Your turn. Please select two.")
	$(".hidden").on("click", clickListener);
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
