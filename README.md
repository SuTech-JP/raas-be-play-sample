# raas-be-play-sample

Play Framework 3.x (Scala) を使用した RaaS バックエンドサンプル。  
[raas-client-java](../raas-client-java) SDK（Pure Java / Spring 非依存）を使用する。

## 動作環境

- Java 11+
- Scala 3.3.x
- sbt 1.9.x

## セットアップ

### 1. raas-client-java の JAR を配置する

SuTech より提供された `raas-client-java-<version>-all.jar` をプロジェクトの `lib/` ディレクトリに配置する。

```
raas-be-play-sample/
└── lib/
    └── raas-client-java-1.0.1-all.jar
```

sbt は `lib/` ディレクトリ内の JAR を自動的に依存関係として認識するため、追加の設定は不要。

### 2. 接続設定

環境変数を設定するか、`conf/application.conf` を直接編集する。

**環境変数（推奨）:**

```bash
export RAAS_APP=your-application-name
export RAAS_LANDSCAPE=dev          # dev または prod
export RAAS_TOKEN=your-system-token
export FRONTEND_ORIGIN=http://localhost:5173  # CORS 許可オリジン（任意）
```

**application.conf を直接編集する場合:**

```hocon
raas {
  application = "your-application-name"
  landscape   = "dev"
  token       = "your-system-token"
}
```

### 3. アプリケーションの起動

```bash
sbt run
```

デフォルトポート: **8080**  
（環境変数 `PORT` で変更可能）

## API エンドポイント

| Method | Path | 説明 |
|--------|------|------|
| `POST` | `/raas/:msa/session` | セッション発行（msa: `report` or `datatraveler`）|
| `GET`  | `/raas/report/layout/:application/:schema` | レポートレイアウト一覧取得 |
| `GET`  | `/raas/report/result/:targetId` | CSVインポートログ取得 |
| `POST` | `/raas/tenant/delete` | テナント削除 |

### リクエスト例

**セッション発行:**

```bash
curl -X POST http://localhost:8080/raas/report/session \
  -H "Content-Type: application/json" \
  -d '{"backUrl": "https://your-app.example.com", "subUrl": "", "subDomain": ""}'
```

**レイアウト一覧取得:**

```bash
curl http://localhost:8080/raas/report/layout/myapp/myschema
```

## プロジェクト構成

```
raas-be-play-sample/
├── build.sbt                        # ビルド設定（依存関係）
├── project/
│   ├── build.properties             # sbt バージョン
│   └── plugins.sbt                  # Play sbt プラグイン
├── conf/
│   ├── application.conf             # アプリ設定（RaaS 接続情報、CORS）
│   └── routes                       # URL ルーティング定義
└── app/
    └── controllers/
        └── RaasController.scala     # RaaS プロキシコントローラー
```

## SDK の使い方

```scala
import jp.co.sutech.raas.RaasClient
import jp.co.sutech.raas.config.RaasConnectionConfig
import jp.co.sutech.raas.context.RaasUserContext

// 設定
val config = RaasConnectionConfig.of("myapp", "dev", "systemToken")

// ユーザーコンテキスト（実際はリクエストの認証情報から取得する）
val userCtx = RaasUserContext.builder("tenantId", "userId")
  .tenantAlias("alias")
  .build()

// クライアント生成
val client = RaasClient.create(config, userCtx)

try {
  // セッション発行
  val session = client.createExternalSession("report", backUrl, subUrl, subDomain)

  // 汎用 GET
  val result: JsonNode = client.get("/report/layouts/app/schema", classOf[JsonNode])

  // 汎用 POST
  val created: JsonNode = client.post("/some/endpoint", body, classOf[JsonNode])
} finally {
  // リクエスト終了時にトークンキャッシュをクリア
  client.clearTokenCache()
}
```
