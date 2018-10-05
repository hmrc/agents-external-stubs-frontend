package uk.gov.hmrc.agentsexternalstubsfrontend.models
import play.api.mvc.Call
import uk.gov.hmrc.agentsexternalstubsfrontend.services.Features

trait PageContext {
  def planetId: String
  def authorisedUserId: String
  def menuItems: Seq[MenuItem]
  def features: Features
}

case class MenuItem(id: String, messageKey: String, call: Call)
