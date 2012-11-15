package models;
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Ticket(id: Long, name: String, artistId: Option[Long])

case class ArtistTicket(id: Long, name: String, artistId: Long, artistName: String)

object Ticket {
  val default: anorm.RowParser[Ticket] = {
    get[Long]("ticket.id") ~
    get[String]("ticket.name") map {
      case id~name => Ticket(id, name, None)
    }
  }

  def findAllByArtist(artist: Artist): Seq[Ticket] = {
    DB.withConnection { implicit c =>
      SQL("SELECT id, name FROM ticket WHERE artist_id = {artist_id}").on('artist_id -> artist.id).as(Ticket.default*)
    }
  }

  def remainCount(ticketId: Long): Long = {
    DB.withConnection { implicit c =>
      val count = SQL("SELECT COUNT(*) FROM variation INNER JOIN stock ON stock.variation_id = variation.id WHERE variation.ticket_id = {ticket_id} AND stock.order_id IS NULL").on(
        'ticket_id -> ticketId
      ).as(scalar[Long].singleOpt)

      count match {
        case None => 0
        case Some(count) => count
      }
    }
  }

}

object ArtistTicket {
  val default: anorm.RowParser[ArtistTicket] = {
    get[Long]("ticket.id") ~
    get[String]("ticket.name") ~
    get[Long]("ticket.artist_id") ~
    get[String]("artist.name") map {
      case id~name~artistId~artistName => ArtistTicket(id, name, artistId, artistName)
    }
  }

  def findByTicketId(ticketId: Long): Option[ArtistTicket] = {
    DB.withConnection { implicit c =>
      SQL("SELECT t.*, a.name AS artist_name FROM ticket t INNER JOIN artist a ON t.artist_id = a.id WHERE t.id = {ticket_id} LIMIT 1").on(
        'ticket_id -> ticketId
      ).as(ArtistTicket.default.singleOpt)
    }
  }

  def toTicket(artistTicket: ArtistTicket): Ticket = {
    Ticket(artistTicket.id, artistTicket.name, Some(artistTicket.artistId))
  }

}

