package controllers

import play.api._
import play.api.mvc._

object Artist extends Controller {
  def show(artist_id: Long) = Action {
    val artist = models.Artist(artist_id, "ももいろクローバーZ")
    Ok(views.html.artist(artist))
  }
}
