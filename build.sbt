name := "eventsource-hub"
scalaVersion := "2.12.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
enablePlugins(PlayScala)
enablePlugins(DockerPlugin)
enablePlugins(GitVersioning)
git.useGitDescribe := true
dockerRepository := Some("scalawilliam")
dockerBaseImage := "java:openjdk-8-jre"
dockerExposedPorts := Seq(9000)
dockerExposedVolumes := Seq("/opt/docker/events")
