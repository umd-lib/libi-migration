package webharvest

import org.dom4j.*
import org.dom4j.io.*

import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.PrettyXmlSerializer

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.PosixParser


infile = null
outfile = null
parent = false

// Fixup nodes
//   1. make body valid xml
 
def main() {
  parseCommandLine()

  cleaner = new HtmlCleaner()     // html cleanup
  props = cleaner.getProperties()
  ds = new DomSerializer(props)

  df = new DocumentFactory()      // dom4j utilities
  dr = new DOMReader(df)

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

  if (parent) {
    createParent(doc)
  }

  outfile = new OutputStreamWriter(new FileOutputStream(outfile))
  outfile << doc.asXML().replace('&amp;','&')
}


/**********************************************************************/
/*
 * Create a new, first parent node of all the others with a list of
 * links.
 */

def createParent(doc) {
  // Create body
  def body = DocumentHelper.createDocument();
  def ul = body.addElement('div').addElement('ul')

  doc.selectNodes('//node').sort{ it.attributeValue('created') }.each { n ->
    def title = n.attributeValue('title')
    def unique = n.attributeValue('unique')

    ul
    .addElement('li')
    .addElement('a')
    .addAttribute('href',"[[${unique}]]")
    .addText(title)
  }

  // Add new node to the doc
  def nodes = doc.selectSingleNode('//nodes')

  def node = nodes
  .addElement('node')
  .addAttribute('created', (new Date()).format('yyyy-MM-dd HH:mm:ss').toString())
  .addAttribute('name', 'anonymous')
  .addAttribute('title', 'Parent Node (fixnodes)')
  .addAttribute('type', 'page')
  .addAttribute('unique','fixnodes-parent')
  .addElement('data')
  .addElement('body')
  .addText(body.getRootElement().asXML())

  // Move to the first position
  def list = nodes.content()
  list.add(0, list.pop())
}


/**********************************************************************/
/*
 * Parse the command line.
 */

def parseCommandLine() {
  // Setup the options
  options = new Options()
  
  option = new Option("i", "infile", true, "input file")
  option.setRequired(true)
  option.setType(File)
  options.addOption(option)

  option = new Option("o", "outfile", true, "output file")
  option.setRequired(true)
  option.setType(File)
  options.addOption(option)

  option = new Option("p", "parent", false, "create parent node with links to all others")
  option.setRequired(false)
  options.addOption(option)

  // Parse the command line
  parser = new PosixParser()
  try {
    cmd = parser.parse(options, args)
  }
  catch (Exception e) {
    printUsage(options, e.getMessage())
  }

  // Validate results
  infile = cmd.getOptionObject('i')
  outfile = cmd.getOptionObject('o')
  parent = cmd.hasOption('p')

}


/**********************************************************************/
/*
 * Print program usage.
 */

def printUsage(Options options, Object[] args) {
  // print messages
  args.each {println it}
  if (args.size() != 0) { println '' }

  formatter = new HelpFormatter()
  formatter.printHelp("fixnodes -i <infile> -o <outfile> [-p] \n", options)

  System.exit(1)
}


main()

