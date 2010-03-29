package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class facteam extends Config {

  def private static log = Logger.getInstance(facteam.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public facteam() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/facteam/')

    followable = [
      ['^http://www.lib.umd.edu/groups/facteam/.*',                     // include
        '^http://www.lib.umd.edu/groups/facteam/formertooltrainingworkshops.html',
        '^http://www.lib.umd.edu/groups/facteam/formermeetingschedule.html',
        '^http://www.lib.umd.edu/groups/facteam/formermeetingminutes.html',
      ],
    ]
  }
}
