package parser

import javax.xml.bind.DatatypeConverter

import scala.xml._

/**
  * Created by anders on 05/04/2016.
  */
object MachineParser {

  case class BMachine(name: String, variables: Seq[Node], events: Seq[Node])

  /**
    * Reads the given UTF-8 file and returns its BMachine
    *
    * @param filepath path to event-b machine
    * @return parsed BMmachine with accessible attributes
    */
  def parseMachine(filepath: String): BMachine = {
    val xmlRoot = XML.loadFile(filepath)

    val machineName = filepath.split('/').last.dropRight(4)
    val variables: Seq[Node] = xmlRoot \ "org.eventb.core.variable"
    val events: Seq[Node] = xmlRoot \ "org.eventb.core.event"

    BMachine(machineName, variables, events)
  }

  /*
  Upon further reflection, combining machines might be more difficult than
  anticipated. Leaving this for future work.
   */
  def combineMachines(bm1: BMachine, bm2: BMachine): BMachine = {
    val variables: Seq[Node] = bm1.variables ++ bm2.variables
    BMachine("NewMachine", bm1.variables ++ bm2.variables, bm1.events ++ bm2.events)
  }


  // labels have attribute @name that starts a 28. 28 is the Hex for '('
  def combineVariables(v1: Seq[Node], v2: Seq[Node]): Seq[Node] = {
    // Keep names of v1, increment v2
    val max = getHighestValuedName(v1)
    val v2Incremented =
      v2
        .map(variable => increment(variable, max))
    ???
  }

  // Lets not assume the nodes are ordered
  def getHighestValuedName(nodes: Seq[Node]): Char = {
    nodes
      .map(node => node \@ "name")
      .max
      .charAt(0)
  }
/*
  This method is a mess and does not work. Why numbers are not used for lines
   will remain a mystery.
   Missing: Trivial implementation of HexString -> Int -> HexString
  */
  def increment(node: Node, max: Char): Node = {
    val increment = max - 27
    new scala.xml.transform.RewriteRule {

      def magic(s: String, inc: Int): String = {
        val hex = bytes2hex(s.getBytes)
        val incHex = (Integer.parseInt(hex, 10) + 1).toString
        val bytes: Array[Byte] = DatatypeConverter.parseHexBinary(incHex)
        val result: String = new String(bytes, "UTF-8")
        result
      }

      def bytes2hex(bytes: Array[Byte], sep: String = ""): String = bytes.map("%02x".format(_)).mkString(sep)


      override def transform(n: Node): Node = n match {
        case Elem(prefix, variable, attribs, scope, _*) => {
          Elem(prefix, variable,
            attribs.append(Attribute("", "org.eventb.core.name",
              magic(attribs.get("name").get.text, 1),
              attribs)),
            scope, minimizeEmpty = true)
        }
      }
    } transform node

  }
}
