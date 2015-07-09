//import play.api._
//
//object Global extends GlobalSettings {
//
//  object Timer {
//	  def apply(interval: Int, repeats: Boolean = true)(op: => Unit) {
//	    val timeOut = new javax.swing.AbstractAction() {
//	      def actionPerformed(e : java.awt.event.ActionEvent) = op
//	    }
//	    val t = new javax.swing.Timer(interval, timeOut)
//	    t.setRepeats(repeats)
//	    t.start()
//	  }
//  }
//
//  override def onStart(app: Application) {
//    Logger.info("JOMAMOMA has started")
//    Timer(60*60*1000){
//      Logger.info("JOMAMOMA timeout")
//      model.Users.logoutOldUsers(compat.Platform.currentTime);
//    }
//  }
//
//  override def onStop(app: Application) {
//    Logger.info("JOMAMOMA shutdown...")
//  }
//
//}