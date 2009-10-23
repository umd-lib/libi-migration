package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class cmt extends Config {

  def private static log = Logger.getInstance(cmt.name);



  /**********************************************************************/
  /**
   * Constructor.
   */

  public cmt() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/CLMD/CMT/cmthomepage.html')

    followable = [
      ['^http://www.lib.umd.edu/CLMD/CMT/.*',             // include
        'http://www.lib.umd.edu/CLMD/CMT/cmtminutes.html', // exclude
        'http://www.lib.umd.edu/CLMD/CMT/minutes/.*',      // exclude
      ]
    ]
  }

  public URL buildUrlRedirect(URL url) {
    def static r = [
      (new URL('http://www.lib.umd.edu/CLMD/CMT/cmthomepage.html/')) : new URL('http://www.lib.umd.edu/CLMD/CMT/cmthomepage.html'),
      (new URL('http://www.lib.umd.edu/CLMD/Staff/CMstaff.html/')) : new URL('http://www.lib.umd.edu/CLMD/Staff/CMstaff.html'),
    ]

    if (r.containsKey(url)) {
      url = r[url]
    }

    return super.buildUrlRedirect(url)
  }
}
