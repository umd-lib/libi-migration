package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class mddaypics extends Config {

  def private static log = Logger.getInstance(mddaypics.getName());

  /**********************************************************************/
  /**
   * Constructor.
   */

  public mddaypics() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/mdday05/mdday08.html')

    followable = [
      ['^http://www.lib.umd.edu/groups/mdday05/.*',   // include
      ]
    ]
  }
}
