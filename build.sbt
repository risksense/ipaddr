
lazy val typesafeReleases = "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val root = (project in file("."))
  .settings(
    name := "ipaddr",
    organization := "com.risksense",
    version := "1.0.0",
    resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-explaintypes",
      "-Xcheckinit",
      "-Xlint",
      "-Xverify",
      "-target:jvm-1.8"),
    resolvers ++= Seq(typesafeReleases),
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.scalactic" %% "scalactic" % "3.0.1",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )

