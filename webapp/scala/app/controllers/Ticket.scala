package controllers
import play.api._
import play.api.mvc._

object Ticket extends Controller {
  def show(ticket_id: Long) = Action {
    val artistTicket = models.ArtistTicket.findByTicketId(ticket_id)
    val seatSeq = 0 to 63
    artistTicket match {
      case None => NotFound
      case Some(artistTicket) =>
        val variations = models.Variation.findAllByTicket(models.ArtistTicket.toTicket(artistTicket))
        val remainCountOf = variations.map(v => (v.id -> models.Variation.remainCount(v.id))).toMap
        val stockOf = variations.map(v => (v.id -> models.Stock.getSeatMapByVariation(v))).toMap
        Ok(views.html.ticket(artistTicket, variations, remainCountOf, stockOf, seatSeq))
    }
  }

  def buy() = TODO
}
