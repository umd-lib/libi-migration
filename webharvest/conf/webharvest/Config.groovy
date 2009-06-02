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
import org.dom4j.DocumentHelper
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

import edu.umd.lims.util.ErrorHandling


class Config {
  def baseUrl = new URL('http://www.lib.umd.edu/JUNK/ben/webharvest/')

  def cleaner = new HtmlCleaner()     // html cleanup
  def props = cleaner.getProperties()

  def df = new DocumentFactory()      // dom4j utilities
  def dr = new DOMReader(df)
  def ds = new DomSerializer(props)

  def out = null      // output MarkupBuilder for xml creation

  def urlDone = []  // pages already processed
  def urlTodo = []  // pages waiting to be processed

  def cookies = [:] // cookies need for authentication

  def followUrl = [:]  // cache of urls checked for following

  def hb = null        // Hibernate session

  def private log = Logger.getInstance(Config.getName());

  def var = [:]        // command-line variables

  def count = [:]      // statistics

  def downloaddir = null // download directory


  /**********************************************************************/
  /**
   * Constructor.
   */

  public Config() {
    count.nodes = 0
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
      url = new URL(url, './')
    }

    return url
  }


  /**********************************************************************/
  /**
   * Build the tree of nodes.
   */

  public List buildTree(List done) {
    def tree = ['nodes']

    // Insert each Page into proper place
    done.each { buildTree(tree, it); log.debug("Tree: ${tree}") }

    return tree
  }


  /**********************************************************************/
  /**
   * Insert a Page into the tree of nodes.
   */

  public void buildTree (List tree, Page page) {

    log.debug("buildTree inserting ${page} into ${tree}")

    def done = false

    // test each of the existing nodes
    for (def i=1; i < tree.size() && !done; i++) {
      def c = compare(page, (tree[i] instanceof Page ? tree[i] : tree[i][0]))
      switch (c) {
        case -1:
          // page is a parent of the tested page
          tree[i] = [page, tree[i]]
          done = true
          break

        case 0:
          // neither parent nor child, do nothing
          break

        case 1:
          // page is a child of the tested page
          if (tree[i] instanceof Page) {
            tree[i] = [tree[i], page]
          } else { // List
            buildTree(tree[i], page)
          }
          done = true
          break
      }
    }

    if (!done) { 
      tree << page 
    }
  }


  /**********************************************************************/
  /**
   * Compare urls for parent, child relationship
   *
   * @return -1 if x is parent of y, 0 if no relationship, 1 if x is child of y
   */

  public int compare (Page x, Page y) {
    // compare host and port
    if (x.url.host != y.url.host) { return 0 }
    if (x.url.port != y.url.port) { return 0 }

    // compare the paths
    def xp = x.url.path.split('/') as List
    def yp = y.url.path.split('/') as List

    while (xp.size() > 0 && yp.size() > 0) {
      if (xp[0] != yp[0]) {
        return 0;
      }
      xp.remove(0)
      yp.remove(0)
    }

    // the paths were the same until one ran out first
    assert xp.size() != 0 || yp.size() != 0

    if (xp.size() == 0) {
      // x is parent of y
      return -1
    } else {
      // y is parent of x
      return 1
    }

  }


  /**********************************************************************/
  /**
   * Extract the content body from the html.  This is not necessarily
   * the html body element.
   */

  public Node extractBody(Document doc) {
    def body = DocumentHelper.createDocument()

    def l 
    def div

    // first h1 or h2 and siblings
    l = doc.selectNodes('//h1|//h2')
    if (l.size() > 0) {
      div = body.addElement('div')

      l[0].parent.elements().each { e ->
        def exclude = false

        // check for exclusions
        if (e.name == 'span' && e.attribute('class')?.value == 'breadcrumbs') {
          exclude = true
        }

        if (!exclude) {
          div.add(e.clone())
        }
      }

    } else {

      // the entire body
      l = doc.selectNodes('/html/body')
      body = l[0].clone()
      body.name = 'div'
    }

    // Is there a sidebar we can tack onto the end?
    l = doc.selectNodes('//table//table/tbody/tr/td[h4]')
    if (l.size() > 0) {
      div = l[0].clone()
      div.name = 'div'
      div.attributes.each { it.detach() }
      body.rootElement.add(div)
    }

    // Update header tags
    def static hmap = [h1:'h2',h2:'h3',h3:'h4',h5:'h4',h6:'h4']
    l = body.selectNodes('//h1|//h2|//h3|//h5|//h6')
    l.each { it.name = hmap[it.name] }

    return body
  }


  /**********************************************************************/
  /**
   * Get the download file name.
   */

  public File getDownload(Page page) {
    def x = new File(page.url.file)
    def name = x.name

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
        log.warn("Error 404: not found: ${url}")
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

    if (page.type == 'image') {
      return new File(page.url.path).name
    }

    def l 

    // title element
    l = doc.selectNodes('/html/head/title');
    if (l.size() > 0) return l[0].text

    // the first h1
    l = doc.selectNodes('//h1');
    if (l.size() > 0) return l[0].text

    // the path part of the url
    return page.url.path.split('/').join(' ').trim()
  }


  /**********************************************************************/
  /**
   * Get node type.
   */

  public String getType (Page page, Node doc, Node body) {

    if (page.ctype.startsWith('image/')) {
      return 'image'
    } else {
      return 'folder'
    }
  }


  /**********************************************************************/
  /**
   * Get unique id of the node.
   */

  public String getUnique (Page page) {
    return page.urlNoAnchor.toString()
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
        log.warn("Error 404: not found: ${url}")
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

    if (log.isDebugEnabled()) {
      log.debug("Extracted body\n${body.getRootElement().asXML()}")
    }

    // Process each link
    getLinks(body).each { node ->
      def link = new Page(url:buildUrl(page.url, node.text))

      log.debug("checking link: ${link.url}")

      if (isFollowable(link)) {
        // change the link to internal
        node.text = '[[' + getUnique(link) + ']]'
        if (link.url.ref) node.text += '#' + link.url.ref

        // check if link should be queued
        if (!(link in urlDone) && 
            !(link in urlTodo)) {
        
          try {
            link.ctype = getContentType(link.url)
            urlTodo << link
          }
          catch (Throwable t) {
            log.error("Error getting Content-Type for link ${link.url}\n" +
                      ErrorHandling.getStackTrace(t))
          }            
        }
      }
    }

    savePage(page, doc, body)
  }


  /**********************************************************************/
  /**
   * Handle one image.
   */

  public void handleImage(Page page) {

    page.download = getDownload(page)

    log.debug("Download file name: ${page.download}")

    def http = new HTTPBuilder(page.url)
  
    http.request(GET, BINARY) { req ->
      headers.'User-Agent' = 'Libi WebHarvest'
 
      // authentication cookies
      if (! cookies.isEmpty()) {
        headers.Cookie = cookies.collect{"${it.key}=${it.value}"}.join('; ')
      }

      response.success = { resp, reader ->

        log.debug("Downloading to ${page.download}")

        page.download << reader
      }
  
      // called only for a 401 (access denied) status code:
      response.'404' = { resp ->  
        log.warn("Error 404: not found: ${url}")
      }
    }

    def doc = DocumentHelper.createDocument()
    doc.addElement('html').addElement('body')

    def body = DocumentHelper.createDocument()
    body.addElement('div')
    
    savePage(page, doc, body)
  }


  /**********************************************************************/
  /**
   * Handle one page.
   */

  public void handlePage(Page page) {
    log.info("handling url: ${page.url}")

    if (page.ctype == 'text/html') {
      urlDone << page
      handleHtml(page);

    } else if (page.ctype.startsWith('image/')) {
      urlDone << page
      handleImage(page)
    }
  }


  /**********************************************************************/
  /**
   * Begin the fetch cycle
   */

  public void harvest() {
    log.info('Beginning harvest...')

    // Add the base url to the pipeline
    if (var.baseUrl) {
      baseUrl = new URL(var.baseUrl)
    }
    urlTodo << new Page(url:baseUrl, ctype:getContentType(baseUrl))

    // Perform any necessary authentication
    authenticate()

    // Iterate over the urls to process
    while (! urlTodo.isEmpty()) {
      def page = urlTodo.remove(0)

      handlePage(page) 

      count.nodes++

      if (var.limit && count.nodes >= (var.limit as int)) {
        // reached our limit
        break;
      }
    }

    // Build the node tree
    log.info('Building node tree')

    def tree = buildTree(urlDone)

    // Output nodes
    log.info('Writing out nodes')
    outputNodes(tree)
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    if (page.url.query != null) return false;

    return page.surl.startsWith(baseUrl.toString())
  }


  /**********************************************************************/
  /**
   * Output nodes.  
   *
   * @param tree a List or a Page for output.  If a List then head must be a String or Page and tail is a list of children consisting of Pages or Lists
   */

  public void outputNodes(Object tree) {

    def head = ((tree instanceof List) ? tree.remove(0) : null)
    def hasChildren = (head != null)

    if (head instanceof String) {
      out."${head}"() { tree.each {outputNodes(it)} }
      return
    }

    // load the page with body back in
    def page = hb.get(Page, ((hasChildren) ? head.surl : tree.surl))
    
    // build the node attributes
    def attrs = [
      created: page.created,
      name:    page.name,
      title:   page.title,
      type:    page.type,
      unique:  page.uniq
    ]

    if (page.type == 'image') {
      attrs.url = page.download.name
    }

    // output a node
    out.node(attrs)
    {
      out.data() {
        out.body(page.body)
      }
      if (hasChildren) tree.each { outputNodes(it) }
    }
  }
 

  /**********************************************************************/
  /**
   * Save the Page to the hibernate object store.
   */

  public void savePage(Page page, Document doc, Node body) {
    // Make a copy of page, add data, save the page
    def clone = page.clone()

    clone.type    = getType(clone, doc, body)
    clone.created = getCreated(clone, doc, body)
    clone.name    = getName(clone, doc, body)
    clone.title   = getTitle(clone, doc, body)
    clone.uniq    = getUnique(clone) 
    clone.body    = body.getRootElement().asXML()

    log.debug("Adding ${clone} to hibernate object store")

    hb.save(clone)
  }


  /**********************************************************************/
  /**
   * Handle one url
   */

  public String toString() {
    return 'foobar'
  }
}

