package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class TSD extends Config {

  def private static log = Logger.getInstance(TSD.getName());

  def tsd = new File('/r/department/Technical Services/TSDWWW/TSDPOLPRO')


  /**********************************************************************/
  /**
   * Constructor.
   */

  public TSD() {
    super()

    if (! tsd.exists()) {
      throw new Exception("TSD WWW is not mounted")
    }

    baseUrl = new URL('http://www.lib.umd.edu/TSD/tsd.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD.*',                     // include
      ]
    ]
  }


  /**********************************************************************/
  /**
   * Build a new url. Perform normalization.
   */

  public URL buildUrl(URL baseUrl, String rel) {
    def m = rel =~ /http:\/\/libdoc.*\/tsdwww\/tsdpolpro\/(.*)/
    if (m) {
      // LAN file
      def f = new File(tsd, m[0][1])
      if (! f.exists()) {
        throw new Exception("${f} does not exist")
      }

      return f.toURI().toURL()
    }

    return super.buildUrl(baseUrl, rel)
  }


  /**********************************************************************/
  /**
   * Get the content-type of one url.
   */

  public String getContentType(Page page) {

    if (page.url.protocol == 'file') {
      // LAN file
      return 'application/lanfile'
    }

    return super.getContentType(page)
  }


  /**********************************************************************/
  /**
   * Handle a file for download.
   */

  public void handleFile(Page page) {

    if (page.url.protocol == 'file') {
      // LAN file

      page.download = getDownload(page)

      log.debug("Download file name: ${page.download}")

      // copy LAN file to work file
      page.download << page.url.openStream()

      // create dummy content
      def doc = DocumentHelper.createDocument()
      doc.addElement('html').addElement('body')

      def body = DocumentHelper.createDocument()
      body.addElement('div')
    
      savePage(page, doc, body)
      
      return
    }

    super.handleFile(page)
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    if (page.url.protocol == 'file') {
      // LAN file
      return true;
    }

    return super.isFollowable(page)
  }


}
