name := "persistence-bug"

version := "0.1"

scalaVersion := "2.11.1"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "1.2.0"

libraryDependencies += "org.joda" % "joda-convert" % "1.6"

libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.11" % "2.3.4"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j"   % "2.3.4",
  "com.typesafe.akka" %% "akka-remote"  % "2.3.4",
  "com.typesafe.akka" %% "akka-agent"   % "2.3.4",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test"
)
