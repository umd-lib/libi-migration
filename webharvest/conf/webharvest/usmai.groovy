package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class usmai extends Config {

  def private static log = Logger.getInstance(usmai.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public usmai() {
    super()
  }

  /**********************************************************************/
  /**
   * Extract the content body from the html.
   */

  public Document extractBody(Page page, Document doc) {
    def body = DocumentHelper.createDocument()
    def div = body.addElement('div')

    def l 


    // use the body, strip out header and footer

    l = doc.selectNodes("/html/body/*")
    l.each { e ->
      div.add(e.clone())
    }

    return body
  }

}
