console.log("Yep, I am running");

var OthelloService = (function() {
	return {
	    getMessages: function() {
	    	return $.getJSON(location.pathname + "/messages");
	    },
	     
	    positionClicked: function(message) {
	    	console.log("Posting json: " + JSON.stringify(message));
	    	$.ajax({
	    		type: "POST",
	    		url: location.pathname + "/messages",
	    		data: JSON.stringify(message),
	    		contentType: "application/json"
	    	});
	    }
	}
})();


var Store = function(reducer) {
	
  var state;
  var listerners = [];

  var getState = function() {
    return state;
  }

  var dispatch = function(action) {
    state = reducer(state, action);
    listerners.forEach((l) => l() );
  }

  var subscribe = function(listener) {
    if (listerners.indexOf(listener) < 0) {
      listerners.push(listener);
    }
    // return () => { listerners.splice(listerners.indexOf(listener)); }
    return function() { listerners.delete(listener); }
  }

  state = reducer(state, null);

  return {dispatch, subscribe, getState}
}

const othelloReducer = (state, action) => {
	  console.log("Reducer: " + state + " action: " + (action == null ? "null" : JSON.stringify(action)));
//	  function createArray(length) {
//	    var arr = new Array(length || 0),
//	        i = length;
//
//	    if (arguments.length > 1) {
//	        var args = Array.prototype.slice.call(arguments, 1);
//	        while(i--) arr[length-1 - i] = createArray.apply(this, args);
//	    }
//
//	    return arr;
//	  }
	  function createArray() {
		  console.log("Create Array, size: " + size);
		  var i, j, rows = [], cols;
		  for (i = 0; i < size; i++) {
			  cols = [];
			  for (j = 0; j < size; j++) {
				  cols.push({
					  row: i,
					  col: j,
					  state: "idle"
				  });
			  }
			  rows.push(cols);
		  }
		  console.log("Created array: " + rows.length);
		  return rows;
	  }
	  
	  function copyArray(gamePositions) {
		  var i, j, row, rows = [], cols;
		  for (i = 0; i < gamePositions.length; i++) {
			  cols = [];
			  row = gamePositions[i];
			  console.log("row " + row);
			  for (j = 0; j < row.length; j++) {
				  cols.push(gamePositions[i][j]);
			  }
			  rows.push(cols);
		  }
		  console.log("Copied array: " + rows.length);
		  return rows;
	  }
	  
  function setGamePositions(curGamePos, swaps) {
	  console.log("setGamePositions: " + JSON.stringify(swaps));
	  var newGamePos = copyArray(curGamePos);
	  for (var i = 0; i < swaps.length; i++) {
		  var o = swaps[i];
		  newGamePos[o.row][o.col]= {
				  row: newGamePos[o.row][o.col].row,
				  col: newGamePos[o.row][o.col].col,
				  state: o.state		  
		  }
	  }
	  return newGamePos;
  }
  
  if (typeof state === 'undefined') {
    return {
    	showRules: false,
    	info: "",
    	yourMove: false,
    	gameOver: false,
    	gamePositions: createArray()
    };
  }
  if (action.type === 'TOGGLE_RULES') {
    return {
		showRules: !state.showRules,
    	info: state.info,
    	yourMove: state.yourMove,
    	gameOver: state.gameOver,
    	gamePositions: state.gamePositions
    };
  }
  else if (action.type === 'GAME_OVER') {
    return {
		showRules: state.showRules,
    	info: "game over",
    	yourMove: state.yourMove,
    	gameOver: true,
    	gamePositions: state.gamePositions
    };		  
  }
  else if (action.type === 'INIT_BOARD') {
	console.log("INIT_BOARD size: " + size);
	var high = size / 2;
	var low = high - 1;
    var newGamePositions = setGamePositions(state.gamePositions,
    		[{state: "red", row: low, col: low},
    		{state: "red", row: high, col: high},
    		{state: "blue", row: low, col: high},
    		{state: "blue", row: high, col: low}]
    );
    return {
		showRules: state.showRules,
    	info: state.info,
    	yourMove: state.yourMove,
    	gameOver: state.gameOver,
    	gamePositions: newGamePositions
    };		  
  }
  else if (action.type === "CHECKING_MOVE") {
    return {
		showRules: state.showRules,
    	info: "Checking move...",
    	yourMove: false,
    	gameOver: state.gameOver,
    	gamePositions: state.gamePositions
    };		  	  
  }
  else if (action.type === "MESSAGE") {
	if (action.payload.message === "yourMove") {
	  var newGamePositions = state.gamePositions;
	  var color = action.payload.prevColor;
	  var prevMove = action.payload.prevMove;
	  if (prevMove) {
		var swaps = [];
		for (var i = 0; i < prevMove.length; i++) {
			swaps.push({state: color, row: prevMove[i].row, col: prevMove[i].column})
		}
		newGamePositions = setGamePositions(state.gamePositions, swaps)
	  }
	  return {
		showRules: state.showRules,
		info: "Your move. Select a box that will flip at least one of your opponents markers or click 'pass'",
	    yourMove: true,
	    gameOver: state.gameOver,
	    gamePositions: newGamePositions
		  
	  }
	}
	if (action.payload.message === "invalidMove") {
	  return {
		showRules: state.showRules,
		info: "Invalid move. You must select a box that will flip at least one of your opponents markers or click 'pass'",
	    yourMove: true,
	    gameOver: state.gameOver,
	    gamePositions: state.gamePositions
		  
	  }
	}
	if (action.payload.message === "oppMove") {
	  var newGamePositions = state.gamePositions;
	  var color = action.payload.prevColor;
	  var prevMove = action.payload.prevMove;
	  if (prevMove) {
		var swaps = [];
		for (var i = 0; i < prevMove.length; i++) {
			swaps.push({state: color, row: prevMove[i].row, col: prevMove[i].column})
		}
		newGamePositions = setGamePositions(state.gamePositions, swaps)
	  }
	  return {
		showRules: state.showRules,
		info: oppName + " turn. Please wait...",
	    yourMove: false,
	    gameOver: state.gameOver,
	    gamePositions: newGamePositions
		  
	  }
	}
	if (action.payload.message === "gameOver") {
	  var newGamePositions = state.gamePositions;
	  var color = action.payload.prevColor;
	  var prevMove = action.payload.prevMove;
	  if (prevMove) {
		var swaps = [];
		for (var i = 0; i < prevMove.length; i++) {
			swaps.push({state: color, row: prevMove[i].row, col: prevMove[i].column})
		}
		newGamePositions = setGamePositions(state.gamePositions, swaps)
	  }
	  var result = "It is a draw";
	  if (action.payload.winner) {
		  if (action.payload.winner == userName) {
			  result = "You have won!"
		  }	
		  else {
			  result = "" + action.payload.winner + " has won!"
		  }
	  }
	  return {
		showRules: state.showRules,
		info: "Game over. " + result,
	    yourMove: false,
	    gameOver: true,
	    gamePositions: newGamePositions
		  
	  }
	}
	
  }

  return state;
}

