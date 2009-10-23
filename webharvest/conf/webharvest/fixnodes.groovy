package webharvest

import org.dom4j.*
import org.dom4j.io.*

import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer


// Fixup nodes
//   1. make body valid xml
 
def main() {
  cleaner = new HtmlCleaner()     // html cleanup
  props = cleaner.getProperties()
  ds = new DomSerializer(props)

  df = new DocumentFactory()      // dom4j utilities
  dr = new DOMReader(df)

  infile = new File(args[0])

  saxreader = new SAXReader()
  doc = saxreader.read(infile)


  doc.selectNodes('//node/data/body').each { n ->
    // Cleanup
    bodyhc = cleaner.clean(new StringReader(n.text))

    // Convert to HtmlCleaner to org.w3c.dom.Document
    bodydom = ds.createDOM(bodyhc)

    // Convert org.ww3c.dom.Document to org.dom4j.Document
    body = dr.read(bodydom)

    n.text = body.getRootElement().asXML()    
  }

  println doc.asXML()
}

main()

