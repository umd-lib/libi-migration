package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class la extends Config {

  def private static log = Logger.getInstance(la.getName());


  /**********************************************************************/
  /**
   * Constructor.
   */

  public la() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/la/')
  }

}
