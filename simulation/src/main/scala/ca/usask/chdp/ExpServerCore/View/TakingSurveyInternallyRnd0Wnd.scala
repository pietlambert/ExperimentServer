package ca.usask.chdp.ExpServerCore.View

import com.vaadin.ui._
import com.vaadin.ui.Button.{ClickEvent, ClickListener}
import com.vaadin.server._
import org.slf4j.LoggerFactory
import ca.usask.chdp.ExpServerCore.View.components.LikertQuestion
import ca.usask.chdp.ExpServerCore.ExpActors.PlayerLogic.{FinishedSurvey, PlayerInfo}
import ca.usask.chdp.ExpServerCore.ExpActors.Lobby
import akka.actor.ActorSystem
import ca.usask.chdp.ExpServerCore.ExpActors.Lobby.WaitingPlayer
import ca.usask.chdp.ExpServerCore.ExpActors.ViewActor.FinishedFirstSurvey

/**
 * This is poor design, but it's late and the night before.
 * TODO: Refactor Me.
 */
class TakingSurveyInternallyRnd0Wnd(plr: WaitingPlayer, viewMgr: ViewManager) extends Window {
  val log = LoggerFactory.getLogger(this.getClass)

  setModal(true)
  setReadOnly(true)
  setResizable(false)
  addStyleName("internalSurveyWnd")
  //  val layout = new CustomLayout( "main/basicWndLayout")
  val layout = new CssLayout()
  layout.addStyleName("surveyWndContainer")
  setContent(layout)
  def closeMe() { this.close() }

  val title = new Label("A Quick survey before we start.")
  title.addStyleName("surveyTitle")
  layout.addComponent(title)

  // add the questions to a horizontal layout, headed by the instructions:
  val questionsContainer = new VerticalLayout()
  questionsContainer.addStyleName("questionsContainer")
  val instructions = new Label(
    "You will now be asked to answer some brief questions about how you feel about your relationship with your teammate. We know you haven't started the simulation yet, but we want your impressions before you begin the game.\n\n It is perfectly fine to answer \"Neutral\" or \"Disagree\" to questions, if that's how you feel. The simulation will start after this.")

  val responsibilityHeading = new Label(
    "Consider what you know so far. How do you view your teammate's task in this simulation?")
  responsibilityHeading.addStyleName("heading")
  val responsibilityQs = List(
    "I feel a very high degree of <u>personal</u> responsibility for the work my  teammate does on this job.",
    "I feel I should personally take the credit or blame for the results of my teammate's work on this job.",
    "Whether or not my teammate's job gets done right is clearly <u>my</u> responsibility.",
    "It's hard, on this job, for me to care very much about whether or not my teammate's work gets done right.")

  val obligationHeading = new Label(
    "Consider the relationship you have with your teammate. How do you feel you should act?")
  obligationHeading.addStyleName("heading")
  val obligationQs = List(
    "I feel a sense of <emph>obligation</emph> to help my teammate complete their task.",
    "I feel like I <emph>should</emph> help my teammate during my part of the task.",
    "I feel my teammate needs my help to complete their task.")

  val cohesionHeading = new Label(
    "Consider the simulation so far, how do you view you and your teammate?")
  cohesionHeading.addStyleName("heading")
  val cohesionQs = List(
    "We all want to achieve the same in our team.",
    "We agree on what is important for our team.",
    "The goals of different team members are not in line.",
    "I found it difficult to balance my needs with my teammate's needs.",
    "I found it tough to decide what I should do.",
    "I felt pressure to behave differently than how I really wanted to behave.")

  // build the questions.
  questionsContainer.addComponents(instructions)
  val respLQs = for ((q, index) <- responsibilityQs.zipWithIndex) yield {
    new LikertQuestion(q, (index % 2 == 0))
  }
  questionsContainer.addComponents((responsibilityHeading :: respLQs): _*)
  val obligLQs = for ((q, index) <- obligationQs.zipWithIndex) yield {
    new LikertQuestion(q, (index % 2 == 0))
  }
  questionsContainer.addComponents((obligationHeading :: obligLQs): _*)

  val firstHalfQs = respLQs ++ obligLQs

  layout.addComponent(questionsContainer)

  val okButton = {
    new Button(
      "Finished", new ClickListener {
        def buttonClick(event: ClickEvent) {
          if (allQuestionsAnswered(firstHalfQs)) {
            viewMgr.send(FinishedFirstSurvey(plr, getQuestionReport(firstHalfQs)))
            closeMe()
          } else {
            val note = new Notification(
              "Need to answer each question before continuing.",
              Notification.Type.TRAY_NOTIFICATION)
            note.setDelayMsec(4000)
            note.show(Page.getCurrent)
          }
        }
      })
  }
  okButton.setSizeUndefined()
  okButton.addStyleName("btn btn-primary")
  layout.addComponent(okButton)

  def allQuestionsAnswered(qs: List[LikertQuestion]): Boolean = {
    qs.foldLeft(true)(_ && _.isQuestionAnswered)
  }
  def getQuestionReport(qs: List[LikertQuestion]): Map[String, Int] = {
    (for ((q, idx) <- qs.zipWithIndex) yield {
      ("Q" + (idx + 1), q.getQuestionVal)
    }).toMap
  }

  if (Lobby.settings.testingMode && Lobby.settings.testing_autoClickInternalSurvey) {
    import scala.concurrent.duration._
    val as: ActorSystem = Lobby.system
    import as.dispatcher

    Lobby.system.scheduler.scheduleOnce(Lobby.settings.autoWorkDelay milliseconds) {
      val lock = layout.getUI.getSession.getLockInstance
      lock.lock()
      try {
        println("TESTING_AUTOCLICK First SURVEY")
        viewMgr.send(FinishedFirstSurvey(plr, Map(("testing", 123))))
        closeMe()
      } finally {
        lock.unlock()
      }
    }
  }

}
