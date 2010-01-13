package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class travel extends Config {

  def private static log = Logger.getInstance(travel.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public travel() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/PASD/LPO/TRAVEL/TravelFirst.html')

    followable = [
      ['^http://www.lib.umd.edu/PASD/LPO/TRAVEL/.*',  // include
      ],
    ]

    sidebar = [baseUrl.toString()]
  }


  /**********************************************************************/
  /**
   * Get title of the doc.
   */

  public String getTitle (Page page, Node doc, Node body) {

    if (page.surl == baseUrl.toString()) {
      return 'Travel Handbook'
    }

    def l = body.selectNodes("//span[@class='heading2']");
    if (l.size() > 0) {
      def title = l[0].text
      return title
    }

    return super.getTitle(page, doc, body)
  }
}
