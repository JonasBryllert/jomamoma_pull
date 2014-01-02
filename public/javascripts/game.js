var symbol = "O"; //default O will be X if first player
var user = "";

var clickListener = function(e) {
//	$(this).html("X");
	jsonObject = {
		type: "click",
		position: $(this).attr('id')
	}
	jsonString = JSON.stringify(jsonObject)
	console.log($(this).attr('id'));
	console.log($(this));
	console.log(jsonString);
	doSend(jsonString)	
}


$( document ).ready(function() {
	console.log($("#gameTable"))
	console.log($("#row-col:1-1"))
	var playsession = getCookie("PLAY_SESSION")
	console.log("cookies=" + document.cookie)
	console.log("PLAY_SESSION=" + playsession)
	user = playsession.substring(playsession.indexOf("user="))
	console.log("user=" + user)
	startWS();
});

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for(var i=0; i<ca.length; i++) {
	  var c = ca[i].trim();
	  if (c.indexOf(name)==0) return c.substring(name.length,c.length);
	}
	return "";
}

function startWS() {    
	//wsUri = "ws://echo.websocket.org/";
//	wsUri = "ws://localhost:9000/ws";
	wsUri = "ws://localhost:9000/wsgame";
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
	//websocket = new WS(@"routes.Application.ws.webSocketURL()")    
	websocket = new WS(wsUri)    
	
//	websocket = new WebSocket(wsUri); 
    websocket.onopen = function(evt) {
		console.log("WS CONNECTED");
//		onOpen(evt) 
	}; 
    websocket.onclose = function(evt) { 
    	console.log("WS CLOSED");
//    	onClose(evt) 
    }; 
    websocket.onmessage = function(evt) { 
    	console.log("WS MESSAGE, data: " + evt.data);
    	var object = JSON.parse(evt.data)
    	var logString = ""
    	for (var key in object) {
    		logString += ", " + key + ": " + object[key] 
    	}
    	console.log("object: " + object + ", attr: " + logString)
    	if ("entry" == object.type) {
    		$("#" + object.position).html(object.symbol);		
    	}
    	else if ("gameStarted" == object.type) {
    		
    	}
    	else if ("gameOver" == object.type) {
    		$("#infoSpan").text(object.result)
    	}
    	else if ("waiting" == object.type) {
    		symbol = "X"
    		$("#infoSpan").text("You have X. Waiting for opponent to join")
    	}
    	else if ("join" == object.type) {
//    		if (player != "X") player = "O"
    	}    	
    	else if ("nextPlayer" == object.type) {
    		if (object.symbol == symbol) {
        		$("#infoSpan").text("You have " + symbol + ". Your turn. Please click in selected position")
        		$("td").on("click", clickListener);
    		}
    		else{
        		$("#infoSpan").text("Please wait for the other players turn...")   			    			
        		$("td").off("click", clickListener);
    		}
    	}
    	else {
    		console.log("Unknown message: " + object)
    	}
//    	onMessage(evt) 
    }; 
    websocket.onerror = function(evt) { 
    	console.log("WS ERROR: " + evt.data);  
//      writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data); 
//    	onError(evt) 
    }; 
}

function doSend(message) { 
    console.log("WS SEND: " + message);  
    websocket.send(message); 
}  

