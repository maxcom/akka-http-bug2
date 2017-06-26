name := "akka-http-bug"
version := "0.1"

scalaVersion := "2.12.2"

scalacOptions := Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Xfuture",
  "-encoding", "utf8",
  "-target:jvm-1.8"
)

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-http"   % "10.0.8",
    "org.asynchttpclient" % "async-http-client" % "2.0.32",
    "org.slf4j" % "slf4j-simple" % "1.7.25"
  )
}
