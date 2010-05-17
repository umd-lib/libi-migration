package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class pub extends Config {

  def private static log = Logger.getInstance(pub.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public pub() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/PUB/documentation.html')

    followable = [
      '^http://www.lib.umd.edu/PUB/.*',        
    ]
  }
}
