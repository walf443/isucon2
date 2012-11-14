package controllers
import play.api._
import play.api.mvc._

object Ticket extends Controller {
  def show(ticket_id: Long) = Action {
    val artistTicket = models.ArtistTicket.findByTicketId(ticket_id)
    artistTicket match {
      case None => NotFound
      case Some(artistTicket) =>
        Ok(views.html.ticket(artistTicket))
    }
  }

  def buy() = TODO
}
