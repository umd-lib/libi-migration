#!/usr/bin/env python
import sys
import getopt

import processor

# This processes the command line to find the arguments, then we run the script to read the files
path = 'DTIS/'
prefix = 'DTIS'
silent = False
attachments_path = 'extra/'
try:
    opts, args = getopt.getopt(sys.argv[1:], 'f:p:a:s', ['file=','prefix=','attachments=','silent'])
except getopt.GetoptError:
    print "Command line options\n" + \
        "\t-f: the filepath for the folder with the main data (e.g., DTIS for the DTIS/ folder)\n" + \
        "\t-a: path for the attachments folder\n" + \
        "\t-p: prefix that will be prepended to make unique names\n" + \
        "\t-s: specify this option to execute silently (not to generate XML)\n"
    sys.exit(2)

for opt, arg in opts:
    if opt in ('-f','--file'):
        path = arg
    if opt in ('-p','--prefix'):
        prefix = arg
    if opt in ('-a','--attachments'):
        attachments_path = arg + '/'
    if opt in ('-s','--s'):
        silent = True
processor = processor.Processor(prefix)
processor.read_files(path, attachments_path, silent)
