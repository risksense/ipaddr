lazy val root = (project in file("."))
  .settings(
    name := "ipaddr",
    organization := "com.risksense",
    version := "1.0.2",
    scalaVersion := "2.13.6",
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
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
      "org.scalatest" %% "scalatest" % "3.0.9" % "test"
    )
  )

// Adds a `src/main/scala-2.13+` source directory for Scala 2.13 and newer
// and a `src/main/scala-2.13-` source directory for Scala version older than 2.13
Compile / unmanagedSourceDirectories += {
  val sourceDir = (Compile / sourceDirectory).value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13+"
    case _                       => sourceDir / "scala-2.13-"
  }
}
