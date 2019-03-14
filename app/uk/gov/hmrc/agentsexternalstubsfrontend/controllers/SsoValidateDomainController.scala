package uk.gov.hmrc.agentsexternalstubsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class SsoValidateDomainController @Inject()()(implicit val configuration: Configuration) extends FrontendController {

  def validate(domain: String): Action[AnyContent] = Action {
    if (domain != "www.google.com") NoContent else BadRequest
  }

}
