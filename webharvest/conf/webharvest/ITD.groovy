package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class ITD extends Config {

  def private static log = Logger.getInstance(ITD.getName());


  /**********************************************************************/
  /**
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {
    log.info("Providing authentication for ITD")

    authHeaders.Authorization = 'Basic aXRkc3RhZmY6Q09PS0lO'

    log.debug("Cookies: ${cookies}")
  }


  /**********************************************************************/
  /**
   * Constructor.
   */

  public ITD() {
    super()

    baseUrl = new URL('http://www.itd.umd.edu/ITD/')
  }


}
