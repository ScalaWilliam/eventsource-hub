name := "eventsource-hub"
scalaVersion := "2.12.1"
libraryDependencies += "com.typesafe.akka" %% "akka-agent" % "2.4.17"
libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"
libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
enablePlugins(PlayScala)
enablePlugins(DockerPlugin)
enablePlugins(GitVersioning)
git.useGitDescribe := true
dockerRepository := Some("scalawilliam")
dockerBaseImage := "java:openjdk-8-jre"
dockerExposedPorts := Seq(9000)
dockerExposedVolumes := Seq("/opt/docker/events")
