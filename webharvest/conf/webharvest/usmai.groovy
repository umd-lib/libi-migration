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
    def inFooter = false
    l.each { e ->
      if (e.matches("/html/body/table[tbody/tr/td[@class='usmai']]")) {
        // skip the header
      } else if (e.matches("/html/body/table[tbody/tr[@class='usmaibar']]")) {
        // skip usmai bar
      } else if (inFooter || e.matches("/html/body/p[@class='footer']")) {
        // skip the footer
        inFooter = true
      } else {
        div.add(e.clone())
      }
    }

    return body
  }

}
