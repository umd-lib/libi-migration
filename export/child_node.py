from xml.dom.minidom import Document

class ChildNode:
    parent = ""
    author = ""
    content = ""
    date = ""
    
    def __init__(self, parent, author, content, date):
        self.parent = parent
        self.author = author
        self.content = content
        self.date = date
        
    def xml(self, document, parentNode):
        commentNode = document.createElement("comment")
        commentNode.setAttribute("author",self.author)
        commentNode.setAttribute("date",str(self.date))
        data = document.createElement("data")
        body = document.createElement("body")
        body.appendChild(document.createTextNode(self.content))
        commentNode.appendChild(data)
        data.appendChild(body)
        parentNode.appendChild(commentNode)
        return document
