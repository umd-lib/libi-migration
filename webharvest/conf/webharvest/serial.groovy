package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class serial extends Config {

  def private static log = Logger.getInstance(serial.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public serial() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/CLMD/SERIALS/reports.html')

    followable = [
      ['^http://www.lib.umd.edu/CLMD/SERIALS/.*',        
      ],
      '^http://www.lib.umd.edu/ETC/dbsupport.html',  
      '^http://www.lib.umd.edu/ETC/FY...xls',  
    ]
  }
}
