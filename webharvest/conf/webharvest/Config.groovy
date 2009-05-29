package webharvest

import groovy.xml.MarkupBuilder

import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer

import org.apache.log4j.Logger;

import groovyx.net.http.HTTPBuilder

import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.HEAD
import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.TEXT

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.InvalidXPathException;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.XPath;

import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentInputSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;


class Config {
  def baseUrl = new URL('http://www.lib.umd.edu/JUNK/ben/webharvest/')

  def cleaner = new HtmlCleaner()     // html cleanup
  def props = cleaner.getProperties()

  def df = new DocumentFactory()      // dom4j utilities
  def dr = new DOMReader(df)
  def ds = new DomSerializer(props)

  def outfile = null  // output file
  def out = null      // output MarkupBuilder for xml creation

  def urlDone = []  // pages already processed
  def urlTodo = []  // pages waiting to be processed

  def cookies = [:] // cookies need for authentication

  def followUrl = [:]  // cache of urls checked for following

  def hb = null        // Hibernate session

  def private log = Logger.getInstance(Config.getName());


  /**********************************************************************/
  /**
   * Constructor.
   */

  public Config() {
  }


  /**********************************************************************/
  /**
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {}


  /**********************************************************************/
  /**
   * Build a new url. Perform normalization.
   */

  public URL buildUrl(URL baseUrl, String rel) {
    def url = new URL(baseUrl, rel);

    if (url.path.endsWith('/index.html')) {
      url.path = url.path.substring(0, url.path.length()-10)
    }

    return url
  }


  /**********************************************************************/
  /**
   * Extract the content body from the html.  This is not necessarily
   * the html body element.
   */

  public Node extractBody(Document doc) {
    def l = doc.selectNodes('/html/body');

    def n = (l.size() < 1 ? doc : l[0])

    n.name = 'div'

    return n
  }


  /**********************************************************************/
  /**
   * Extract all links from the body.  Determine elsewhere if they are
   * followable.
   */

  public List getLinks(Node body) {
    return body.selectNodes('//a/@href|//img/@src')
  }


  /**********************************************************************/
  /**
   * Get the content-type of one url.
   */

  public String getContentType(URL url) {

    def http = new HTTPBuilder(url)

    def ctype = null

    // make an http HEAD call
    http.request(HEAD) { req ->
      headers.'User-Agent' = 'Libi WebHarvest'
  
      response.success = { resp, reader ->
        // resp.statusLine
        // resp.statusLine.statusCode
        // resp.headers.each {println it}

        if (resp.headers.'Content-Type') {
          (ctype) = resp.headers.'Content-Type'.split(';')
        }

        // reader
      }
  
      // called only for a 401 (access denied) status code:
      response.'404' = { resp ->  
        println 'Not found'
      }
    }

    
    log.debug("Content-Type: ${ctype}")

    return ctype;
  }


  /**********************************************************************/
  /**
   * Get creation time of the doc.
   */

  public String getCreated (Page page, Node doc, Node body) {
    return '2000-01-01 00:00:00'
  }


  /**********************************************************************/
  /**
   * Get name of the author.
   */

  public String getName (Page page, Node doc, Node body) {
    return 'anonymous'
  }


  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String getTitle (Page page, Node doc, Node body) {
    return page.url.toString()
  }


  /**********************************************************************/
  /**
   * Get node type.
   */

  public String getType (Page page, Node doc, Node body) {
    return 'folder'
  }


  /**********************************************************************/
  /**
   * Get unique id of the node.
   */

  public String getUnique (Page page, Node doc, Node body) {
    return page.url.toString()
  }


  /**********************************************************************/
  /**
   * Handle one text/html page.
   */

  public void handleHtml(Page page) {

    // Get the cleaned up html
    def doc = null

    def http = new HTTPBuilder(page.url)
  
    http.request(GET, BINARY) { req ->
      headers.'User-Agent' = 'Libi WebHarvest'
 
      // authentication cookies
      if (! cookies.isEmpty()) {
        headers.Cookie = cookies.collect{"${it.key}=${it.value}"}.join('; ')
      }

      response.success = { resp, reader ->
        // resp.statusLine
        // resp.statusLine.statusCode
        // resp.headers.each {println it}

        doc = cleaner.clean(reader)
      }
  
      // called only for a 401 (access denied) status code:
      response.'404' = { resp ->  
        println 'Not found'
      }
    }

    if (log.isDebugEnabled()) {
      def serializer = new PrettyXmlSerializer(props)
      def sw = new StringWriter()
      serializer.serialize(doc, sw)
      log.debug("Cleaned page:\n" + sw.getBuffer().toString())
    }

    // Convert to dom4j document
    doc = dr.read(ds.createDOM(doc))
      
    // Extract the content body from the html
    def body = extractBody(doc)

    // Process each link
    getLinks(body).each { node ->
      def link = new Page(url:buildUrl(page.url, node.text))

      log.debug("checking link: ${link.url}")

      if (!(urlDone.any {it.surl == link.surl}) && 
          !(urlTodo.any {it.surl == link.surl}) &&
          isFollowable(link)) {
        
        link.ctype = getContentType(link.url)
        urlTodo << link
      }
    }

    // Make a copy of page, add data, save the page
    def clone = page.clone()

    clone.created = getCreated(page, doc, body)
    clone.name    = getName(page, doc, body)
    clone.title   = getTitle(page, doc, body)
    clone.type    = getType(page, doc, body)
    clone.uniq    = getUnique(page, doc, body) 
    clone.body    = body.asXML()

    hb.save(clone)
  }


  /**********************************************************************/
  /**
   * Handle one page.
   */

  public void handlePage(Page page) {
    log.info("handling url: ${page.url}")

    if (page.ctype == 'text/html') {
      handleHtml(page);
    }

    outfile.flush()
  }


  /**********************************************************************/
  /**
   * Begin the fetch cycle
   */

  public void harvest() {
    log.info('Beginning harvest...')

    // Add the base url to the pipeline
    urlTodo << new Page(url:baseUrl, ctype:getContentType(baseUrl))

    // Perform any necessary authentication
    authenticate()

    // Iterate over the urls to process
    while (! urlTodo.isEmpty()) {
      def page = urlTodo.remove(0)

      if (page.ctype == 'text/html') {
        urlDone << page

        handlePage(page) 
      }
    }

    // Read the saved pages back in
    urlDone.each { page ->
      // load the page with body back in
      page = hb.get(Page, page.surl)

      // output a node
      out.node(created: page.created,
               name:    page.name,
               title:   page.title,
               type:    page.type,
               unique:  page.uniq)
        {
        out.data() {
          out.body(page.body)
        }
      }
    }

  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    return page.surl.startsWith(baseUrl.toString())
  }


  /**********************************************************************/
  /**
   * Handle one url
   */

  public String toString() {
    return 'foobar'
  }
}

