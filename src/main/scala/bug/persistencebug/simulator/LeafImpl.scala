package bug.persistencebug.simulator

import bug.persistencebug.Leaf
import com.github.nscala_time.time.Imports._

// A Lot specific to the Property application
case class LeafImpl(id: Long,
                       location: String,
                       minPrice: Int,
                       maxPrice: Int,
                       creationTime: DateTime,
                       expiryTime: DateTime) extends Leaf{
  override val facets = List(location)

  override def equals(that : Any) = that match {
    case (that : LeafImpl) => this.id == that.id;
    case _ => false;
  }
}
