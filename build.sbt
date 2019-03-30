enablePlugins(ScalaJSPlugin)

name := "chem_prog_exp"

scalaVersion := "2.12.6" // or any other Scala version >= 2.10.2

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.5"

libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.6.7"

scalaJSUseMainModuleInitializer := false

