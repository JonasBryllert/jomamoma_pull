

$( document ).ready(function() {
	$("#doSend2").click(function() {
		var text = $("#theText").val()
    	writeToScreen("SENT: " + text);  
    	websocket.send(text); 	
	});
	
    output = document.getElementById("output");

	startWS();
});

function startWS() {    
	//wsUri = "ws://echo.websocket.org/";
	wsUri = "ws://localhost:9000/ws";
//	wsUri = "ws://localhost:9000/ws2";
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
	//websocket = new WS(@"routes.Application.ws.webSocketURL()")    
	websocket = new WS(wsUri)    
	
//	websocket = new WebSocket(wsUri); 
    websocket.onopen = function(evt) { onOpen(evt) }; 
    websocket.onclose = function(evt) { onClose(evt) }; 
    websocket.onmessage = function(evt) { onMessage(evt) }; 
    websocket.onerror = function(evt) { onError(evt) }; 
}

function onOpen(evt) { 
	writeToScreen("CONNECTED"); 
//	doSend("WebSocket rocks"); 
}
 
function onClose(evt) { 
    writeToScreen("DISCONNECTED"); 
}  

function onMessage(evt) { 
	console.log(evt.data)
    writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data+'</span>'); 
 //   websocket.close(); 
}  

function onError(evt) { 
    writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data); 
}  

function doSend(message) { 
    writeToScreen("SENT: message... ");  
    websocket.send("message"); 
}  

function doSend2(message) { 
    writeToScreen("SENT: message... ");  
    websocket.send("message"); 
}  

function writeToScreen(message) { 
    var pre = document.createElement("p"); 
    pre.style.wordWrap = "break-word"; 
    pre.innerHTML = message; 
    output.appendChild(pre);     
} 
