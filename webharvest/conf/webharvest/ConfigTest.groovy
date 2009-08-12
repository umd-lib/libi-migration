package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node

class ConfigTest extends Config {

  def private log = Logger.getInstance(ConfigTest.getName());


  /**
   * Constructor.
   */

  public ConfigTest() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/JUNK/ben/webharvest/')

    followable = [
      'abc',
      'xyz',
      ['^http://www.lib.umd.edu/JUNK/ben/webharvest/.*',   // incl
      ]
    ]
  }
}
