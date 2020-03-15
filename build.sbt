lazy val root = (project in file("."))
  .settings(
    name := "ipaddr",
    organization := "com.risksense",
    version := "1.1.0",
    scalaVersion := "2.13.1",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-explaintypes",
      "-Xcheckinit",
      "-Xlint",
      "-Xverify",
      "-target:jvm-1.8"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq(
      "ch.qos.logback"               % "logback-classic"  % "1.2.3",
      "com.typesafe.scala-logging"  %% "scala-logging"    % "3.9.2",
      "org.scalatest"               %% "scalatest"        % "3.1.1"   % "test"
    )
  )
