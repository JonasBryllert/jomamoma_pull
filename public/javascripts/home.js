var userId;

$( document ).ready(function() {
	console.log("index ready");
	userId = $("#userIdSpan").text();
	console.log("userId: " + userId);
	
	//Start message checking
	setInterval(function() {getMessages()}, 5000);
	
	//load users logged in
	loadUsers();
	
//	$("#startGame").click(function(e) startGame());
	$("#challengeButton").click(function(e) {challenge()});
});

function enableGameSelection() {
	$("#gameSelect").removeAttr('disabled');
	$("#opponentSelect").removeAttr('disabled');
	$("#challengeButton").removeAttr('disabled');
}

function disableGameSelection() {
	$("#gameSelect").attr('disabled', 'disabled');
	$("#opponentSelect").attr('disabled', 'disabled');
	$("#challengeButton").attr('disabled', 'disabled');
}

function challenge() {
	disableGameSelection();
	var gameChoice = $("#gameSelect").val();
	var opp = $("#opponentSelect").val();
	if (!opp || "" == opp) return;
	console.log("startGame: " + opp);
	var oMessage = {type: "challenge", game: gameChoice, opponent: opp};
	var message = JSON.stringify(oMessage)
	console.log("startGame: " + message)
	$("#messageDiv").text("You have challenged " + opp + ". Waiting for resonse...")
	$("#messageDiv").show();
	sendClientMessage(message)
}

function loadUsers() {
	$.getJSON( "loadUsers", function( data ) {
		console.log("loadUsers: " + data);
		if (data instanceof Array && !data.empty) {
			var items = [];
			data.forEach(function(user) {
				console.log('<option value=' + '"' + user + '">' + user + '</option>')
				items.push( '<option value=' + '"' + user + '">' + user + '</option>' );
			});
		    allOptions = items.join( "" ) 
		    console.log("allOptions: " + allOptions)
			$( allOptions ).appendTo( "#opponentSelect" );
		}
	});	
}

function getMessages(){
	$.getJSON("getMessages", function( data ) {
		console.log("getMessages: " + data + "  " + JSON.stringify(data));
		if ("type" in data && data.type == "challenge") {
			handleChallenge(data.challenger, data.game);
		}
		else if ("type" in data && data.type == "challengeAccepted") {
			handleChallengeAccepted(data.challengee, data.gameId);
		}
		else if ("type" in data && data.type == "challengeRejected") {
			if (userId == data.challenger) {
				challengeRejected();
			}
		}
//		startTimeOut();
	});
}

function handleChallenge(challenger, gameChoice) {
	disableGameSelection();
	$("#challengerSpan").text(challenger);
	$("#challengerGameSpan").text(gameChoice);
	$("#acceptChallengeButton").one("click", function() {
		var json = JSON.stringify({type:"challengeAccepted", game: gameChoice, challenger:challenger})
		sendClientMessage(json)
	});
	$("#rejectChallengeButton").one("click", function() {
		var json = JSON.stringify({type:"challengeRejected",challenger:challenger})
		sendClientMessage(json)
		challengeRejected();
	});
	$("#challengeDiv").show();
}

function handleChallengeAccepted(challengee, gameId) {
	$("#messageDiv").text("The challenge has been accepted, please wait...")
	$("#messageDiv").show();
	window.setTimeout(function(){
		$("#challengeDiv").hide();		
		$("#messageDiv").hide();
		window.location.href = "/game/" + gameId; 	
	}, 2000);
}

function challengeRejected() {
	$("#messageDiv").text("The challenge has been rejected, please wait...")
	$("#messageDiv").show();
	window.setTimeout(function(){
		$("#challengeDiv").hide();		
		$("#messageDiv").hide();
		enableGameSelection();
	}, 3000);
}

function sendClientMessage(json) {
	$.ajax({
        url: "clientMessage",
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
