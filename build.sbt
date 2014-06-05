name := "jomamoma_pull"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

//lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "webjars" at "http://www.webjars.org/" 

//libraryDependencies ++= Seq(javaJdbc, javaEbean)