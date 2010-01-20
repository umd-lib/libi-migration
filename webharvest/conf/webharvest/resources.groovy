package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class resources extends usmai {

  def private static log = Logger.getInstance(resources.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public resources() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/resources/ADHOCindex.html')

    followable = [
      ['^http://usmai.umd.edu/resources/.*',  // include
      ],
    ]

  }
}
