package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class TSD extends Config {

  def private static log = Logger.getInstance(TSD.getName());


  /**********************************************************************/
  /**
   * Constructor.
   */

  public TSD() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/tsd_policies2.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD/.*'             // incl
      ]
    ]
  }

}
