package webharvest

import org.dom4j.*
import org.dom4j.io.*

import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer


// Cleanup html
 
def main() {
  cleaner = new HtmlCleaner()     // html cleanup
  props = cleaner.getProperties()
  ds = new DomSerializer(props)

  df = new DocumentFactory()      // dom4j utilities
  dr = new DOMReader(df)

  // Cleanup
  bodyhc = cleaner.clean(new InputStreamReader(System.in, 'UTF-8'))

  // Convert to HtmlCleaner to org.w3c.dom.Document
  bodydom = ds.createDOM(bodyhc)

  // Convert org.ww3c.dom.Document to org.dom4j.Document
  body = dr.read(bodydom)

  println body.getRootElement().asXML()    
}

main()

