package ca.usask.chdp.ExpServerCore.Tutorial

import com.vaadin.ui.{JavaScriptFunction, Button, CustomLayout, CustomComponent}
import ca.usask.chdp.ExpServerCore.View.jsExtensions.TooltipGuider
import com.vaadin.server.ClientConnector.{AttachEvent, AttachListener}
import org.json.JSONArray
import ca.usask.chdp.ExpServerCore.ExpActors.PlayerLogic.PlayerInfo

trait DamageReportTutorialView {
  this: CustomComponent =>
  val layout: CustomLayout
  val continue: Button
  val player: PlayerInfo

  val tooltipGuide = player.role match {
    case "RoleA" => new TooltipGuider("tutorialADamageReport")
    case _ => new TooltipGuider("tutorialBDamageReport")
  }
  tooltipGuide.extend(this)
  this.addAttachListener(new AttachListener {
    def attach(event: AttachEvent) {
      registerTutorialCallbacks()
      tooltipGuide.initGuiders()
      tooltipGuide.start()
      continue.setEnabled(false)
    }
  })
  def registerTutorialCallbacks() {
    layout.getUI.getPage.getJavaScript.addFunction("chdp_DamageReportTutorialView_enableContinueButton", new JavaScriptFunction {
      def call(arguments: JSONArray) {
        continue.setEnabled(true)
      }
    })
  }

}
