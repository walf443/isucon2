package models;
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Artist(id: Long, name: String)

object Artist {
  val default: anorm.RowParser[models.Artist] = {
    get[Long]("artist.id") ~
    get[String]("artist.name") map {
      case id~name=> Artist(id, name)
    }
  }

  def list: Seq[Artist] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM artist").as(Artist.default*)
    }
  }

  def find(id:Long): Option[Artist] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM artist WHERE id = {id} ").on('id -> id).as(Artist.default.singleOpt)
    }
  }
}
