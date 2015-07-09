name := "jomamoma_pull"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

//lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// The Typesafe repository 
//resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

//resolvers += "webjars" at "http://www.webjars.org/" 

//libraryDependencies ++= Seq(javaJdbc, javaEbean)

//fork in run := true

routesGenerator := InjectedRoutesGenerator