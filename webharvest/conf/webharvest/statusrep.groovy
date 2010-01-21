package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class statusrep extends usmai {

  def private static log = Logger.getInstance(statusrep.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public statusrep() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/statusrep/')

    followable = [
      ['http://usmai.umd.edu/statusrep.*', ],
      ['http://www.itd.umd.edu/LIMS3/statusrep.*'],
    ]

  }
}
