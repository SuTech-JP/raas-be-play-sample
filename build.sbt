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

// Jackson バージョン統一
// Play 3.0.x / Pekko が jackson-module-scala 2.14.3（Scala 2.13 版）を引き込む。
// このモジュールは jackson-databind < 2.15.0 を要求するが、
// raas-client-java は 2.15.2 を使用するため起動時に競合が発生する。
//
// 解決策: jackson-module-scala を 2.15.2 に強制する。
// ※ Scala 3 プロジェクトで %% を使うと _3 アーティファクトにしか効かないため、
//    Play/Pekko が引き込む _2.13 版は % で明示的に指定する。
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core"   %  "jackson-databind"          % "2.15.2",
  "com.fasterxml.jackson.core"   %  "jackson-core"              % "2.15.2",
  "com.fasterxml.jackson.core"   %  "jackson-annotations"       % "2.15.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.15.2",  // _3 向け
  "com.fasterxml.jackson.module" %  "jackson-module-scala_2.13" % "2.15.2"   // Pekko/_2.13 向け
)

// Java 11 互換
javacOptions ++= Seq("-source", "11", "-target", "11")

// Play デフォルトポートを 8080 に変更（他サンプルと統一）
PlayKeys.devSettings += "play.server.http.port" -> "8080"
