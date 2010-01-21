package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class devplan extends usmai {

  def private static log = Logger.getInstance(devplan.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public devplan() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/itd/devplan/')

    followable = [
      ['http://usmai.umd.edu/itd/devplan/.*'],
    ]

  }
}
