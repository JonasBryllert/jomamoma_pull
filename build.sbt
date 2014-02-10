name := "jomamoma_pull"

version := "1.0-SNAPSHOT"

resolvers += "webjars" at "http://www.webjars.org/" 

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.0"
)     

play.Project.playScalaSettings
