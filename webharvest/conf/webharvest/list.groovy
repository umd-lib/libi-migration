package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class list extends Config {

  def private static log = Logger.getInstance(list.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public list() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/listeam/listeam.html')

    followable = [
      '^http://www.lib.umd.edu/groups/listeam/.*',             // include
    ]
  }
}
