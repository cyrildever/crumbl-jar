organization := "fr.edgewhere"
name := "crumbl-jar"
version := "6.0.0"
scalaVersion := "2.12.13"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
mainClass in assembly := Some("io.crumbl.Main")
assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "org.bouncycastle" % "bcprov-jdk15to18" % "1.64" % "provided",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "org.scalatest" %% "scalatest" % "3.0.8",
  "fr.edgewhere" %% "feistel" % "1.3.4" % "provided"
)
