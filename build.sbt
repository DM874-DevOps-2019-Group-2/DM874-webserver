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

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "edlav.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "edlav.binders._"
