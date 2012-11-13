package controllers

import play.api._
import play.api.mvc._
import models._

object Application extends Controller {
  
  def index = Action {
    val artists = Array(models.Artist(1, "NHN48"), models.Artist(2, "はだいろクローバーZ"))
    Ok(views.html.index(artists))
  }
  
}
