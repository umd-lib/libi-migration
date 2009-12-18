package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.Document
import org.dom4j.DocumentHelper

/**
 * Extract the entire website.  Used for link checking, not content extraction.
 */

class lec extends Config {

  def private static log = Logger.getInstance(lec.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public lec() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/lec/')

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

    // first, try to recognize some patterns
    l = doc.selectNodes("/html/body/center[//h1]|//h1|//h2|//span[@class='breadcrumbs']|//tbody/tr/td/h3")
    if (l.size() > 0) {
      def top = l[0].parent

      div = body.addElement('div')

      // check if we are a <center>
      if (top.name == 'center') {
        // move up one level
        top = top.parent
      }

      top.content().each { e ->
        def exclude = false

        // check for exclusions
        if (e.name == 'span' && e.attribute('class')?.value == 'breadcrumbs') {
          exclude = true
        }

        if (!exclude) {
          if (e.name == 'center') {
            // add the children, not the center
            e.content().each { div.add(it.clone()) }
          } else {
            div.add(e.clone())
          }
        }
      }

    } else {

      // the entire body
      l = doc.selectNodes('/html/body')
      div = l[0].clone()
      div.name = 'div'
      body.add(div)
    }

    // Is there a sidebar we can tack onto the end?
    extractBodySidebar(page, doc, body);

    // remove all comments
    l = body.selectNodes("//comment()")
    l.each { e ->
      log.debug("stripping comment: ${e.text}")
      e.detach()
    }
    
    // change all table bgcolor attributes to style
    body.selectNodes("//table//@bgcolor").each { a ->
      def n = a.parent

      // remove bgcolor attribute
      n.remove(a)

      // set style attribute
      def style = n.attribute('style')?.text ?: ''
      if (style) style += '; '
      style += "background-color: ${a.text}"

      n.addAttribute('style', style)
    }

    return body
  }


}