var HeaderSection = React.createClass({
	render: function() {
		return (
			<header className="text-center">
				<div>
			 		<h1>JOMAMOMA - Othello</h1>
					<div>
						<span>Welcome </span> 
						<span id="userSpan">{userName}</span>
						<span id="oppSpan" className="hide">{oppName}</span>
						<span> - You have <i id="color" className={color}>{color}</i> color</span>
					</div>
					<hr/>
				</div>
			</header>
		)	
	}
});

var RulesSection = React.createClass({
	_toggleRules: function() {
		console.log("Dispatch: TOGGLE_RULES");
		OthelloStore.dispatch({
			type: "TOGGLE_RULES"
		});
	},
	
	render: function() {
		if (OthelloStore.getState().showRules) {
			return (
				<div className="info">
					<p>The goal of the Othello game to put your marker so 
					that you have at least one (the more the better) of 
					the opponents markers between one of your markers and the marker you 
					are playing. The opponents marker(s) caught between will be turned
					to your color.</p>
					<p>At the end of the game the player with most markers will win.</p>
					<p>If you can not find a place for your marker which will at least turn one
					of the opponents you can click the 'Pass' button.</p>
			
					<a href="javascript:" onClick={this._toggleRules}>hide info</a>
					<hr />
				</div>
			)
		}
		else {
			return (
				<div className="text-center">
					Need to know the rules? 
					<span>
						Click <a href="javascript:" onClick={this._toggleRules}>here</a>.
					</span>
				</div>
			)
		}
	}
});

