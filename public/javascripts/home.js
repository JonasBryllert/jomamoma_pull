var userId;
var userArray = [];
var state = "idle"; //idle, challenging, challenged  not used yet.

$( document ).ready(function() {
	console.log("index ready");
	userId = $("#userIdSpan").text();
	console.log("userId: " + userId);
	
	//Start message checking
	setInterval(function() {getMessages()}, 2000);
	
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

function checkShowGameDiv() {
	if (userArray.length > 0) {
		$("#startGameDiv").removeClass("hide");
		if (!$("#waitForOpponentDiv").hasClass("hide")) {
			$("#waitForOpponentDiv").addClass("hide")
		};		
	}
	else {
		if (!$("#startGameDiv").hasClass("hide")) {
			$("#startGameDiv").addClass("hide")
		};		
		$("#waitForOpponentDiv").removeClass("hide");
	}
}

function challenge() {
	disableGameSelection();
	var gameChoice = $("#gameSelect").val();
	var gameChoiceAsText = $("#gameSelect option:selected").text();
	var opp = $("#opponentSelect").val();
	if (!opp || "" == opp) return;
	console.log("startGame: " + opp);
	var oMessage = {message: "challenge", game: gameChoice, opponent: opp};
	var message = JSON.stringify(oMessage)
	console.log("startGame: " + message)
	$("#messageDiv").text("You have challenged " + opp + " for a game of " + gameChoiceAsText + ". Waiting for response...")
	$("#messageDiv").show();
	sendClientMessage(message)
}

function loadUsers() {
	$.getJSON( "loadUsers", function( data ) {
		console.log("loadUsers: " + data);
		if (data instanceof Array && !data.empty) {
			data.forEach(function(user) {
				_addUser(user);
			});
		}
		checkShowGameDiv();
	});	
}

function _addUser(user) {
	//Don't add oneself or if already exist
	if (user === userId || userArray.indexOf(user) >= 0) return;
	userArray.push(user);
	console.log("New user: " + user);
	var optionString = '<option class="Temp' + user + '" value=' + '"' + user + '">' + user + '</option>';
	$( optionString ).appendTo( "#opponentSelect" );	
}

function _removeUser(user) {
	var index = userArray.indexOf(user);
	if (index >= 0) {
		console.log("Remove user: " + user);
		userArray.splice(index, 1);
		var classToRemove = ".Temp" + user;
		$(classToRemove).remove();
	}
}

function getMessages(){
	$.getJSON("getMessages", function( data ) {
		console.log("getMessages: " + data + "  " + JSON.stringify(data));
		if (!("message" in data)) console.log("Invalid message: " + data);
		
		if (data.message === "challenge") {
			handleChallenge(data.challenger, data.game);
		}
		else if (data.message === "challengeAccepted") {
			handleChallengeAccepted(data.challengee, data.url);
		}
		else if (data.message === "challengeRejected") {
			if (userId == data.challenger) {
				handleChallengeRejected();
			}
		}
		else if (data.message === "users") {
			handleUserChange(data.messageObject);
		}
//		startTimeOut();
	});
}

function handleChallenge(challenger, gameChoice) {
	disableGameSelection();
	$("#challengerSpan").text(challenger);
	$("#challengerGameSpan").text(gameChoice);
	$("#acceptChallengeButton").one("click", function() {
		var json = JSON.stringify({message:"challengeAccepted", game: gameChoice, challenger:challenger})
		sendClientMessage(json)
	});
	$("#rejectChallengeButton").one("click", function() {
		var json = JSON.stringify({message:"challengeRejected",challenger:challenger})
		sendClientMessage(json)
		handleChallengeRejected();
	});
	$("#challengeDiv").removeClass("hide");
}

function handleChallengeAccepted(challengee, url) {
	$("#challengeDiv").addClass("hide");
	$("#messageDiv").text("The challenge has been accepted, launching game...")
	$("#messageDiv").show();
	window.setTimeout(function(){
		$("#messageDiv").hide();
		window.location.href = url; 	
	}, 2000);
}

function handleChallengeRejected() {
	$("#challengeDiv").addClass("hide");
	$("#messageDiv").text("The challenge has been rejected, please wait...")
	$("#messageDiv").show();
	window.setTimeout(function(){
		$("#messageDiv").hide();
		enableGameSelection();
	}, 3000);
}

function handleUserChange(users) {
	if (users.loggedOn) {
		users.loggedOn.forEach(function(user) {
			_addUser(user);
		});
	}
	if (users.loggedOff) {
		users.loggedOff.forEach(function(user) {
			_removeUser(user);
		});
	}
	checkShowGameDiv();
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
