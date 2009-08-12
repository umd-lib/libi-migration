package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class lfrf extends Config {

  def private static log = Logger.getInstance(lfrf.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public lfrf() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/la/lfrf/')


    def awards = new URL('http://www.lib.umd.edu/groups/la/lfrf/awards/');
    buildUrls[awards] = awards

  }
}
