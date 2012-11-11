package controllers

import play.api._
import scala.io.Source
import play.api.mvc._
import play.api.Play.current
import play.api.db._
import anorm.SQL

object Admin extends Controller {
  def index() = Action {
    Ok(views.html.admin())
  }
  def init_data() = Action {
    val source = Source.fromFile("../config/database/initial_data.sql")
    DB.withConnection { implicit c =>
      source.getLines.foreach( line =>
        if ( line != "" ) {
          SQL(line).execute()
        }
      )
    }

    Redirect(routes.Admin.index)
  }
  def download_order_csv() = TODO
}

