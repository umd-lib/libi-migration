package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class hr extends Config {

  def private static log = Logger.getInstance(hr.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public hr() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/PASD/hrpolicies.html')

    followable = [
      ['^http://www.lib.umd.edu/PASD/.*',        
        '^http://www.lib.umd.edu/PASD/asd.html',
        '^http://www.lib.umd.edu/PASD/LPO/TRAVEL/.*',
      ]
    ]

    def start = [
      'http://www.lib.umd.edu/PASD/hrservices.html',
      'http://www.lib.umd.edu/PASD/hrforms.html',
      'http://www.lib.umd.edu/PASD/hrcollaborative.html',
    ]

    start.each {
      def url = new URL(it)
      def page = new Page(url:url, depth:0)
      page.ctype = getContentType(page) 
      urlTodo << page
    }

  }
}
