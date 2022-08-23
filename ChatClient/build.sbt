import sbtassembly.AssemblyPlugin.autoImport.{MergeStrategy, PathList, ShadeRule, assembly, assemblyMergeStrategy, assemblyShadeRules}


ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ChatClient",
    idePackagePrefix := Some("org.kyadav.scala.chatbot.chatclient"),
    Compile / run / mainClass := Some("org.kyadav.scala.chatbot.chatclient.ChatClient"),
    assembly / assemblyMergeStrategy := {
      //        case path if path.endsWith("/module-info.class") => MergeStrategy.concat
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
      //      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.concat
      case x => (assembly / assemblyMergeStrategy).value(x)
    }
  )
Compile / mainClass := Some("org.kyadav.scala.chatbot.chatclient.ChatClient")
packageBin / mainClass := Some("org.kyadav.scala.chatbot.chatclient.ChatClient")
//mainClass in assembly := Some("ChatClient")

// json processing
libraryDependencies += "com.google.code.gson" % "gson" % "2.9.0"

// slf4j
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.36"

// log4j
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.17.2"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.17.2"

// slf4j - log4j
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2"
