import os
import re
import commands
from xml.dom.minidom import Document
from datetime import datetime

import node
import format_content

class Processor:
    prefix = ''
    nodes = {}

    def __init__(self, prefix):
        self.prefix = prefix

    def printNode(self, node, spaces):
        print spaces + str(node)
        for i in node.children:
            self.printNode(self.nodes[i], spaces + " ")

    def processChildren(self, silent):
        rootNode = node.Node(self.prefix, "", "", "")
        rootNode.unique = self.prefix
        for name, curNode in self.nodes.iteritems():
            if curNode.parent != '':
                self.nodes[curNode.parent].children.append(name)
            else:
                rootNode.children.append(name)
        xml = rootNode.xml(Document(), None, self.nodes)
        if not silent:
            print xml.toprettyxml(indent="  ")
        
    # Each line of the wiki file could have meta information which
    # we will need to store in the node. Meta information could
    # be the parent of the node, the author of the node, etc.
    def processMeta(self, curNode, line):
        if "TOPICPARENT" in line:
            curNode.parent = line[line.find("\"")+1:line.find("\"",line.find("\"")+1)]
        elif "TOPICINFO" in line:
            curNode.author = re.compile('.*author=\"([^\"]*)\".*').match(line).group(1)
            curNode.date = datetime.fromtimestamp(int(re.compile('.*date=\"([^\"]*)\".*').match(line).group(1))).strftime("%Y-%m-%d %H:%S:%M")

    def process(self, path, name, attachment_path, attachments_path):
        buffer = open(path)
        content = ""
        curNode = node.Node(name, content, "", attachments_path)
        for line in buffer:
            if "META" in line:
                self.processMeta(curNode, line)
            else:
                content += line
        if curNode.parent == '' and curNode.name != 'WebHome':
            curNode.parent = 'WebHome'
        formatter = format_content.Formatter()

        
        curNode.unique = self.prefix + "-" + curNode.name
        curNode.contentType = 'folder'
        curNode.attachment_path = attachment_path
        
        
        curNode.old_content = format_content.format(formatter, content + "\n \n")
        curNode.content = commands.getoutput('/usr/local/apache/twiki/bin/export ' + self.prefix + ' ' + name)
        
        curNode.content = format_content.fix_internal_processed(curNode.content, self.prefix, attachment_path)
        
        format_content.format_internal_links(formatter, curNode.old_content, curNode.name, curNode.unique, curNode.attachment_path)
        
        if attachment_path.strip() != '':
            # We will now find all the attachments for this node and add them
            list = os.listdir(attachment_path)
            list.sort()
            for f in list:
                if not f.endswith(',v'):
                    curNode.attachments.append([attachment_path + '/' + f, "attachment-" + attachment_path + '/' + f])
            
        self.nodes[curNode.name] = curNode

    def read_files(self, directory, attachments_path, silent):
        attachments = os.listdir(attachments_path)
        list = os.listdir(directory)
        list.sort()
        for f in list:
            if f.startswith('Web') and f != 'WebHome.txt':
                continue
            if os.path.isfile(os.path.join(directory, f)) and f.endswith('txt'):
                attachment_path = ""
                if f.replace('.txt','') in attachments:
                    attachment_path = os.path.join(attachments_path, f.replace('.txt',''))
                self.process(os.path.join(directory, f), f.replace('.txt',''), attachment_path, attachments_path)
        self.processChildren(silent)
        
