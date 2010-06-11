#!/usr/bin/env python

# This is the main file for the wordpress export
#   It essentially goes through all the wordpress tables and creates nodes based on the
#   Wordpress tables, then outputs the XML


import sys
import getopt
import getpass
import MySQLdb
import re
from xml.dom.minidom import Document

import node
import child_node


# formats Wordpress content (adds paragraphs) 
def format_content (content):
    pattern = re.compile(r"""
           \n\s*\n # The goal of this regex is to match two newlines in a row, with 
                   # only spaces in between. This is where we will add in our paragraph breaks.
            """, re.VERBOSE | re.MULTILINE);
    if len(content) < 4 or content[0:2] != '<p':
        return '<p>' + re.sub(pattern, r'</p><p>', content, 0) + '</p>'
    else:
        return content

# This processes the command line to find the arguments, then we run the script to read the files
username = 'root'
password = ''
table = 'library_matters'
rootName = 'DTIS'
content = 'news'
silent = False
prefix = 'wordpress'
attachments_path = ''
version = '2.0'
debug = False

try:
    opts, args = getopt.getopt(sys.argv[1:], 'u:t:x:r:f:pc:t:sa:v:d', ['username=','table=','root=','prefix=','passfile=','content=','password', 'silent', 'attachments=', 'version=', 'debug'])
except getopt.GetoptError:
    sys.stderr.write("Command line options\n" + \
        "\t-u: username\n" + \
        "\t-p: prompt for password\n" + \
        "\t-f: passfile\n" + \
        "\t-x: prefix for the unique URL's that get generated (such as 'wordpress')\n" + \
        "\t-t: table prefix (e.g., webservices)\n" + \
        "\t-r: root node (e.g., DTIS)\n" + \
        "\t-c: content type (e.g., \"news\" or)\n" + \
        "\t-a: path for the attachments folder\n" + \
        "\t-v: wordpress version; default is 2.0\n" + \
        "\t-s: silent (no output)\n" + \
        "\t-d: debug (verbose output to STDERR)\n")
    sys.exit(2)

for opt, arg in opts:
    if opt in ('-p','--password'):
        password = getpass.getpass()
    if opt in ('-f','--passfile'):
        buf = open(arg, "r")
        password = buf.readline().rstrip("\n")
    if opt in ('-u','--username'):
        username = arg
    if opt in ('-t','--table'):
        table = arg
    if opt in ('-x','--prefix'):
        prefix = arg
    if opt in ('-c','--content'):
        content = arg
    if opt in ('-r','--root'):
        rootName = arg
    if opt in ('-s','--silent'):
        silent = True
    if opt in ('-a','--attachments'):
        attachments_path = arg + '/'
    if opt in ('-v','--version'):
        version = arg
    if opt in ('-d','--debug'):
        debug = True

if (debug):
    template = """Params:
  username: %(u)s
  table: %(t)s
  prefix = %(p)s
  content = %(c)s
  rootName = %(r)s
  silent = %(s)s
  attachments_path = %(a)s
  version = %(v)s
  debug = %(d)s
""" 
    sys.stderr.write(template % { 'u' :username,
                                  't' : table,
                                  'p' : prefix,
                                  'c' : content,
                                  'r' : rootName,
                                  's' : silent,
                                  'a' : attachments_path,
                                  'v' : version,
                                  'd' : debug
                                  })
    

if (debug): sys.stderr.write("connecting to mysql\n")

db = MySQLdb.connect(host="localhost", user=username, passwd=password, db="wordpress")


if (debug): sys.stderr.write("retrieving posts and attachments\n")

cursor = db.cursor(MySQLdb.cursors.DictCursor)
cursor.execute("SELECT user_login, post_content, post_title, post_date, guid, " + table + "_posts.ID, post_mime_type  FROM " + table + "_posts JOIN " + table + "_users ON " + table + "_users.ID = post_author WHERE post_status != 'draft' AND post_title != '' AND post_type in ('post','attachment','')")

nodes = {}
rootNode = node.Node(rootName, "", "", attachments_path)
nodes[rootNode.name] = rootNode
for x in range(0,cursor.rowcount):  
    row = cursor.fetchone()
    nodeType = content
    url, uniquePrefix = "", ""
    if row['post_mime_type'] != '':
        if row['post_mime_type'] == 'image/jpeg':
            nodeType = 'image'
            url = row['guid']
        else:
            nodeType = 'attachment'
            uniquePrefix = 'attachment-'
            url = row['guid']
    newNode = node.Node(row['post_title'], format_content(row['post_content']), rootName, attachments_path)
    newNode.author = str(row['user_login'])
    newNode.date = str(row['post_date'])
    newNode.url = row['guid']
    newNode.unique = uniquePrefix + prefix + '-' + str(row['ID'])
    newNode.contentType = nodeType
    rootNode.children.append(newNode.unique)
    nodes[newNode.unique] = newNode

    if (debug): sys.stderr.write("got %(c)s: %(n)s\n" % {'c':newNode.contentType, 'n':newNode.name})

    categoryCursor = db.cursor(MySQLdb.cursors.DictCursor)
    if version == '2.6':
        categoryCursor.execute("SELECT " + table + "_terms.name AS cat_name FROM " + table + "_posts JOIN " + table + "_term_relationships ON " + table + "_term_relationships.object_id=" + table + "_posts.ID JOIN " + table + "_term_taxonomy ON " + table + "_term_taxonomy.term_taxonomy_id = " + table + "_term_relationships.term_taxonomy_id JOIN " + table + "_terms ON " + table + "_term_taxonomy.term_id = " + table + "_terms.term_id WHERE " + table + "_posts.ID = " + str(row['ID']) + " AND " + table + "_terms.name <> 'Uncategorized'")
    else:
        categoryCursor.execute("SELECT cat_name FROM " + table + "_post2cat JOIN " + table + "_categories ON cat_ID = category_id WHERE post_id = " + str(row['ID']))
    for y in range(0, categoryCursor.rowcount):
      category = categoryCursor.fetchone()
      newNode.categories.append(category['cat_name'])
      
    commentCursor = db.cursor(MySQLdb.cursors.DictCursor)
    commentCursor.execute("SELECT comment_author, comment_content, comment_date FROM " + table + "_comments WHERE comment_post_id = " + str(row['ID']))
    
    for y in range(0, commentCursor.rowcount):
      comment = commentCursor.fetchone()
      childNode = child_node.ChildNode(newNode, comment['comment_author'], comment['comment_content'], comment['comment_date'])
      newNode.childCommentNodes.append(childNode)
      
for node,curNode in nodes.iteritems():
    if curNode.contentType == 'image' or curNode.contentType == 'attachment':
        rootNode.format_links(nodes, curNode.url, curNode.unique, curNode.contentType)

for node,curNode in nodes.iteritems():
    curNode.url = curNode.make_internal_link(curNode.url)



xml = rootNode.xml(Document(), None, nodes)
if not silent:
    xmltext = xml.toprettyxml(indent="  ")
    xmltext = xmltext.replace('<?xml version="1.0" ?>','<?xml version="1.0" encoding="UTF-8" ?>')
    print xmltext
    
