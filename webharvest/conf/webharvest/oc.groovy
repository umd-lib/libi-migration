package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class oc extends Config {

  def private static log = Logger.getInstance(oc.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public oc() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/CATDEPT/OCPGHome.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD/CATDEPT/.*',                     // include
      ],
    ]
  }
}
