package controllers

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import jp.co.sutech.raas.RaasClient
import jp.co.sutech.raas.config.RaasConnectionConfig
import jp.co.sutech.raas.context.RaasUserContext
import jp.co.sutech.raas.exception.{RaasClientException, RaasException}
import play.api.Configuration
import play.api.libs.json.*
import play.api.mvc.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

/**
 * RaaS プロキシコントローラー。
 *
 * raas-client-java（Pure Java SDK）を使用して RaaS API と通信し、
 * フロントエンドへ結果を返す。
 *
 * 【ExecutionContext について】
 * AbstractController が controllerComponents.executionContext を implicit で提供するため
 * コンストラクタでの EC インジェクションは不要。
 * ただし raas-client-java の HTTP 呼び出しはブロッキングなので、
 * 本番環境では専用のブロッキング用スレッドプールに切り替えること。
 *
 * 【認証について】
 * このサンプルでは tenant / sub をハードコードしているが、
 * 実際の実装ではリクエストの認証情報（JWT など）から取得すること。
 */
@Singleton
class RaasController @Inject()(
    cc: ControllerComponents,
    configuration: Configuration
) extends AbstractController(cc) {

  // RaaS 接続設定（application.conf または環境変数から読み込む）
  private val raasConfig: RaasConnectionConfig = RaasConnectionConfig.of(
    configuration.get[String]("raas.application"),
    configuration.get[String]("raas.landscape"),
    configuration.get[String]("raas.token")
  )

  // AbstractController.executionContext（protected lazy val）を implicit として公開する。
  // Future { } ブロックおよび future.recover() に必要。
  private implicit val ec: ExecutionContext = controllerComponents.executionContext

  // Jackson ObjectMapper（JsonNode → JSON 文字列の変換に使用）
  private val mapper = new ObjectMapper()

  /**
   * サンプル用ユーザーコンテキストを返す。
   * 実際の実装ではリクエストの認証情報から tenant / sub を取得すること。
   */
  private def newUserContext: RaasUserContext =
    RaasUserContext.builder("sample_tenant", "sample_user").build()

  // ---------------------------------------------------------------------------
  // POST /raas/:msa/session
  // セッション発行 API（msa: "report" または "datatraveler"）
  // ---------------------------------------------------------------------------

  def createSession(msa: String): Action[JsValue] = Action.async(parse.json) { request =>
    Future {
      val backUrl   = (request.body \ "backUrl").as[String]
      val subUrl    = (request.body \ "subUrl").asOpt[String].getOrElse("")
      val subDomain = (request.body \ "subDomain").asOpt[String].getOrElse("")

      val client = RaasClient.create(raasConfig, newUserContext)
      try {
        val session = client.createExternalSession(msa, backUrl, subUrl, subDomain)
        Ok(Json.obj(
          "application" -> session.getApplication,
          "url"         -> session.getUrl,
          "newUrl"      -> session.getNewUrl
        ))
      } finally {
        client.clearTokenCache()
      }
    }.recover(errorHandler)
  }

  // ---------------------------------------------------------------------------
  // GET /raas/report/layout/:application/:schema
  // レポートレイアウト一覧の取得
  // ---------------------------------------------------------------------------

  def loadLayouts(application: String, schema: String): Action[AnyContent] = Action.async { _ =>
    Future {
      val client = RaasClient.create(raasConfig, newUserContext)
      try {
        val result = client.get(
          s"/report/layouts/$application/$schema",
          classOf[JsonNode]
        )
        Ok(Json.parse(mapper.writeValueAsString(result)))
      } finally {
        client.clearTokenCache()
      }
    }.recover(errorHandler)
  }

  // ---------------------------------------------------------------------------
  // GET /raas/report/result/:targetId
  // CSVインポートログデータの取得
  // ステータスが FINISH の場合は詳細データも取得してマージする
  // ---------------------------------------------------------------------------

  def loadLogData(targetId: String): Action[AnyContent] = Action.async { _ =>
    Future {
      val client = RaasClient.create(raasConfig, newUserContext)
      try {
        val log = client.get(
          s"/datatraveler/import/logs/$targetId",
          classOf[JsonNode]
        )

        if (log.path("status").asText() == "FINISH") {
          val details = client.get(
            s"/datatraveler/import/logs/$targetId/data",
            classOf[JsonNode]
          )
          // log の全フィールドに details を追加してマージ
          val merged = mapper.createObjectNode()
          log.fields().asScala.foreach(e => merged.set(e.getKey, e.getValue))
          merged.set("details", details)
          Ok(Json.parse(mapper.writeValueAsString(merged)))
        } else {
          Ok(Json.parse(mapper.writeValueAsString(log)))
        }
      } finally {
        client.clearTokenCache()
      }
    }.recover(errorHandler)
  }

  // ---------------------------------------------------------------------------
  // POST /raas/tenant/delete
  // テナント削除
  // ---------------------------------------------------------------------------

  def deleteTenant(): Action[JsValue] = Action.async(parse.json) { request =>
    Future {
      val tenant = (request.body \ "tenant").as[String]

      val client = RaasClient.create(raasConfig, newUserContext)
      try {
        client.deleteTenant(tenant)
        Ok(Json.obj("message" -> "Tenant deleted successfully"))
      } finally {
        client.clearTokenCache()
      }
    }.recover(errorHandler)
  }

  // ---------------------------------------------------------------------------
  // エラーハンドリング
  // ---------------------------------------------------------------------------

  private val errorHandler: PartialFunction[Throwable, Result] = {
    case e: JsResultException =>
      BadRequest(Json.obj("error" -> "Invalid request body", "detail" -> e.getMessage))
    case e: RaasClientException =>
      // 4xx エラー: RaaS からのクライアントエラーをそのまま伝播
      Status(e.getStatusCode)(Json.obj("error" -> e.getMessage))
    case e: RaasException =>
      // リトライ全失敗など RaaS 通信エラー
      InternalServerError(Json.obj("error" -> e.getMessage))
    case e: Exception =>
      InternalServerError(Json.obj("error" -> e.getMessage))
  }
}
