#!/usr/bin/env groovy

package webharvest

import javax.persistence.Entity

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory

import javax.xml.transform.stream.StreamSource 
import javax.xml.transform.stream.StreamResult 

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.PosixParser

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Priority;

import org.hibernate.cfg.AnnotationConfiguration


// Use grape repository to download required libraries
@Grapes([
    @Grab(group='org.hibernate', module='hibernate-annotations', version='3.4.0.GA'),
    @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.4.2'),
    @Grab(group='javassist', module='javassist', version='3.4.GA'),
])

// Load necessary annotation classes
@Entity class Foo {}

// We should be able to dynamically load a class from a string
// name of the class, but it's been a nightmare.  I'm punting.
configs = [
  config:   new webharvest.Config(),
  test:     new webharvest.ConfigTest(),
  pmitd:    new webharvest.PmItd(),
  la:       new webharvest.LibraryAssembly(),
  tsd:      new webharvest.TSD(),
  itd:      new webharvest.ITD(),
]

parseCommandLine()

try {
 
  tFactory = TransformerFactory.newInstance()

  // Setup logging
  System.setProperty('log4j.configuration', 'log4j.properties')

  log = Logger.getInstance('webharvest')

  if (debug) {
    log.setPriority(Priority.DEBUG)
  }

  // Setup the download directory
  downloaddir = new File(workdir, 'files')

  if (downloaddir.exists()) {
    log.info("Deleting contents of ${downloaddir}")
    downloaddir.eachFile { it.delete() }

  } else {

    log.info("Creating ${downloaddir}")
    if (!downloaddir.mkdirs()) {
      throw new Exception("Error: unable to create ${downloaddir}")
    }
  }

  // Hibernate for object storage
  hb = configureHibernate()

  // Setup the config
  conf.out = new groovy.xml.MarkupBuilder(outfilew) 
  conf.hb = hb
  conf.downloaddir = downloaddir

  // add command-line vars
  vars.each {
    (k, v) = it.split('=',2)
    conf.var[k] = v
  }

  // Execute the harvest
  conf.harvest()

  // Report the results
  trans = tFactory.newTransformer(new StreamSource(new File('conf/report.xsl')))
  source = new StreamSource(outfile)
  result = new StreamResult(System.out)
  trans.transform(source, result)
  

  // Remove the hibernate db
  log.info("Removing hibernate db")

  def db = new File(workdir, 'db')
  def files = []
  db.eachFileRecurse { files << it } 
  files.reverseEach { it.delete() }
  db.delete()

}
catch (Throwable t) {
  t.printStackTrace()
  System.exit(1)
}
System.exit(0)



/**********************************************************************/
/*
 * Configure hibernate for object storage.
 */

def configureHibernate() {

  log.info("Initializing Hibernate")

  // Derby log location
  System.setProperty('derby.stream.error.file', "${workdir}/derby.log")

  // hibernate properties
  def hbProps = [
    "hibernate.dialect":                 "org.hibernate.dialect.DerbyDialect",
    "hibernate.connection.driver_class": "org.apache.derby.jdbc.EmbeddedDriver",
    "hibernate.connection.url":          "jdbc:derby:${workdir}/db;create=true",
    "hibernate.connection.username":     "",
    "hibernate.connection.password":     "",
    "hibernate.connection.pool_size":    "1",
    "hibernate.connection.autocommit":   "true",
    "hibernate.cache.provider_class":    "org.hibernate.cache.NoCacheProvider",
    "hibernate.hbm2ddl.auto":            "create-drop",
    "hibernate.show_sql":                "true",
    "hibernate.transaction.factory_class": "org.hibernate.transaction.JDBCTransactionFactory",
    "hibernate.current_session_context_class": "thread"
  ]

  def config = new AnnotationConfiguration()
  hbProps.each { k, v -> config.setProperty(k, v) }
  config.addAnnotatedClass(Page)

  def factory = config.buildSessionFactory()
  def session = factory.currentSession
  session.beginTransaction()

  return session
}


/**********************************************************************/
/*
 * Parse the command line.
 */

def parseCommandLine() {
  // Setup the options
  options = new Options()
  
  option = new Option("c", "config", true, "config class in conf; default is Config")
  option.setRequired(false)
  option.setType(String)
  options.addOption(option)

  option = new Option("o", "outfile", true, "output file; default is <workdir>/out.xml")
  option.setRequired(false)
  option.setType(File)
  options.addOption(option)

  option = new Option("w", "work", true, "working directory; default is work")
  option.setRequired(false)
  option.setType(File)
  options.addOption(option)

  option = new Option("d", "debug", false, "turn on debugging")
  option.setRequired(false)
  options.addOption(option)

  option = new Option("h", "help", false, "show this help")
  option.setRequired(false)
  options.addOption(option)

  // Parse the command line
  parser = new PosixParser()
  cmd = parser.parse(options, args)

  // Validate results
  if (cmd.hasOption('h')) {
    printUsage(options)
  }

  // config
  config = (cmd.hasOption('c')
            ? cmd.getOptionObject('c')
            : 'config'
            )
  if (configs.containsKey(config)) {
      conf = configs[config]
  } else {
    printUsage(options, "Error: unknown config: ${config}")
  }

  // workdir
  workdir = (cmd.hasOption('w')
             ? cmd.getOptionObject('w')
             : new File("work/${config}")
             )
  if (! workdir.isDirectory() || ! workdir.canWrite()) {
    printUsage(options, "Error: Can't write to directory '${workdir}'")
  }

  // outfile
  if (cmd.hasOption('o')) {
    outfile = cmd.getOptionObject('o')
  } else {
    outfile = new File(workdir, 'nodes.xml')
  }

  if (outfile.exists() && ! outfile.canWrite()) {
    printUsage(options, "Error: Unable to write to ${outfile}")
  }

  outfilew = new OutputStreamWriter(new FileOutputStream(outfile))


  // debug
  debug = cmd.hasOption('d')

  // vars
  vars = cmd.getArgList()
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
  formatter.printHelp("webharvest [-c <configfile> -o <outfile> -w <workdir> -d -h] [var=value var=value...]\n", options)

  System.exit(1)
}

