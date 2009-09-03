package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node
import org.dom4j.DocumentHelper


class tsdpp extends TSD {

  def private static log = Logger.getInstance(tsdpp.getName());

  /**********************************************************************/
  /**
   * Constructor.
   */

  public tsdpp() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/TSD/tsd_policies2.html')

    followable = [
      ['^http://www.lib.umd.edu/TSD.*',                     // include
        'http://www.lib.umd.edu/TSD/web_procedure.html',    //   exclude
        'http://www.lib.umd.edu/TSD/tsd_timesheets.html',   //   exclude
        'http://www.lib.umd.edu/TSD/forms%26flyers.html',   //   exclude
        'http://www.lib.umd.edu/TSD/tsd.html',              //   exclude
        'http://www.lib.umd.edu/TSD/TEST.*',                //   exclude
        'http://www.lib.umd.edu/TSD/checklist_form.html',   //   exclude
        'http://www.lib.umd.edu/TSD/cpaleph_authorization.html', //exclude
      ]
    ]

    // set the depth limit
    if (!var.depth) {
      var.depth = 2
    }

    // include the sidebar for these only
    sidebar = ['http://www.lib.umd.edu/TSD/tsd_policies2.html']
  }
}
