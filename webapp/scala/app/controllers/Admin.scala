package controllers

import play.api._
import play.api.mvc._
import anorm.SQL

object Admin extends Controller {
  def index() = Action {
    Ok(views.html.admin())
  }
  def init_data() = Action {

    Redirect(routes.Admin.index)
  }
  def download_order_csv() = TODO
}

