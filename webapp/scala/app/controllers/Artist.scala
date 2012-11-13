package controllers

import play.api._
import play.api.mvc._

object Artist extends Controller {
  def show(artist_id: Long) = Action {
    val artist = models.Artist.find(artist_id)
    artist match {
      case None =>
        NotFound
      case Some(artist) =>
        val tickets = models.Ticket.findAllByArtist(artist)
        Ok(views.html.artist(artist, tickets))
    }
  }
}
