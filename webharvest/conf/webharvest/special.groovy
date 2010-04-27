package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class special extends Config {

  def private static log = Logger.getInstance(special.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public special() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/CLMD/SpecColl/')

    followable = [
      '^http://www.lib.umd.edu/CLMD/SpecColl/Meeting.Roles.2009-2010.doc',
      '^http://www.lib.umd.edu/CLMD/SpecColl/rotationlist.doc',
      '^http://www.lib.umd.edu/CLMD/SpecColl/collaborative/minutes.*',
      '^http://www.lib.umd.edu/CLMD/SpecColl/collaborative/reports.*',
    ]
  }

  /**********************************************************************/
  /**
   * Build the tree of nodes.
   */

  public List buildTree(List done) {

    done.each { page ->
      if (page.surl == 'http://www.lib.umd.edu/CLMD/SpecColl/collaborative/minutes.html') {

        // Make the minutes page look like the parent folder of all minutes

        def hbpage = hb.get(Page, page.surl)

        page.url = new URL('http://www.lib.umd.edu/CLMD/SpecColl/collaborative/minutes/')
        page.title = 'Minutes Archive'

        def hbpageNew = hbpage.clone()
        hbpageNew.body = hbpage.body 

        hbpageNew.url = page.url
        hbpageNew.title = page.title

        hb.save(hbpageNew)
      }
    }

    return super.buildTree(done)
  }

}
