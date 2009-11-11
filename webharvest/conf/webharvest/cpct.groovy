package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class cpct extends Config {

  def private static log = Logger.getInstance(cpct.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public cpct() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/cataloging_policy.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD/cpct.*',                     // include
      ],
      ['^http://www.lib.umd.edu/TSD/ACT.*',                      // include
      ],
    ]
  }
}
