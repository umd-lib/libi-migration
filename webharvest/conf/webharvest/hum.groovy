package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class hum extends Config {

  def private static log = Logger.getInstance(hum.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public hum() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/PUBSERV/humteam.html')

    followable = [
      '^http://www.lib.umd.edu/PUBSERV/hum.*',             // include
    ]
  }
}
