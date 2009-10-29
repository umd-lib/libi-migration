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
        '.*/JUNK/.*',                              // exclude
        '^http://www.lib.umd.edu/LAB/DB/.*',       // exclude
        '^http://www.lib.umd.edu/LAB/IMG/.*',      // exclude
        '^http://www.lib.umd.edu/LAB/JERRYLEE/.*', // exclude
        '^http://www.lib.umd.edu/archivesum/.*',   // exclude
        '^http://www.lib.umd.edu/blogs/.*',        // exclude
        '^http://www.lib.umd.edu/cgi-bin/.*',      // exclude
        '^http://www.lib.umd.edu/dcr/civilwar/.*', // exclude
        '^http://www.lib.umd.edu/dcr/collections/sterling/.*', // exclude
        '^http://www.lib.umd.edu/digital/.*',      // exclude
        '^http://www.lib.umd.edu/drum/.*',         // exclude
        '^http://www.lib.umd.edu/eres/.*',         // exclude
        '^http://www.lib.umd.edu/itd/web/.*',      // exclude
        '^http://www.lib.umd.edu/sapps/.*',        // exclude
        '^http://www.lib.umd.edu/stats/.*',        // exclude
        '^http://www.lib.umd.edu/twiki/.*',        // exclude
      ]
    ]

    // set nofiles option
    if (!var.nofiles) {
      var.nofiles = 'true'
    }

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
