package decac

import scala.collection.mutable.Set
import scala.collection.mutable.HashSet
import java.lang.Math
import jllvm.LLVMType
import jllvm.LLVMIntegerType

class EnumeratedRho(p: Option[EnumeratedRho],syms: List[String]) extends PrimitiveRho {
  val parent: Option[EnumeratedRho] = p
  val symbols: List[String] = syms
  var values: List[Int] = Nil
  var childEnums: Set[EnumeratedRho] = new HashSet[EnumeratedRho]()
  
  protected def assign_symbol_values(s: Int): Int = {
    values = Nil
    var start = s
    var i = 0
    for(symbol <- symbols) {
      values = values ::: List(i)
      i += 1
    }
    start += values.length
    for(child <- childEnums)
      start = child.assign_symbol_values(start)
    return start
  }
  
  def assign_values(): Unit = parent match {
    case None => assign_symbol_values(0)
    case Some(parent) => parent.assign_values
  }
  
  protected def calculateRepresentationSize: Int = {
    values.length + childEnums.map(child => child.calculateRepresentationSize).foldLeft(0)((x,y) => x + y)
  }
  
  protected def representationSize: Int = parent match {
    case None => calculateRepresentationSize
    case Some(par) => par.representationSize
  }
  
  override def subtypes(tau: TauType,possibly: Boolean) = tau match {
    case enum: EnumeratedRho => parent match {
      case Some(par) => enum == par || par.subtypes(enum,possibly)
      case None => false
    }
    case range: RhoRange => subtypes(range.lowerBound,possibly)
    case tvar: TauVariable => possibly
    case _ => false
  }
  
  override def compile(substitution: TauSubstitution): Option[LLVMType] = {
    val numBits = representationSize
    Some(new LLVMIntegerType(numBits))
  }
}

object BooleanRho extends EnumeratedRho(None,List("true","false"))
