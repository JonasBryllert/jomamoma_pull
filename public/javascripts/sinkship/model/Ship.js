function Ship(p1, p2) {
  this.start = p1;
  this.end = p2;
}

Ship.prototype = function() {
  var _isHit = function(x, y) {
    if (this.start.x <= x && this.end.x >= x && this.start.y <= y && this.end.y >= y) true
    else false;
    
  return {
    isHit = _isHit
  }
}();
    
  