package uk.gov.hmrc.agentsexternalstubsfrontend

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import javax.inject.{Inject, Named, Singleton}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class TcpProxies @Inject()(
  @Named("proxies.start") startProxies: String,
  @Named("company-auth-frontend.port") companyAuthFrontendPort: Int,
  @Named("stride-auth-frontend.port") strideAuthFrontendPort: Int,
  @Named("identity-verification-frontend.port") identityVerificationFrontendPort: Int,
  @Named("government-gateway-registration-frontend.port") governmentGatewayRegistrationFrontendPort: Int,
  @Named("personal-details-validation-frontend.port") personalDetailsValidationFrontendPort: Int,
  @Named("http.port") httpPort: String
)(implicit system: ActorSystem, materializer: Materializer) {

  if (startProxies == "true") {
    println("Starting TCP proxies ...")

    implicit val ec: ExecutionContext = system.dispatcher

    val agentsExternalStubsFrontendPort = Try(httpPort.toInt).toOption.getOrElse(9009)

    val tcpOutgoingConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
      Tcp().outgoingConnection("localhost", agentsExternalStubsFrontendPort)

    val tcpProxy = Flow[ByteString].via(tcpOutgoingConnection)

    def startProxy(port: Int, serviceName: String): Future[Unit] =
      Tcp(system)
        .bindAndHandle(tcpProxy, interface = "localhost", port = port)
        .map(s => Logger(getClass).info(s"Listening for $serviceName requests on ${s.localAddress}"))
        .recover {
          case e: Exception =>
            Logger(getClass).error(s"Could not start TCP proxy for $serviceName requests on $port because of $e")
        }

    startProxy(companyAuthFrontendPort, "company-auth-frontend")
    startProxy(strideAuthFrontendPort, "stride-auth-frontend")
    startProxy(identityVerificationFrontendPort, "identity-verification-frontend")
    startProxy(governmentGatewayRegistrationFrontendPort, "government-gateway-registration-frontend")
    startProxy(personalDetailsValidationFrontendPort, "personal-details-validation-frontend")

  } else {
    println("TCP proxies feature switched off")
  }

}
