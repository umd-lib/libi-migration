package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class grants extends Config {

  def private static log = Logger.getInstance(grants.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public grants() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/PASD/About.html')

    followable = [
      ['^http://www.lib.umd.edu/PASD/.*',             // include
        '^http://www.lib.umd.edu/PASD/Project_Guide.html',
        '^http://www.lib.umd.edu/PASD/Grants.html',
      ]
    ]
  }
}
