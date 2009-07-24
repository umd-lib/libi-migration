package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class irst extends Config {

  def private static log = Logger.getInstance(irst.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public irst() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/irst/')

    followable = [
      ['^http://www.lib.umd.edu/groups/irst/.*',             // incl
        '^http://www.lib.umd.edu/groups/irst/dateline/.*'    //   excl
      ]
    ]
  }


  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String getTitle (Page page, Node doc, Node body) {

    if (page.download) {
      return new File(URLDecoder.decode(page.url.path,'UTF-8')).name
    }

    def l 
    def title

    // the first h2
    l = body.selectNodes('//h2/font|//h2');
    if (l.size() > 0) {
      title = l[0].text
      l[0].detach()
      return title
    }

    // the first h1
    l = body.selectNodes('//h1');
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

}
