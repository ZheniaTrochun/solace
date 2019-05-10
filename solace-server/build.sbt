name := "solace"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "com.typesafe" % "config" % "1.3.4"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "org.onosproject" % "openflowj" % "3.2.1.onos"
libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.5.15"
libraryDependencies += "com.softwaremill.sttp" %% "akka-http-backend" % "1.5.16"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-stream
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.22"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"
libraryDependencies += "com.github.scredis" %% "scredis" % "2.2.4"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.22"


