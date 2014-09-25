name := "play-glass-launcher"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, SbtWeb)

scalaVersion := "2.11.1"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
	javaWs,
	"com.google.api-client" % "google-api-client" % "1.18.0-rc",
	"com.google.apis" % "google-api-services-oauth2" % "v2-rev70-1.18.0-rc",
	"com.google.http-client" % "google-http-client" % "1.18.0-rc",
	"com.google.http-client" % "google-http-client-jackson2" % "1.18.0-rc",
	"commons-logging" % "commons-logging" % "1.1.1",
	"com.google.code.gson" % "gson" % "2.1",
	"org.apache.httpcomponents" % "httpclient" % "4.0.1",
	"org.apache.httpcomponents" % "httpcore" % "4.0.1",
	"org.codehaus.jackson" % "jackson-core-asl" % "1.9.11",
	"com.fasterxml.jackson.core" % "jackson-core" % "2.1.3",
	"com.google.code.findbugs" % "jsr305" % "1.3.9",
	"com.google.protobuf" % "protobuf-java" % "2.4.1",
	"xpp3" % "xpp3" % "1.1.4c",
	"com.google.apis" % "google-api-services-mirror" % "v1-rev50-1.18.0-rc",
  	"org.webjars" %% "webjars-play" % "2.3.0",
  	"org.webjars" % "jquery" % "2.1.1",
 	"org.webjars" % "bootstrap" % "3.1.1-2"
)     

