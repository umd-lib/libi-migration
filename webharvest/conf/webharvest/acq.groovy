package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class acq extends Config {

  def private static log = Logger.getInstance(acq.getName());

  /**********************************************************************/
  /**
   * Constructor.
   */

  public acq() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/ACQ/acqhomepage2.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD/ACQ/.*',                     // include
      ]
    ]
  }
}
