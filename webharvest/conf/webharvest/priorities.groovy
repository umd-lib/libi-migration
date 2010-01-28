package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class priorities extends usmai {

  def private static log = Logger.getInstance(priorities.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public priorities() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/cc/prioritiesindex.html')

    followable = [
      ['http://usmai.umd.edu/pass/.*'],
    ]

  }

}
