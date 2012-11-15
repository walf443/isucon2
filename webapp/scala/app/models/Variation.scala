package models;
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Variation(id: Long, name: String, ticketId: Long)

object Variation {
  val default: anorm.RowParser[Variation] = {
    get[Long]("variation.id") ~
    get[String]("variation.name") ~
    get[Long]("variation.ticket_id") map {
      case id~name~ticketId => Variation(id, name, ticketId)
    }
  }

  def findAllByTicket(ticket: Ticket): Seq[Variation] = {
    DB.withConnection { implicit c =>
      SQL("SELECT id, name, ticket_id FROM variation WHERE ticket_id = {ticketId} ORDER BY id").on(
        'ticketId -> ticket.id
      ).as(Variation.default*)
    }
  }
}
