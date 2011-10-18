package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class itdat extends itd {

  def private static log = Logger.getInstance(itdat.name);

  /**********************************************************************/
  /**
   * Constructor.
   */

  public itdat() {
    super()

    baseUrl = new URL('http://www.itd.umd.edu/ITD/aleph/alephteam.html')

    addTodo('http://www.itd.umd.edu/ITD/aleph/status.html')
    addTodo('http://www.itd.umd.edu/ITD/aleph/NAAUG04_pics.html')
    addTodo('http://www.itd.umd.edu/ITD/aleph/NAAUG05_pics.html')
    addTodo('http://www.itd.umd.edu/ITD/aleph/marylandday2005.html')

    followable = [
      '^http://www.itd.umd.edu/ITD/aleph.*',        
      '^http://www.itd.umd.edu/LIMS3/statusrep.*',  
      '^http://www.itd.umd.edu/IMG.*',    
      '^http://www.itd.umd.edu/ITD/users.*',    
    ]


  }

}
