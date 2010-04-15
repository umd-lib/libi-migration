package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class mdhc extends Config {

  def private static log = Logger.getInstance(mdhc.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public mdhc() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/CLMD/SpecColl/mdhist/')

    followable = [
      ['^http://www.lib.umd.edu/CLMD/SpecColl/mdhist/.*',        
      ],
    ]
  }
}
