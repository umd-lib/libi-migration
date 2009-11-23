package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class pres extends Config {

  def private static log = Logger.getInstance(pres.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public pres() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/PRES/1Preshp.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD/PRES/.*',                     // include
      ],
    ]

  }
}
