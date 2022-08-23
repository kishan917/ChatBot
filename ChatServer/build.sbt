import sbtassembly.AssemblyPlugin.autoImport.{
  assembly,
  assemblyMergeStrategy,
  assemblyShadeRules,
  MergeStrategy,
  PathList,
  ShadeRule
}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ChatServer",
    idePackagePrefix := Some("org.kyadav.scala.chatbot.chatserver"),
    Compile / run / mainClass := Some("org.kyadav.scala.chatbot.chatserver.StartServer"),
    assembly / assemblyMergeStrategy := {
//        case path if path.endsWith("/module-info.class") => MergeStrategy.concat
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
//      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.concat
      case x => (assembly / assemblyMergeStrategy).value(x)
    }
  )
Compile / mainClass := Some("org.kyadav.scala.chatbot.chatserver.StartServer")
packageBin / mainClass := Some("org.kyadav.scala.chatbot.chatserver.StartServer")
//mainClass in assembly := Some("StartServer")

// json processing
libraryDependencies += "com.google.code.gson" % "gson" % "2.9.0"

// slf4j
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.36"

// log4j
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.17.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.17.2"

// slf4j - log4j
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2"

// akka
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19"
