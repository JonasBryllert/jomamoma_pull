

$( document ).ready(function() {
	console.log("index ready")
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
			$( allOptions ).appendTo( ".opponentSelect" );
		}
	});
//	$("#Start").click(function(event) {
//		console.log("index start")
//		$("#opponentForm").submit();
//		event.preventDefault();
		//		$.get( "loadUsers", function( data ) {
//			console.log("loadUser: " + data)
//			data.forEach(function(user) {
//				$( ".opponentSelect" ).append("<option value="+user+">"+data+"</option>");
//			})
//		});	
//	});

//	startWS();
});

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
    	if ("entry" == object.type) {
    		console.log($("#" + object.position))
    		console.log($("#2,2"))
    		$("#" + object.position).html("X");		
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

