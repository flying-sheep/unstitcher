import AssemblyKeys._

assemblySettings

name := "unstitcher"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies <+= scalaVersion( "org.scala-lang" % "scala-swing" % _ )

scalaSource in Compile <<= baseDirectory { _ / "src" }

resourceDirectory in Compile <<= baseDirectory { _ / "res" }

mainClass in (Compile, run) := Some("net.minecraft.util.ui.UnstitcherCli")


target in assembly := new File(".")

jarName in assembly := "unstitcher.jar"

mainClass in assembly := Some("net.minecraft.util.ui.UnstitcherCli")
