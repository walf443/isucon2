package models;
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Ticket(id: Long, name: String, artist_id: Option[Long])

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
}

