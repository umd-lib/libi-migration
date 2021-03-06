package webharvest

import org.apache.log4j.Logger

import org.dom4j.Document
import org.dom4j.Node
import org.dom4j.DocumentHelper


class TSD extends Config {

  def private static log = Logger.getInstance(TSD.getName());

  def tsd = new File('/r/department/Technical Services/TSDWWW/TSDPOLPRO')

  def sidebar = []   // list of url to keep sidebar
  def sidebarSaved = null  // a saved sidebar, not included in the body


  /**********************************************************************/
  /**
   * Constructor.
   */

  public TSD() {
    super()

    if (! tsd.exists()) {
      throw new Exception("TSD WWW is not mounted")
    }

    baseUrl = new URL('http://www.lib.umd.edu/TSD/tsdx.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD/.+',                    // include
        'http://www.lib.umd.edu/TSD/TEST.*',                //   exclude
        'http://www.lib.umd.edu/TSD/checklist_form.html',   //   exclude
        'http://www.lib.umd.edu/TSD/cpaleph_authorization.html', //exclude
        'http://www.lib.umd.edu/TSD/web_procedure.html',    //   exclude
        'http://www.lib.umd.edu/TSD/tsd_timesheets.html',   //   exclude
        'http://www.lib.umd.edu/TSD/helpdesk.html',                  //   exclude
        'http://www.lib.umd.edu/TSD/tsd.html',                       //   exclude
        'http://www.lib.umd.edu/TSD/PRES/disasterplan.html',         //   exclude
        'http://www.lib.umd.edu/TSD/PRES/Microfilm.html',            //   exclude
        'http://www.lib.umd.edu/TSD/ACQ/acqhomepage.html',           //   exclude
        'http://www.lib.umd.edu/TSD/CATDEPT/OCPGHome.html',          //   exclude
        'http://www.lib.umd.edu/TSD/ACDM/ACDM.html',                 //   exclude
        'http://www.lib.umd.edu/TSD/PRES/1Preshp.html',              //   exclude
        'http://www.lib.umd.edu/TSD/tsd_comm.html',                  //   exclude
        'http://www.lib.umd.edu/TSD/workingpaper6_ptA.html',         //   exclude
        'http://www.lib.umd.edu/TSD/tsd_orgchart.html',              //   exclude
        'http://www.lib.umd.edu/TSD/IMG/search_tsd_7.gif',           //   exclude
        'http://www.lib.umd.edu/TSD/CATDEPT/OCPG_Staff.html',        //   exclude
        'http://www.lib.umd.edu/TSD/CATDEPT/OrCD_OrgChart_Nov2009.png',   //exclude
        'http://www.lib.umd.edu/TSD/CATDEPT/projectscatalogingteam.html', //exclude
        'http://www.lib.umd.edu/TSD/CATDEPT/schalowjmt.html',             //exclude
        'http://www.lib.umd.edu/TSD/CR_contact_list.doc',                 //exclude
      ]
    ]

    // set the depth limit
    if (! var.depth) {
      var.depth = 2
    }

    // retain all sidebars
    sidebar = null
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
   * Is there a sidebar we can tack onto the end?
   */

  public void extractBodySidebar(Page page, Document doc, Document body) {

    sidebarSaved = null

    if (sidebar == null || page.surl in sidebar) {
      super.extractBodySidebar(page, doc, body);

    } else {
      def l = doc.selectNodes(sidebarSelection);

      if (l.size() > 0) {
        sidebarSaved = l[0].clone()
      }
    }

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
   * Extract all links from the body and non-extracted sidebars.  Determine elsewhere if 
   * they are followable.
   */

  public List getLinks(Node body) {
    def links = super.getLinks(body);

    if (sidebarSaved) {
      links += super.getLinks(df.createDocument(sidebarSaved))
    }

    return links
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
