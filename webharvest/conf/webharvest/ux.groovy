package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class ux extends Config {

  def private static log = Logger.getInstance(ux.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public ux() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/itd/ux/')

    followable = [
      ['^http://www.lib.umd.edu/itd/ux/.*',                     // include
      ],
      ['^http://www.lib.umd.edu/groups/redesign/.*',                     // include
      ],
    ]
  }
}
