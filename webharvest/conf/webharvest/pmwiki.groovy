package webharvest

import groovy.lang.Binding
import groovy.lang.GroovyShell

import static groovyx.net.http.ContentType.URLENC

import groovyx.net.http.HTTPBuilder

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node

import org.apache.log4j.Logger


/**
   PmWiki

   Authentication:
     Request: POST, authpw=<passwd>
     Response: Set-Cookie	PHPSESSID=51e0537c81b68481e7d77726ca9fc840; path=/
*/


class pmwiki extends Config {

  def pmPasswords = null

  def pmExclusions = ['RecentChanges']

  def private log = Logger.getInstance(pmwiki.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public pmwiki() {
    super()

    Page.ignoreQuery = false

    var.pmHome = 'ITDStaff'
    var.pmPage = 'HomePage'

    followable = [
      ['^http://www.itd.umd.edu/pmwiki/itdextras/.*'             // incl
      ]
    ]

    var.urlFixups = [
      '\\?action=': '&action=',
    ]

    followUrl[(new URL('http://www.itd.umd.edu/pmwiki/pmwiki.php?n=ASTG.Yalan-NewPage&action=download&upname=test.html')).toURI()] = false

    // Read in PmWiki passwords
    def binding = new Binding()
    def shell = new GroovyShell(binding)

    try {
      shell.evaluate(new File('conf/pmwiki.conf'))
      pmPasswords = binding.getVariable('pmPasswords')
    }
    catch (Throwable t) {
      t.printStackTrace()
      System.exit(1)
    }
  }


  /**********************************************************************/
  /**
   * Begin the fetch cycle
   */

  public void harvest() {
    // create baseUrl after command vars have been set
    baseUrl = new URL("http://www.itd.umd.edu/pmwiki/pmwiki.php?n=${var.pmHome}.${var.pmPage}")

    super.harvest()
  }

    
  /**********************************************************************/
  /**
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {
    log.info("Providing authentication for PmWiki")

    def http = new HTTPBuilder(baseUrl)
    def passwd = pmPasswords[var.pmHome] ?: pmPasswords.admin
    def postBody = [authpw:passwd]

    def cookie = null

    http.post(body: postBody, requestContentType: URLENC ) { resp ->

      if (resp.headers.'Set-Cookie') {
        (cookie) = resp.headers.'Set-Cookie'.split(';')
      }
    }

    def (key, value) = cookie.split('=')
    cookies[key] = value

    log.debug("Cookies: ${cookies}")
  }


  /**********************************************************************/
  /**
   * Build the tree of nodes.  Make the first node in the list the parent
   * of all the rest of the nodes.
   */

  public List buildTree(List done) {
    return ['nodes'] + [done]
  }


  /**********************************************************************/
  /**
   * Extract the content body from the html.
   */

  public Document extractBody(Page page, Document doc) {
    def body = DocumentHelper.createDocument()

    def l = doc.selectNodes("//div[@id='wikitext']");

    l.each { body.add(it.clone()) }

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
   * Get the charset of a page in handleHtml
   */

  public String getCharset(Page page, groovyx.net.http.HttpResponseDecorator$HeadersDecorator headers) {

    return 'WINDOWS-1252'
  }


  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String getTitle (Page page, Node doc, Node body) {
    if (page.download) {
      if (page.query.upname) {
        return new File(URLDecoder.decode(page.query.upname,'UTF-8')).name
      } else {
        return new File(URLDecoder.decode(page.url.path,'UTF-8')).name
      }
    }

    def title = (page.query.n?.split('\\.'))[1]

    if (title == var.pmPage) {
      title = var.pmHome
    }

    return title
  }


  /**********************************************************************/
  /**
   * Get unique id of the node.
   */

  public String getUnique (Page page) {

    if (page.query.upname) {
      return page.query.upname
    } else if (page.query.n) {
      return page.query.n
    } else {
      return page.urlNoAnchor.toString()
    }
  }


  /**********************************************************************/
  /**
   * Get the download file name.
   */

  public File getDownload(Page page) {
    def name = page.query?.upname?.replace('%20','_')

    if (name == null) {
      def x = new File(page.url.file)
      name = x.name.replace('%20','_')
    }

    def file = new File(downloaddir, name)

    for (def i=0; file.exists(); i++) {
      // split into base and extensions
      def parts = name.split('\\.') as List
      def base = parts.remove(0)

      // create next name
      def testname = "${base}_${i}"
      parts.each { testname += ".${it}" }

      file = new File(downloaddir, testname)
    }

    return file
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    def url = page.url

    if (followUrl.containsKey(url.toURI())) {
      return followUrl[url.toURI()]
    }

    def ret = false

    // compare host, port, path
    if (baseUrl.host == url.host 
        && baseUrl.port == url.port
        && baseUrl.path == url.path) 
    {

      // Get the query parameters
      def q = page.query

      if (q.n?.startsWith("${var.pmHome}.")
          && (!q.containsKey('action') || q.action == 'download')) 
      {
        def home, loc
        (home, loc) = q.n.split('\\.')

        if (pmExclusions.any { it == loc }) {
          ret = false
        } else { 
          ret = true
        }
      }
    }

    if (!ret && followable) {
      ret = isFollowableConf(page, followable)
    }

    followUrl[url.toURI()] = ret

    return ret
  }


  /**********************************************************************/
  /**
   * Build a new url. Follow server redirects to get the correct url.
   */

  public URL buildUrlRedirect(URL url) {
    return url
  }

}
