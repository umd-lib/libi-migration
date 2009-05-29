package webharvest

import org.dom4j.Node

class ConfigTest extends Config {

  /**
   * Constructor.
   */

  public ConfigTest() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/JUNK/ben/webharvest/')
  }


  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String titlex (URL url, Node doc, Node body) {
    return 'foobar'
  }

  public String getName(Page page, Node doc, Node body) { return 'xyz' }
}
