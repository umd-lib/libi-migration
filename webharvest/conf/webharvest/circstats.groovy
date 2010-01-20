package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class circstats extends usmai {

  def private static log = Logger.getInstance(circstats.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public circstats() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/circresill/statsarchive.html')

    followable = [
      ['^http://usmai.umd.edu/circresill/.*',  // include
      ],
    ]

  }
}
