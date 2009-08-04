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
   PmWiki - ITDStaff

   Authentication:
     Request: POST, authpw=<passwd>
     Response: Set-Cookie	PHPSESSID=51e0537c81b68481e7d77726ca9fc840; path=/
*/


class PmItd extends Config {

  def pmHome = 'ITDStaff'
  def pmPasswords = null

  def pmExclusions = ['RecentChanges']

  def private log = Logger.getInstance(PmItd.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public PmItd() {
    super()

    baseUrl = new URL("http://www.itd.umd.edu/pmwiki/pmwiki.php?n=${pmHome}.HomePage")

    followable = [
      ['^http://www.itd.umd.edu/pmwiki/itdextras/.*'             // incl
      ]
    ]

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
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {
    log.info("Providing authentication for PmWiki")

    def http = new HTTPBuilder(baseUrl)
    def postBody = [authpw:pmPasswords[pmHome]]

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

  public Document extractBody(Document doc) {
    def body = DocumentHelper.createDocument()

    def l = doc.selectNodes("//div[@id='wikitext']");

    l.each { body.add(it.clone()) }

    return body
  }


  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String getTitle (Page page, Node doc, Node body) {
    if (page.download) {
      return new File(URLDecoder.decode(page.url.path,'UTF-8')).name
    }

    return (page.query.n?.split('\\.'))[1]
  }


  /**********************************************************************/
  /**
   * Get unique id of the node.
   */

  public String getUnique (Page page) {

    if (page.query.n) {
      return page.query.n
    } else {
      return page.urlNoAnchor.toString()
    }
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    def url = page.url

    if (followUrl.containsKey(url)) {
      return followUrl[url]
    }

    def ret = false

    // compare host, port, path
    if (baseUrl.host == url.host 
        && baseUrl.port == url.port
        && baseUrl.path == url.path) 
    {

      // Get the query parameters
      def q = page.query

      if (q.size() == 1                       // Only one query param of 'n'
          && q.n?.startsWith("${pmHome}.")    // Is the corrent pmHome
          && q.n.indexOf('?') == -1)          // Doesn't contain extra params
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

    followUrl[url] = ret

    return ret
  }


}
