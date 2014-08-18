package bug.persistencebug

import java.util.Date
import com.github.nscala_time.time.Imports._

trait Leaf extends Matchable{

  val id: Long
  val facets: List[String]
  val minPrice: Int
  val maxPrice: Int
  val creationTime: DateTime
  val expiryTime: DateTime
  var currentPricePerDay = maxPrice

  override def equals(that : Any) = that match {
    case (that : Leaf) => this.id == that.id;
    case _ => false;
  }


  override def toString =
  "Lot. Id: %s, Facets: %s, Min Price: %s, Max Price: %s".
  format(id, facets mkString "/", minPrice, maxPrice)
}
