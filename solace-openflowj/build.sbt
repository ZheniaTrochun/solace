name := "solace-openflowj"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions += "-Ypartial-unification"

libraryDependencies += "com.typesafe" % "config" % "1.3.4"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"
// redis
libraryDependencies += "com.github.scredis" %% "scredis" % "2.2.4"
// OpenFlowJ
libraryDependencies += "org.onosproject" % "openflowj" % "3.2.1.onos"
// akka
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.22"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.22"
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.8"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8"
// cats
libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0-M1"
