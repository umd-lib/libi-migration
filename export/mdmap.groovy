#!/usr/bin/groovy

@Grab('dom4j:dom4j:1.6.1')
@Grab('jaxen:jaxen:1.1.1')
import org.dom4j.*
import org.dom4j.io.*

def main() {

  saxreader = new SAXReader()
  infiler = new InputStreamReader(new FileInputStream('work/mdmap/nodes.xml'), "UTF-8")
  doc = saxreader.read(infiler)

  doc.selectNodes("/nodes/node[@type='blog_post']").each { n ->
    title = n.selectSingleNode("@title").text
    body = saxreader.read(new StringReader(n.selectSingleNode("data/body").text))
    year = body.selectSingleNode("//tr[td[text() = 'Year']]/td[2]").text.replaceAll(/\n/,' ').trim()
    callnum = body.selectSingleNode("//tr[td[text() = 'Map Call Number']]/td[2]").text.replaceAll(/\n/,' ').trim()

    println "title:$title"
    println "year:$year"
    println "callnum:$callnum"    
  }
}

main()

