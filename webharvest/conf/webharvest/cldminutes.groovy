package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class cldminutes extends usmai {

  def private static log = Logger.getInstance(cldminutes.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public cldminutes() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/cld/minutesindex.html')

    followable = [
      ['http://usmai.umd.edu/cld/.*',
        'http://usmai.umd.edu/cld/index.html',
        'http://usmai.umd.edu/cld/',
      ],
    ]

  }
}
