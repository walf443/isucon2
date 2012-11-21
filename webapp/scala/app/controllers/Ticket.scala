package controllers
import play.api._
import play.api.data._
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

  import play.api.data.Forms._
  val buyForm: Form[models.Order] = Form(mapping(
    "variationId" -> longNumber,
    "memberId"    -> longNumber
  )(models.Order.apply)(models.Order.unapply))

  def buy() = Action { implicit request =>
    buyForm.bindFromRequest.fold(
      errors => BadRequest("bad request"),
      ticket => {
        val seatId = models.Ticket.buy(ticket.memberId, ticket.variationId)
        seatId match {
          case None =>
            Ok(views.html.soldout())
          case Some(seatId) =>
            Ok(views.html.complete(seatId, ticket.memberId))
        }
      }
    )
  }
}
