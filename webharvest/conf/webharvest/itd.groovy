package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class itd extends Config {

  def private static log = Logger.getInstance(itd.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public itd() {
    super()

    authenticate()
  }

  /**********************************************************************/
  /**
   * Authenticate once at the beginning of the cycle.
   */

  public void authenticate() {
    log.info("Providing authentication for ITD")

    // Read in auth password
    def binding = new Binding()
    def shell = new GroovyShell(binding)

    try {
      shell.evaluate(new File('conf/usmai.conf'))

      authHeaders.Authorization = binding.getVariable('auth')
    }
    catch (Throwable t) {
      t.printStackTrace()
      System.exit(1)
    }

    log.debug("Cookies: ${cookies}")
  }

}
