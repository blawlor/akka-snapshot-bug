package bug.persistencebug

trait Matchable {
  val facets: List[String]

  def searchPath:String = facets mkString("/")
  def searchElement(index: Int) = searchPath.split('/')(index)
  def searchElementList:List[String] = searchPath.split('/').toList

}
