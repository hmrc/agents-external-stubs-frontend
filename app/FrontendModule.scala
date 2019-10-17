import java.net.URL

import akka.actor.ActorSystem
import javax.inject.{Inject, Provider, Singleton}
import com.google.inject.AbstractModule
import com.google.inject.name.{Named, Names}
import com.typesafe.config.Config
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentsexternalstubsfrontend.TcpProxies
import uk.gov.hmrc.agentsexternalstubsfrontend.connectors.FrontendAuthConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

class FrontendModule(val environment: Environment, val configuration: Configuration)
    extends AbstractModule with ServicesConfig {

  override val runModeConfiguration: Configuration = configuration
  override protected def mode = environment.mode

  def configure(): Unit = {

    val appName = "agents-external-stubs-frontend"

    val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")
    Logger(getClass).info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))

    bindProperty("appName")

    bind(classOf[HttpGet]).to(classOf[HttpVerbs])
    bind(classOf[HttpPost]).to(classOf[HttpVerbs])
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])

    //example of service property bindings
    bindServiceConfigProperty[Int]("agents-external-stubs.port")
    bindServiceConfigProperty[Int]("company-auth-frontend.port")
    bindServiceConfigProperty[Int]("stride-auth-frontend.port")
    bindServiceConfigProperty[Int]("identity-verification-frontend.port")
    bindServiceConfigProperty[Int]("government-gateway-registration-frontend.port")
    bindServiceConfigProperty[Int]("personal-details-validation-frontend.port")

    bindProperty("http.port")
    bindProperty("proxies.start")

    bindBaseUrl("auth")
    bindBaseUrl("agents-external-stubs")

    bind(classOf[TcpProxies]).asEagerSingleton()
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(propertyName)).toProvider(new PropertyProvider(propertyName))

  private class PropertyProvider(confKey: String) extends Provider[String] {
    override lazy val get = configuration
      .getString(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  import scala.reflect.ClassTag
  import com.google.inject.binder.ScopedBindingBuilder
  import com.google.inject.name.Names.named

  private def bindServiceConfigProperty[A](
    propertyName: String)(implicit classTag: ClassTag[A], ct: ServiceConfigPropertyType[A]): ScopedBindingBuilder =
    ct.bindServiceConfigProperty(classTag.runtimeClass.asInstanceOf[Class[A]])(propertyName)

  sealed trait ServiceConfigPropertyType[A] {
    def bindServiceConfigProperty(clazz: Class[A])(propertyName: String): ScopedBindingBuilder
  }

  object ServiceConfigPropertyType {

    implicit val stringServiceConfigProperty: ServiceConfigPropertyType[String] =
      new ServiceConfigPropertyType[String] {
        def bindServiceConfigProperty(clazz: Class[String])(propertyName: String): ScopedBindingBuilder =
          bind(clazz)
            .annotatedWith(named(s"$propertyName"))
            .toProvider(new StringServiceConfigPropertyProvider(propertyName))

        private class StringServiceConfigPropertyProvider(propertyName: String) extends Provider[String] {
          override lazy val get = getConfString(
            propertyName,
            throw new RuntimeException(s"No service configuration value found for '$propertyName'"))
        }
      }

    implicit val intServiceConfigProperty: ServiceConfigPropertyType[Int] = new ServiceConfigPropertyType[Int] {
      def bindServiceConfigProperty(clazz: Class[Int])(propertyName: String): ScopedBindingBuilder =
        bind(clazz)
          .annotatedWith(named(s"$propertyName"))
          .toProvider(new IntServiceConfigPropertyProvider(propertyName))

      private class IntServiceConfigPropertyProvider(propertyName: String) extends Provider[Int] {
        override lazy val get = getConfInt(
          propertyName,
          throw new RuntimeException(s"No service configuration value found for '$propertyName'"))
      }
    }

    implicit val booleanServiceConfigProperty: ServiceConfigPropertyType[Boolean] =
      new ServiceConfigPropertyType[Boolean] {
        def bindServiceConfigProperty(clazz: Class[Boolean])(propertyName: String): ScopedBindingBuilder =
          bind(clazz)
            .annotatedWith(named(s"$propertyName"))
            .toProvider(new BooleanServiceConfigPropertyProvider(propertyName))

        private class BooleanServiceConfigPropertyProvider(propertyName: String) extends Provider[Boolean] {
          override lazy val get = getConfBool(propertyName, false)
        }
      }
  }
}

@Singleton
class HttpVerbs @Inject()(
  val auditConnector: AuditConnector,
  @Named("appName") val appName: String,
  config: Configuration,
  val actorSystem: ActorSystem)
    extends HttpGet with HttpPost with HttpPut with HttpPatch with HttpDelete with WSHttp with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override def configuration: Option[Config] = Some(config.underlying)
}
