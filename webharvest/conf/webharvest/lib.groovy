package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.Document
import org.dom4j.DocumentHelper

/**
 * Extract the entire website.  Used for link checking, not content extraction.
 */

class lib extends Config {

  def private static log = Logger.getInstance(lib.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public lib() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/')

    followable = [
      ['^http://www.lib.umd.edu/.*',             // include
      ]
    ]
  }


  /**********************************************************************/
  /**
   * Extract the content body from the html.  This is not necessarily
   * the html body element.
   */

  public Document extractBody(Page page, Document doc) {
    def body = DocumentHelper.createDocument()

    def l 
    def div

    // the entire body
    l = doc.selectNodes('/html/body')
    div = l[0].clone()
    div.name = 'div'
    body.add(div)

    return body
  }
}
