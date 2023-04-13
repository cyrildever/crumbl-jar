organization := "fr.edgewhere"
name := "crumbl-jar"
version := "6.2.0"
scalaVersion := "2.12.13"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
mainClass in assembly := Some("io.crumbl.Main")
assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "org.bouncycastle" % "bcprov-jdk15to18" % "1.72" % "provided",
  "com.github.scopt" %% "scopt" % "4.1.0",
  "org.scalatest" %% "scalatest" % "3.2.15",
  "com.cyrildever" %% "feistel-jar" % "1.5.1"
)
