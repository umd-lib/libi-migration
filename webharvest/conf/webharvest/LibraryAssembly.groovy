package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class LibraryAssembly extends Config {

  def private static log = Logger.getInstance(LibraryAssembly.getName());


  /**********************************************************************/
  /**
   * Constructor.
   */

  public LibraryAssembly() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/groups/la/')
  }

}
