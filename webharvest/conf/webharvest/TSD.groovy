package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class TSD extends Config {

  def followable = ['http://www.lib.umd.edu/TSD/']

  def private static log = Logger.getInstance(TSD.getName());


  /**********************************************************************/
  /**
   * Constructor.
   */

  public TSD() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/tsd_policies2.html')
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    if (page.url.query != null) return false;

    return page.surl.startsWith(followable[0])
  }


}
