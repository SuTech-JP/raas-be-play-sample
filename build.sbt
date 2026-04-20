name         := "raas-be-play-sample"
organization := "jp.co.sutech"
version      := "1.0-SNAPSHOT"
scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

// raas-client-java の JAR を lib/ ディレクトリから解決する
// SuTech より提供された raas-client-java-<version>-all.jar を lib/ に配置すること

libraryDependencies ++= Seq(
  guice,
  filters
)

// Java 11 互換
javacOptions ++= Seq("-source", "11", "-target", "11")

// Play デフォルトポートを 8080 に変更（他サンプルと統一）
PlayKeys.devSettings += "play.server.http.port" -> "8080"
