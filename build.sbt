name := "big-cash"

version := "1.0"

scalaVersion := "2.12.1"

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.typesafeIvyRepo("releases")

libraryDependencies ++= Seq(
  "org.mongodb" %% "casbah" % "3.1.1",
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "io.spray" %%  "spray-json" % "1.3.3",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5"
)

    