package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class diversity extends Config {

  def private static log = Logger.getInstance(diversity.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public diversity() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/diversity/')

    followable = [
      ['^http://www.lib.umd.edu/groups/diversity/.*',                     // include
      ],
    ]
  }
}
