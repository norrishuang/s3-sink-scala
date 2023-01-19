import sbt.Keys.javacOptions

lazy val root = (project in file(".")).
  settings(
    name := "s3-sink-scala",
    version := "1.1",
    scalaVersion := "3.2.0",
    mainClass := Some("com.amazonaws.services.kinesisanalytics.main"),
    javacOptions ++= Seq("-source", "11", "-target", "11")
  )

val jarName = "s3-sink-scala-1.1.jar"
val flinkVersion = "1.15.2"
val kdaRuntimeVersion = "1.2.0"
val scalaBinVersion = "2.12"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-kinesisanalytics-runtime" % kdaRuntimeVersion,
  "org.apache.flink" % "flink-connector-kinesis" % flinkVersion,
  "org.apache.flink" % "flink-streaming-java" % flinkVersion,
  "org.apache.flink" % "flink-clients" % flinkVersion
)

artifactName := { (_: ScalaVersion, _: ModuleID, _: Artifact) => jarName }

assembly / assemblyJarName := jarName
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
