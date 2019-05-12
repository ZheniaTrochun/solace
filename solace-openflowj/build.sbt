name := "solace-openflowj"

version := "0.1"

scalaVersion := "2.12.8"

// https://bitbucket.org/openflowj/openflowj.git

lazy val root = (project in file(".")).dependsOn(openflowj)

lazy val openflowj = RootProject(uri("https://bitbucket.org/openflowj/openflowj.git"))


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
