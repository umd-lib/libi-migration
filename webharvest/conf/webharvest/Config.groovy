package webharvest

import java.text.DateFormat

import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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
import org.dom4j.CharacterData
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

import org.apache.http.impl.cookie.DateUtils

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
  def urlSaved = [] // pages saved for output

  def cookies = [:]     // cookies need for authentication
  def authHeaders = [:] // other headers needed for authentication

  def followUrl = [:]  // cache of urls checked for following

  def hb = null        // Hibernate session

  def private log = Logger.getInstance(Config.getName());

  def var = [:]        // command-line variables

  def count = [:]      // statistics

  def downloaddir = null // download directory

  def buildUrls = [:]  // cache of normalized (buildUrl) urls

  def ctypes = [:]     // cache of content types per url
                       // use toURI() for the key because the query part
                       // will be ignored otherwise

  def followable = null // tree (include/exclude) of regexes

  def sidebarSelection = "//table//table/tbody/tr/td[h4]|//table//table/tbody/tr/td[//*[@class='leftcol_heading' or @class='leftcol_text']]"  // re for sidebar selection
  def sidebar = null       // list of url (string) to keep sidebar
  def sidebarSaved = null  // a saved sidebar, not included in the body


  def badlinks = [:]  // report bad links

  def outbadlinks = null  // bad links report file


  /**********************************************************************/
  /**
   * Constructor.
   */

  public Config() {
    count.nodes = 0

    HttpURLConnection.followRedirects = false
  }


  /**********************************************************************/
  /**
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {}


  /**********************************************************************/
  /**
   * Add a bad link to the report.
   */

  public void addBadLink(String from, String to) {
    if (! badlinks[from]) {
      badlinks[from] = []
    }

    badlinks[from] << to
  }


  /**********************************************************************/
  /**
   * Add url to the todo list.
   */

  public void addTodo(String surl) {
    def url = new URL(surl)
    def page = new Page(url:url, depth:0)
    page.ctype = getContentType(page) 
    urlTodo << page
  }


  /**********************************************************************/
  /**
   * Build a new url. Perform normalization.
   */

  public URL buildUrl(URL baseUrl, String rel) {
    def orig = null
    try {
      orig = new URL(baseUrl, rel);
    }
    catch (Exception e) {
      return new URL("http://invalid.url/${rel}")
    }

    // check the cache
    if (buildUrls.containsKey(orig)) return buildUrls[orig]

    // fixups
    if (var.urlFixups) {
      def preFixup = rel
      var.urlFixups.each { regex, replacement ->
        rel = rel.replaceAll(regex, replacement)
      }
      if (preFixup != rel) {
        log.debug("Url fixup ${preFixup} -> ${rel}")
      }
    }

    def url = new URL(baseUrl, rel);

    url = buildUrlRedirect(url)

    if (url.path.endsWith('/index.html')) {
      url = new URL(url, './')
    }

    // normalization: encode each part of the path
    url.set(url.protocol, 
            url.host, 
            url.port, 
            url.path.replaceAll('//','/').split('/').collect { URLEncoder.encode(URLDecoder.decode(it,'UTF-8')) }.join('/') + (url.path.endsWith('/') ? '/' : ''),
            url.ref?.replaceAll(' ','+'))

    // cache the result
    buildUrls[orig] = url

    return url
  }


  /**********************************************************************/
  /**
   * Build a new url. Follow server redirects to get the correct url.
   */

  public URL buildUrlRedirect(URL url) {
    log.debug("checking for url redirects: ${url}")

    def done = false

    // loop as long as we continue to get a redirect
    while (!done && isFollowable(new Page(url:url))) {
      done = true

      // make an http HEAD call
      def h = url.openConnection()
      if (h instanceof HttpURLConnection) {
        h.requestMethod = 'HEAD'
        h.connectTimeout = 10000

        try {
          h.connect()
        }
        catch (SocketTimeoutException e) {
          log.warn("Timeout following url: ${url}")
          return url
        }

        if (h.responseCode in (300..399) && h.headerFields.Location) {
          // we got a redirect
          done = false

          // get the new url
          def loc = h.headerFields.Location.toString()
          loc = loc.substring(1, loc.length()-1)
          def redirect = new URL(loc)

          log.debug("${url} redirected to ${redirect}")

          url = redirect

        } else {
          // no redirect; cache the content type
          if (! (h.responseCode in (400..499)) && h.headerFields.'Content-Type') {
            def ctype = null
            (ctype) = h.headerFields.'Content-Type'.toString().split(';')
            ctype = ctype.replace('[','').replace(']','')
            ctypes[url.toURI()] = ctype
          }
        }
      }
    }

    return url
  }


  /**********************************************************************/
  /**
   * Build the tree of nodes.
   */

  public List buildTree(List done) {
    def tree = ['nodes']
    
    // Add missing folders
    if (var.adddirs) {
      buildTreeAddDirs(done)
    }

    // Sort by url, leave the first entry in place
    done = done[0..0] + done.tail().sort() { it.surl }

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
   * Add missing directory nodes.
   */

  public void buildTreeAddDirs(List done) {
    done.clone().each { buildTreeAddDirs(done, it, '.') }
  }


  /**********************************************************************/
  /**
   * Add missing directory nodes.
   */

  public void buildTreeAddDirs(List done, Page n, String rel) {
    if (n.download) return

    def pu = new URL(n.url, rel)
    if (pu.path in ['..','/'] || pu == n.url) return

    def p = new Page(url:pu, ctype:'text/html')
    if (! (p in done) && isFollowable(p)) {
      log.info("  add missing directory: ${p}")

      // Add the new Page
      p.title = pu.path.split('/')[-1]

      def doc = DocumentHelper.createDocument()
      doc.addElement('html').addElement('body')

      def body = DocumentHelper.createDocument()
      body.addElement('body')

      savePage(p, doc, body)

      // Try the next directory up
      buildTreeAddDirs(done, p, '..')
    }
  }


  /**********************************************************************/
  /**
   * Cleanup the doc after all else and it has been converted to a string.
   */

  public String cleanupBodyPost (String body) {
    def origbody = body
    def newbody = null
    def changed = true

    while (changed) {
      newbody = body.replace('&amp;amp;','&amp;').replace('&amp;apos;','&apos;').replace('&amp;quot;','&quot;')

      changed = (newbody != body)

      body = newbody
    }

    if (log.isDebugEnabled() && origbody != newbody) {
      log.debug("Post cleaned up body:\n${newbody}")
    }

    return newbody
  }


  /**********************************************************************/
  /**
   * Cleanup the doc delivered by HtmlCleaner
   */

  public void cleanupDoc (Object node) {
    if (node instanceof CharacterData) {
      // This is a stub for future work
      //log.debug("text: ${node.text}")
    } else {
      //node.nodeIterator().each { cleanupDoc(it) }
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

    // the paths were the same until at least one of them ran out

    if (! (xp.size() != 0 || yp.size() != 0)) {
      // the paths are identical
      log.error("x=${x}")
      log.error("y=${y}")
      return 0
    }

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

  public Document extractBody(Page page, Document doc) {
    def body = DocumentHelper.createDocument()

    def l 
    def div

    // first, try to recognize some patterns
    l = doc.selectNodes("/html/body/center[//h1]|//h1|//h2|//span[@class='breadcrumbs']|//p[@class='menu_text'")
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


  /**********************************************************************/
  /**
   * Is there a sidebar we can tack onto the end?
   */

  public void extractBodySidebar(Page page, Document doc, Document body) {

    sidebarSaved = null

    def l = doc.selectNodes(sidebarSelection)
    if (l.size() > 0) {
      if (sidebar == null || page.surl in sidebar) {
        def div = l[0].clone()
        div.name = 'div'
        div.attributes.each { it.detach() }
        body.rootElement.add(div)
      } else {
        sidebarSaved = l[0].clone()
      }
    }
  }


  /**********************************************************************/
  /**
   * Get the download file name.
   */

  public File getDownload(Page page) {
    def x = new File(page.url.file)
    def name = x.name.replace('%20','_')

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
    def match = '//a/@href|//img/@src'
    def links = body.selectNodes(match)

    if (sidebarSaved) {
      links += df.createDocument(sidebarSaved).selectNodes(match)
    }

    return links;
  }


  /**********************************************************************/
  /**
   * Get the charset of a page in handleHtml
   */

  public String getCharset(Page page, groovyx.net.http.HttpResponseDecorator$HeadersDecorator headers) {

    def charset = 'WINDOWS-1252'  // default value

    if (headers['Content-Type']) {
      headers.'Content-Type'.split(';').each {
        if (it.contains('=')) {
          def (k, v) = it.trim().split('=')
          if (k == 'charset') {
             charset = v
          }
        }
      }
    }

    return charset
  }


  /**********************************************************************/
  /**
   * urlPlusToSpace
   */

  public URL urlPlusToSpace(URL uin) {
    def uout = new URL(uin, uin.path.replace('+','%20'))
    uout.query = uin.query
    uout.ref = uin.ref

    return uout
  }


  /**********************************************************************/
  /**
   * Get the content-type of one url.
   */

  public String getContentType(Page page) {

    def ctype = null

    // check the cache
    if (ctypes.containsKey(page.url.toURI())) {
      log.debug('Content-Type cache hit')
      ctype = ctypes[page.url.toURI()]

    } else {

      def http = new HTTPBuilder(urlPlusToSpace(page.url))

      // make an http HEAD call
      http.request(HEAD) { req ->
        headers.'User-Agent' = 'Libi WebHarvest'
  
        // authentication cookies/headers
        if (! cookies.isEmpty()) {
          headers.Cookie = cookies.collect{"${it.key}=${it.value}"}.join('; ')
        }

        authHeaders.each {
          headers[it.key] = it.value
        }

        response.success = { resp, reader ->
          // resp.statusLine
          // resp.statusLine.statusCode
          // resp.headers.each {println it}

          if (resp.headers['Content-Type']) {
            (ctype) = resp.headers.'Content-Type'.split(';')
          }

          // reader
        }
  
        // not round
        response.'404' = { resp ->  
          log.warn("Error 404: not found: ${page.url} (from=${page.fromPage.url})")
          addBadLink(page.fromPage.surl, page.surl)
          ctype = 'unknown/notfound'
        }
      }

      // add to the cache
      ctypes[page.url.toURI()] = ctype
    }

    log.debug("Content-Type: ${ctype}")

    return ctype;
  }


  /**********************************************************************/
  /**
   * Get creation time of the doc.
   */

  static final pattern = ~/last *(modified|revised): *(\w+ +\d{1,2}, +\d{4})/

  public String getCreated (Page page, Node doc, Node body) {


    if (page.created) {
      return page.created
    }

    for (n in doc.selectNodes("//*").reverse()) {
      def m = pattern.matcher(n.text.toLowerCase())
      if (m) {
        try {
          def date = m[0][2]
          Date d = DateFormat.getDateInstance().parse(date)
          return d.format('yyyy-MM-dd HH:mm:ss')
        }
        catch (Exception e) {}
      }
    }

    return (new Date()).format('yyyy-MM-dd HH:mm:ss')
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

    if (page.title) {
      return page.title
    }

    if (page.download) {
      return new File(URLDecoder.decode(page.url.path,'UTF-8')).name
    }

    def l 
    def title

    // the first h1
    l = body.selectNodes('//h1/font|//h1');
    if (l.size() > 0) {
      title = l[0].text
      l[0].detach()
      return title
    }

    // title element
    l = doc.selectNodes('/html/head/title');
    if (l.size() > 0) {
      return l[0].text.trim().replaceAll(', UM Libraries$','')
    }

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

    def done = false
  
    http.request(GET, BINARY) { req ->
      headers.'User-Agent' = 'Libi WebHarvest'
 
      // authentication cookies/headers
      if (! cookies.isEmpty()) {
        headers.Cookie = cookies.collect{"${it.key}=${it.value}"}.join('; ')
      }
      authHeaders.each {
        headers[it.key] = it.value
      }

      response.success = { resp, reader ->
        // resp.statusLine
        // resp.statusLine.statusCode
        // resp.headers.each {println it}
 
        // Get the charset
        def charset = getCharset(page, resp.headers)
        log.debug("handleHtml charset: ${charset}")

        // Get Last-Modified
        if (resp.headers['Last-Modified']) {
          try {
            Date d = DateUtils.parseDate(resp.headers.'Last-Modified');
            page.created = d.format('yyyy-MM-dd HH:mm:ss').toString()
          }
          catch (Exception e) {}
        }

        doc = cleaner.clean(reader, charset)
      }
  
      // called only for a 401 (access denied) status code:
      response.'404' = { resp ->  
        log.warn("Error 404: not found: ${page.url}")
        addBadLink(page.fromPage.surl, page.surl)
        done = true
      }
    }

    if (done) return

    if (log.isDebugEnabled()) {
      def serializer = new PrettyXmlSerializer(props)
      def sw = new StringWriter()
      serializer.serialize(doc, sw)
      log.debug("Cleaned page:\n" + sw.getBuffer().toString())
    }

    // Convert to HtmlCleaner to org.w3c.dom.Document
    def dom = ds.createDOM(doc)

    // Convert org.ww3c.dom.Document to org.dom4j.Document
    doc = dr.read(dom)
    
    if (log.isDebugEnabled()) {
      log.debug("dom4j doc:\n${doc.getRootElement().asXML()}")
    }

    // Check for the Multiple Choices page
    def title = doc.selectSingleNode('/html/head/title')
    if (title?.text == '300 Multiple Choices') {
      log.warn("Error: multiple choices: ${page.surl} (from=${page.fromPage})")
      addBadLink(page.fromPage.surl, page.surl)
      return
    }

    // Cleanup the doc
    cleanupDoc(doc)

    // Extract the content body from the html
    def body = extractBody(page, doc)

    if (log.isDebugEnabled()) {
      log.debug("Extracted body\n${body.getRootElement().asXML()}")
    }

    // Process each link
    getLinks(body).each { node ->
      try {
        def link = new Page(url:buildUrl(page.url, node.text), fromPage:page, depth:page.depth+1)

        log.debug("checking link: ${link.url}")

        def follow = isFollowable(link)
        log.debug("isFollowable: ${follow}")

        if (!follow) {
          // turn the link into an absolute url
          node.text = link.url.toString()

        } else {
          // change the link to internal
          node.text = '[[' + getUnique(link) + ']]'
          if (link.url.ref) node.text += '#' + link.url.ref

          // check if link should be queued
          if (link in urlDone) {
            log.debug("link already done")
            
          } else if (link in urlTodo) {
            log.debug("link already in todo list")

          } else {
            log.debug("adding link into todo list")

            try {
              link.ctype = getContentType(link)
              urlTodo << link
            }
            catch (Throwable t) {
              log.error("Error getting Content-Type for link ${link.url}\n" +
                        ErrorHandling.getStackTrace(t))
            }            
          }
        }
      }
      catch (Exception e) {
        log.error("Error handling link ${node.text}")
        addBadLink(page.surl, node.text)
      }
    }

    savePage(page, doc, body)
  }


  /**********************************************************************/
  /**
   * Handle a file for download.
   */

  public void handleFile(Page page) {

    page.download = getDownload(page)

    log.debug("Download file name: ${page.download}")

    def http = new HTTPBuilder(urlPlusToSpace(page.url))
  
    if (!var.nofiles || var.nofiles != 'true') {
      http.request(GET, BINARY) { req ->
        headers.'User-Agent' = 'Libi WebHarvest'
 
        // authentication cookies
        if (! cookies.isEmpty()) {
          headers.Cookie = cookies.collect{"${it.key}=${it.value}"}.join('; ')
        }

        authHeaders.each {
          headers[it.key] = it.value
        }

        response.success = { resp, reader ->

          log.debug("Downloading to ${page.download}")
          
          page.download << reader
        }
  
        // called only for a 401 (access denied) status code:
        response.'404' = { resp ->  
          log.warn("Error 404: not found: ${page.url}")
        }
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
    // check for depth limit on this page
    if (page.ctype == 'text/html' && var.depth && page.depth > (var.depth as int)) {
      log.debug("  not handling url, depth=${page.depth}: ${page.url}")
      return
    }

    log.info("  handling url: ${page.url}")
    log.debug("   ctype=${page.ctype}, depth=${page.depth}, from=${page.fromPage?.surl}")

    if (page.ctype == 'text/html') {
      urlDone << page
      handleHtml(page);

    } else if (!page.ctype.startsWith('unknown/')) {
      urlDone << page
      handleFile(page)
    }
  }


  /**********************************************************************/
  /**
   * Begin the fetch cycle
   */

  public void harvest() {
    log.info('Beginning harvest...')

    // Perform any necessary authentication
    authenticate()

    // Add the base url to the pipeline
    if (var.baseUrl) {
      baseUrl = new URL(var.baseUrl)
    }

    def basePage = new Page(url:baseUrl, depth:0)
    basePage.ctype = getContentType(basePage) 
    urlTodo << basePage

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

    def tree = buildTree(urlSaved)

    // Output nodes
    log.info('Writing out nodes')
    outputNodes(tree)

    // Bad link report
    badlinks.each { k,v ->
      outbadlinks.println(k)
      v.each {
        outbadlinks.println(" -> ${it}")
      }
      outbadlinks.println()
    }
    outbadlinks.close()
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed
   */

  public boolean isFollowable(Page page) {
    if (page.url.query != null) return false;

    if (followable) {
      return isFollowableConf(page, followable)
    }

    if (baseUrl.toString().endsWith('/')) {
      // harvesting a directory, include all files, subdirs
      def s = baseUrl.toString()
      s = s.substring(0, s.length()-1)
      followable = ["^${s}.*"]

      return isFollowableConf(page, followable)
    }

    return page.surl.startsWith(baseUrl.toString())
  }


  /**********************************************************************/
  /**
   * Determine if a url should be followed based on a configured list
   * of regexes which indicate include or exclude.
   */

  public boolean isFollowableConf(Page page, List f, include = true) {

    // default return value
    def ret = !include

    // iterate over the regexes
    f.each { match ->
      // get the regex and exception list
      def regex =      (match instanceof List ? match.head() : match)
      def exceptions = (match instanceof List ? match.tail() : null)

      // check for a match
      if (page.url.toString() ==~ regex) {
        // set the return value
        ret = (!exceptions ? include : isFollowableConf(page, exceptions, !include))

        // break out of the loop
        return
      }
    }

    return ret
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
      // data
      out.data() {
        // body
        if (var.bodyxml) {
          // output body as xml
          out.body([xml:'true']) {
            def bodyx = DocumentHelper.parseText(page.body)

            outputBody(bodyx.getRootElement())
          }
        } else {
          // output body as text
          out.body(page.body)
        }

        // attachments
        if (page.download && page.type != 'image') {
          out.attachment(unique:"attachment-${page.uniq}",
                         url:page.download.name)
        }
      }

      // children
      if (hasChildren) tree.each { outputNodes(it) }
    }
  }
 

  /**********************************************************************/
  /**
   * Output the body as XML.
   */

  public void outputBody(Object node) {
    if (node instanceof CharacterData) {
      out.yield(node.text)
    } else {
      def attrs = [:]
      node.attributes.each { attrs[it.name] = it.value }

      out."${node.name}"(attrs) {
        node.nodeIterator().each { outputBody(it) }
      }
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
    clone.body    = cleanupBodyPost(body.getRootElement().asXML())

    log.debug("Adding ${clone} to hibernate object store")

    hb.save(clone)

    urlSaved << page
  }


  /**********************************************************************/
  /**
   * Handle one url
   */

  public String toString() {
    return 'foobar'
  }
}

