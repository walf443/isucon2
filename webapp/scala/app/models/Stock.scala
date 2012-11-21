package models;
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Stock(id: Long, variationId: Long, seatId: String, orderId: Option[Long])

case class Order(variationId: Long, memberId: Long)

object Stock {
  val default: anorm.RowParser[Stock] = {
    get[Long]("stock.id") ~
    get[Long]("stock.variation_id") ~
    get[String]("stock.seat_id") ~
    get[Option[Long]]("stock.order_id") map {
      case id~variation_id~seat_id~order_id => Stock(id, variation_id, seat_id, order_id)
    }
  }

  def findAllByVariation(variation: Variation): Seq[Stock] = {
    DB.withConnection { implicit c =>
      SQL("SELECT id, variation_id, seat_id, order_id FROM stock WHERE variation_id = {variationId}").on(
        'variationId -> variation.id
      ).as(default*)
    }
  }

  def getSeatMapByVariation(variation: Variation): Map[String, Stock] = {
    val stocks = this.findAllByVariation(variation)
    stocks.map { stock =>
      stock.seatId -> stock
    }.toMap
  }
}

