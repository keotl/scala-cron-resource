val scala3Version = "3.4.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "cron-resource",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "org.playframework" %% "play-json" % "3.0.3",

    scalacOptions += "-Wunused:all" // required by `RemoveUnused` rule
  )

inThisBuild(
  List(
    scalaVersion := "3.4.1",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)


