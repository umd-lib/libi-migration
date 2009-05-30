#!/usr/bin/env groovy

package webharvest

import javax.persistence.Entity

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


parseCommandLine()

try {
 
  // Setup logging
  System.setProperty('log4j.configuration', 'log4j.properties')

  log = Logger.getInstance('webharvest')

  if (debug) {
    log.setPriority(Priority.DEBUG)
  }

  // Hibernate for object storage
  hb = configureHibernate()

  // Create the configuration object
  try {
    // We should be able to dynamically load a class from a string
    // name of the class, but it's been a nightmare.  I'm punting.
    switch (config) {
      case 'Config':      conf = new webharvest.Config(); break
      case 'ConfigTest':  conf = new webharvest.ConfigTest(); break
      case 'PmItd':       conf = new webharvest.PmItd(); break
      default: throw new Exception("Error: unknown config: ${config}")
    }
  }
  catch (Throwable t) {
    println "Error: unable to instanciate ${config} class\n"
    t.printStackTrace()
    System.exit(1)
  }

  conf.outfile = outfile
  conf.out = new groovy.xml.MarkupBuilder(outfile) 
  conf.hb = hb

  // add command-line vars
  vars.each {
    (k, v) = it.split('=')
    conf.var[k] = v
  }

  // Execute the harvest
  conf.harvest()

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
            : 'Config'
            )

  // workdir
  workdir = (cmd.hasOption('w')
             ? cmd.getOptionObject('w')
             : new File('work')
             )
  if (! workdir.isDirectory() || ! workdir.canWrite()) {
    printUsage(options, "Error: Can't write to directory '${workdir}'")
  }

  // outfile
  if (cmd.hasOption('o')) {
    outfile = cmd.getOptionObject('o')
  } else {
    outfile = new File(workdir, 'out.xml')
  }

  if (outfile.exists() && ! outfile.canWrite()) {
    printUsage(options, "Error: Unable to write to ${outfile}")
  }

  outfile = new OutputStreamWriter(new FileOutputStream(outfile))


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

