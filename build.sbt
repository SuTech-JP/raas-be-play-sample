name         := "raas-be-play-sample"
organization := "jp.co.sutech"
version      := "1.0-SNAPSHOT"
scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

// raas-client-java を GitHub Packages から解決する
// 事前に ~/.sbt/1.0/credentials.properties に GitHub トークンを設定すること
credentials += Credentials(Path.userHome / ".sbt" / "1.0" / "credentials.properties")

resolvers += "GitHub Packages" at "https://maven.pkg.github.com/SuTech-JP/raas-client-java"

libraryDependencies ++= Seq(
  guice,
  filters,
  // Pure Java SDK (Spring 非依存)
  "jp.co.sutech" % "raas-client-java" % "1.0.0"
)

// Java 11 互換
javacOptions ++= Seq("-source", "11", "-target", "11")

// Play デフォルトポートを 8080 に変更（他サンプルと統一）
PlayKeys.devSettings += "play.server.http.port" -> "8080"
