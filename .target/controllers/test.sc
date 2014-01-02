package controllers

object test {
  val t: Array[String] = new Array(10)            //> t  : Array[String] = Array(null, null, null, null, null, null, null, null, nu
                                                  //| ll, null)
  t(0) = "gh"
	t(2) = "fem"
	t                                         //> res0: Array[String] = Array(gh, null, fem, null, null, null, null, null, nul
                                                  //| l, null)
	val size = 10                             //> size  : Int = 10
  val game: Array[Array[String]] = {
    val a = new Array[Array[String]](size)
    for (i <- 0 to size - 1) a(i) = new Array[String](size)
    for (col <- 1 to size; row <- 1 to size) {
      a(row-1)(col-1) = ""
    }
    a
    }                                             //> game  : Array[Array[String]] = Array(Array("", "", "", "", "", "", "", "", "
                                                  //| ", ""), Array("", "", "", "", "", "", "", "", "", ""), Array("", "", "", "",
                                                  //|  "", "", "", "", "", ""), Array("", "", "", "", "", "", "", "", "", ""), Arr
                                                  //| ay("", "", "", "", "", "", "", "", "", ""), Array("", "", "", "", "", "", ""
                                                  //| , "", "", ""), Array("", "", "", "", "", "", "", "", "", ""), Array("", "", 
                                                  //| "", "", "", "", "", "", "", ""), Array("", "", "", "", "", "", "", "", "", "
                                                  //| "), Array("", "", "", "", "", "", "", "", "", ""))
  
}