package webharvest

import java.util.regex.Pattern;

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

  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String getTitle (Page page, Node doc, Node body) {
    static pattern = Pattern.compile('^USMAI *-? *')

    String title = super.getTitle(page, doc, body)

    title = pattern.matcher(title).replaceAll('')

    if (title.equals('')) {
      title = new File(URLDecoder.decode(page.url.path,'UTF-8')).name
    }

    return title;
  }

  /**********************************************************************/
  /**
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {
    log.info("Providing authentication for ITD")

    // Read in auth password
    def binding = new Binding()
    def shell = new GroovyShell(binding)

    try {
      shell.evaluate(new File('conf/usmai.conf'))

      authHeaders.Authorization = binding.getVariable('auth')
    }
    catch (Throwable t) {
      t.printStackTrace()
      System.exit(1)
    }

    log.debug("Cookies: ${cookies}")
  }

}
