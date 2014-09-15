function OwnBoard(size) {
  this.shipLengths = [2,2,3,3,5];
  this.ships = this.generateShips(size);
  
}

OwnBoard.prototype = function() {
  var _generateShips = function(_gameSize) {
    var gameSize = _gameSize || 8;
    var ships = [];
    
    for (i = 0; i < this.shipLenghts; i++ ) {
      var ship = null;
      do {
        var direction = Math.floor(Math.random() * 2); //0 = horizontal, 1 = vertical
        var startingPointX = direction == 0 ? 
          directionMath.floor((Math.random() * (gameSize  - this.shipLenghts[i]) + 1 -) : //; //1 to 8 minus ship length
          directionMath.floor((Math.random() * gameSize + 1) : //; //1 to 8
        
        var startingPointY = direction == 1 ? 
          directionMath.floor((Math.random() * (gameSize  - this.shipLenghts[i]) + 1 -) : //; //1 to 8 minus ship length
          directionMath.floor((Math.random() * gameSize + 1) : //; //1 to 8
      
        //Check no collision
        var collision = false;
        for (j = 0; j < ships.length; j++) {
          if (ships[j].isHit(startingPointX, startingPointY) {
            collision = true;
            break;
          }
        }
        
        if (!collision) {
          var endX = direction == 0 ? startingPointX + this.shipLenghts[i] : startingPointX;
          var endY = direction == 1 ? startingPointY + this.shipLenghts[i] : startingPointY;
          
          ship = new Ship(newPoint(startingPointX, startingPointY), new Point(endX, endY));
        }
        
      }
      while (ship == null);
      
      ships.push(ship);
    }
        
  };
    
  return {
    generateShips = _generateShips
  }
}();
    
  