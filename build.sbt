name := """webserver"""
organization := "edlav"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test
libraryDependencies += "com.pauldijou" %% "jwt-core" % "3.0.1"
libraryDependencies += "io.circe" %% "circe-core" % "0.12.0-M3"
libraryDependencies += "io.circe" %% "circe-parser" % "0.12.0-M3"
libraryDependencies += "io.circe" %% "circe-generic" % "0.12.0-M3"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.0"
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "4.1"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"
libraryDependencies += "org.apache.kafka" % "kafka-streams" % "2.3.0"
libraryDependencies += "org.apache.kafka" %% "kafka-streams-scala" % "2.3.0"
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.3.0"
libraryDependencies += "org.apache.zookeeper" % "zookeeper" % "3.5.5"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.5"
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.0.0"
libraryDependencies += "io.github.valdemargr" %% "sdis" % "1.1.3"
libraryDependencies += "com.google.cloud" % "google-cloud-storage" % "1.102.0"


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "edlav.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "edlav.binders._"
