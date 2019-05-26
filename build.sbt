val compilerPlugins = List(
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full),
  compilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.10"),
  compilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.0")
)

val zioDemo = project
  .in(file("."))
  .settings(
    scalaVersion := "2.12.8",
    libraryDependencies ++= List(
      "org.scalaz" %% "scalaz-zio" % "1.0-RC4"
    ) ++ compilerPlugins,
    scalacOptions ++= Options.all
  )
