package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class acdm extends Config {

  def private static log = Logger.getInstance(acdm.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public acdm() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/ACDM/ACDM.html')

    followable = [
      '^http://www.lib.umd.edu/TSD/ACDM/.*',             // include
    ]
  }
}