var GameInfoSection = React.createClass({
	render: function() {
		var state = OthelloStore.getState();
		var hideGameOverInfo = !state.gameOver ? " hide" : "";
		return (
			<div>
	 			<div id="infoDiv" className="text-center">
	 				<p>&nbsp;
	 				    <span>
	 				        {state.info}
	 				    </span>
	 				</p>
	 			</div>
			
		        <div id="returnDiv" className={"text-center" + hideGameOverInfo}>
		      		<p>Return to <a href="/home">home page</a>.</p>
		      	</div>
	      	</div>		
		)
	}
});

var positionClick = (row, col) => {
	console.log("positionClick:" + row + ", " + col);
	OthelloStore.dispatch({"type": "CHECKING_MOVE"});
	OthelloService.positionClicked({"message": "positionSelected", "row": row, "col": col});
};

var GameCol = React.createClass({
	render: function() { 
//		console.log("col props:" + JSON.stringify(this.props.data));
		var row = this.props.data.row;
		var col = this.props.data.col;
		var yourMove = OthelloStore.getState().yourMove;
		if (yourMove) {
//			console.log("Render listener: " + row + "," + col);
			return (
				<td className='gamePos'>
					<div className={this.props.data.state}
					     onClick={() => positionClick(row,col)}>
					</div>
				</td>
			)			
		}
		else {
//			console.log("Render no listener: " + row + "," + col);
			return (
				<td className='gamePos'>
					<div className={this.props.data.state}></div>
				</td>
			)			
		}
	}
});

var GameRow = React.createClass({
	render: function() {
//		console.log("row props:" + JSON.stringify(this.props.data));
		var cols = [];
		for (var i = 0; i < this.props.data.length; i++) {
			cols.push(<GameCol key={i} data={this.props.data[i]}/>)
		}
		return (
			<tr>
				{cols}
			</tr>
		)
	}
});

var GameSection = React.createClass({
	render: function() {
		var gamePositions = OthelloStore.getState().gamePositions;
		var rows = [];
		for (var i = 0; i < gamePositions.length; i++) {
			rows.push(<GameRow key={i} data={gamePositions[i]}/>)
		}
		var yourMoveClass = OthelloStore.getState().yourMove ? " your-turn" : "";
		console.log("yourMoveClass: " + yourMoveClass);
		return (
            <div id="main" className="center-block text-center">
            	<button type="button" disabled={!OthelloStore.getState().yourMove}
            		    onClick={() => {
            		    	//Send a PASS message (row and col is -1 )
            		    	OthelloStore.dispatch({"type": "CHECKING_MOVE"});
            		    	OthelloService.positionClicked({"message": "positionSelected", "row": -1, "col": -1});
            		    }}>
            		pass
            	</button>
            	<br />
                <table id="gameTable" className={"game-table text-center" + yourMoveClass}>
 			      	<tbody>
 			      		{rows}
			      	</tbody>    	
                </table>
             </div>	
		)
	}
})


var OthelloGame = React.createClass({
	render: function() {
		return (
			<div className="container">
				<HeaderSection/>
				<RulesSection/>
				<GameInfoSection/>
				<GameSection/>
			</div>
		)
	}
	
});

var userName = document.getElementById("userSpan").innerHTML;
var oppName = document.getElementById("oppSpan").innerHTML;
var color = document.getElementById("colorSpan").innerHTML;
var size = document.getElementById("sizeSpan").innerHTML;

const OthelloStore = Store(othelloReducer);

//set up initial positions
OthelloStore.dispatch({
	type: "INIT_BOARD"
});

console.log("userName:" + userName);

OthelloStore.subscribe(() => {
	console.log("Render due to subscribe");
	ReactDOM.render(
		<OthelloGame/>,
		document.getElementById("app")
	)	
});

ReactDOM.render(
	<OthelloGame/>,
	document.getElementById("app")
);

//message loop
(function() {
	console.log("Starting message loop");
	var repeat = function() {
		if (OthelloStore.getState().isGameOver) {
			consolog.log("message loop exiting, game over.")
			return;
		}
		setTimeout(function() {
			OthelloService.getMessages().
				done(function(data){
//					console.log("Message received: " + data);
		        	if (data.message !== "empty") {
		        		console.log("Message received: " + JSON.stringify(data));
						OthelloStore.dispatch({
							type: "MESSAGE",
							payload: data
						});
		        	}
				}).
				fail(function() {
					console.log("Failed to get message");
				});

			repeat();
		}, 2000);
	};
	repeat();
})();