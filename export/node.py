from xml.dom.minidom import Document
import re

import child_node

class Node:
    # old_content exists so that when we apply Ben's script on content, we can 
    # still retain the old TWiki stuff for parsing
    old_contnet = ""
    content = ""
    name = ""
    parent = ""
    date = ""
    author = ""
    unique = ""
    contentType = ""
    url = ""
    categories = []
    children = []
    childCommentNodes = []
    attachments = []
    attachments_path = ""
    
    def __init__(self, name, content, parent, attachments_path):
        self.name = name
        self.old_content = content
        self.content = content
        self.parent = parent
        self.unique = ""
        self.date = ""
        self.author = ""
        self.contentType = ""
        self.attachment_path = ""
        self.children = []
        self.childCommentNodes = []
        self.categories = []
        self.attachments = []
        self.attachments_path = attachments_path
        
    def __str__(self):
        return "Node name: " + self.name + \
            ", node parent: " + self.parent + \
            ", content length: " + str(len(self.content)) + \
            ", date: " + self.date + \
            ", author: " + self.author + \
            ", children: " + str(self.children)
            
    def xml(self, document, parentNode, nodeList):
        if self.contentType == 'attachment':
            return
        if parentNode is None:
            node = document.createElement("nodes")
            document.appendChild(node)
        else:
            node = document.createElement("node")
            parentNode.appendChild(node)
            node.setAttribute("name",self.author)
            node.setAttribute("title",self.name)
            node.setAttribute("unique",self.unique)
            node.setAttribute("created",self.date)
            if self.contentType == 'image'  or self.contentType == 'attachment':
                node.setAttribute("url",self.url)
            node.setAttribute("type",self.contentType)
            data = document.createElement("data")
            body = document.createElement("body")
            node.appendChild(data)
            data.appendChild(body)
            body.appendChild(document.createTextNode(self.content))
            for i in self.childCommentNodes:
                i.xml(document, data)
            for i in self.categories:
                cat = document.createElement("category")
                data.appendChild(cat)
                cat.setAttribute("value", i)
            for i in self.attachments:
                attach = document.createElement("attachment")
                data.appendChild(attach)
                attach.setAttribute("url", i[0].replace(self.attachments_path,''))
                attach.setAttribute("unique", i[1])
        for i in self.children:
            nodeList[i].xml(document, node, nodeList)
        return document
        
    def format_links (self, nodes, url, unique, contentType):
        if contentType == 'attachment' and self.content.find(url) >= 0:
            self.attachments.append([self.make_internal_link(url), unique])
        for i in self.children:
            nodes[i].format_links(nodes, url, unique, contentType)
        self.content = self.content.replace(url, '[[' + unique + ']]')
        
    # What this function does is it takes a link in the form "http://www.blah1/blah2.blah3
    # and returns blah2.blah3, essentially "making" an internal link 
    def make_internal_link (self, url):
        pattern = re.compile(r"""
            http.*\/([^\/]+\.[^\/]+$)
            """, re.VERBOSE)
        url = re.sub(pattern, r'\1', url, 0)
        return url
