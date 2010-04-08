package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class faroc extends Config {

  def private static log = Logger.getInstance(faroc.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public faroc() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/la/FAROC/faroc_index.html')

    followable = [
      ['^http://www.lib.umd.edu/groups/la/FAROC/.*',        
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

    // first, try to recognize some patterns
    l = doc.selectNodes("/html/body/center[//h1]|//h1|//h2|//tbody/tr/td/h3|//span[@class='breadcrumbs']|//p[@class='menu_text'")
    if (l.size() > 0) {
      log.debug("body match: " + l[0].asXML())

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
