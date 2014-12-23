name := "Twitter_Server"

version := "1.0"

scalaVersion := "2.11.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

unmanagedClasspath in Runtime += baseDirectory.value / "lib"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "com.typesafe.akka" %% "akka-remote" % "2.3.5",
  "io.spray"            %%  "spray-json" % "1.3.1",
  "org.json4s" %%  "json4s-native" % "3.2.10",
  "com.owlike" % "genson-scala" % "1.1"
)