package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class irst extends Config {

  def private static log = Logger.getInstance(irst.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public irst() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/irst/')

    followable = [
      ['^http://www.lib.umd.edu/groups/irst/.*',             // incl
        '^http://www.lib.umd.edu/groups/irst/dateline/.*'    //   excl
      ]
    ]
  }
}
