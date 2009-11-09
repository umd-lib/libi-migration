package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class nevpc extends Config {

  def private static log = Logger.getInstance(nevpc.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public nevpc() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/la/NEVPC/')

    var.urlFixups = [
      '/nevpc': '/NEVPC',
      '/groups/lfa': '/groups/la',
    ]
  }
}
