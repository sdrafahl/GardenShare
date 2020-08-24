val Http4sVersion = "0.21.3"
val CirceVersion = "0.13.0"
val Specs2Version = "4.9.3"
val LogbackVersion = "1.2.3"


lazy val root = (project in file("."))
  .settings(
    organization := "com.gardenShare",
    name := "gardenshare",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.typesafe" % "config" % "1.4.0",
      "com.lihaoyi" %% "utest" % "0.7.2" % "test",
      "org.mockito" %% "mockito-scala" % "1.14.2",
      "com.typesafe.slick" %% "slick" % "3.3.2",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
      "org.postgresql" % "postgresql" % "42.2.12",
      "software.amazon.awssdk" % "aws-sdk-java" % "2.13.30",
      "jakarta.xml.bind" % "jakarta.xml.bind-api" % "3.0.0-RC3",
      "com.walterjwhite.java.dependencies" % "bouncy-castle" % "0.0.17",
      "org.bouncycastle" % "bcprov-jdk15on" % "1.65.01",
      "com.github.3tty0n" %% "jwt-scala" % "1.3.0"
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
)
