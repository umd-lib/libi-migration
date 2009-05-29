import re
import commands

# This class takes a bunch of content and formats it as if we were displaying it as a wiki
class Formatter:

    # we store the url for any attachments we find here so we can retrieve them later
    url = []

    def __init__(self):
        self.url = []
    
    # While we don't use "format" for any real parsing (we let TWiki handle that),
    # we still try to parse it as best we can so we can find certain data later --
    # this is stored in "old_content" for each node
    def format (self, content):
        self.url = []
        content = self.convert_external_links(content)
        content = self.convert_headers(content)
        content = self.convert_lists(content)
        content = self.convert_bold(content)
        content = self.convert_br(content)
        #print content
        return content

    # Here, we include the logic for finding the headers (h1, h2, etc.)
    def convert_headers(self, content):
        h1 = re.compile(r"""
           ^\-\-\-\+\s\!?       # ---+ is the top-level header
           (?P<heading>.*)$
            """, re.VERBOSE | re.MULTILINE);
        content = re.sub(h1, r'<h1>\1</h1>', content, 0)
        h2 = re.compile(r"""
           \-\-\-\+\+\s\!?      # ---++ is the second-level header
           (?P<heading>.*)$
            """, re.VERBOSE | re.MULTILINE)
        content = re.sub(h2, r'<h2>\1</h2>', content, 0)
        h3 = re.compile(r"""
           \-\-\-\+\+\+\s\!?    # ---++++ is the third-level header
           (?P<heading>.*)$
            """, re.VERBOSE | re.MULTILINE)
        content = re.sub(h3, r'<h3>\1</h3>', content, 0)
        h4 = re.compile(r"""
           \-\-\-\+\+\+\+\s\!?  # ---++++ is the fourth-level header
           (?P<heading>.*)$
            """, re.VERBOSE | re.MULTILINE)
        content = re.sub(h4, r'<h4>\1</h4>', content, 0)
        return content

    # External links are just a find and replace for this regex:
    #   [[http://www.itd.umd.edu/pmwiki/pmwiki.php?n=ITDStaff.MetaLibSFX][Metalib/SFX]]
    def convert_external_links (self, content):
        pattern = re.compile(r"""
            \[\[                            # "[[" is how all external links start
            (?P<url>http*)                  # We match what sort of looks like a URL
            \]\[                            # Now we know it will have "]["
            (?P<title>[^\]]*)               # The second brackets will contain a name
            \]\]
            """, re.VERBOSE)
        return re.sub(pattern, r'<a href="\1">\2</a>', content, 0)
    
    def convert_lists (self, content):
        # I apologize from the bottom of my heart
        pattern = re.compile(r"""
            (?P<content>
            (^\s\s\s+\*([^\n])*$\n?)+        # First we find the content, which
            )                                # starts with spaces followed by a star
            (?P<remainder>
            (^([^\s]|\s[^\s]|\s\s[^\s]|\s\s\s[^\s\*])([^\n])*$)|(^\s*$\n)
                                            
                                             # looking for the first "non-content"
            )
            """, re.VERBOSE | re.MULTILINE | re.S)
        content = re.sub(pattern, r'<ul><li>\n\g<content></li></ul>\n\g<remainder>', content, 0)
        
        pattern = re.compile(r"""
            (?P<content>
            (^\s{6,}\*([^\n])*$\n?)+        # First we find the content, which
            )                                # starts with spaces followed by a star
            (?P<remainder>
            ^([^\s]|\s[^\s]|\s\s[^s]|\s\s\s[^\s]|\s\s\s\s[^\s\*]|
                \s\s\s\s\s[\s]||\s\s\s\s\s\s[^\s\*])([^\n])*$      
                                            
                                             # looking for the first "non-content"
            )
            """, re.VERBOSE | re.MULTILINE | re.S)
        content = re.sub(pattern, r'<ul><li>\n\g<content></li></ul>\n\g<remainder>', content, 0)
        
        pattern = re.compile(r"""
            (?P<content>
            (^\s{9,}\*([^\n])*$\n?)+         # First we find the content, which
            )                                 # starts with spaces followed by a star
            (?P<remainder>
            ^([^\s]|\s[^\s]|\s\s[^s]|\s\s\s[^\s]|\s\s\s\s[^\s\*]|
                \s\s\s\s\s[\s]||\s\s\s\s\s\s[^\s\*]|
                \s\s\s\s\s\s\s[\s]|\s\s\s\s\s\s\s[^\s]|
                \s\s\s\s\s\s\s\s\s[\s\*])
            ([^\n])*$
                                            
                                             # looking for the first "non-content"
            )
            """, re.VERBOSE | re.MULTILINE | re.S)
        content = re.sub(pattern, r'<ul><li>\n\g<content></li></ul>\n\g<remainder>', content, 0)
        
        pattern = re.compile(r"""
            ^\s\s\s+\*
            """, re.VERBOSE | re.MULTILINE)
        content = re.sub(pattern, r'</li><li>', content, 0)
        return content
        
    def convert_br (self, content):
        pattern = re.compile(r"""
            %BR%
            """, re.VERBOSE)
        content = re.sub(pattern, r'<br />', content, 0)
        
        pattern = re.compile(r"""
            (<p>.*)<br\s\/>
            """, re.VERBOSE)
        content = re.sub(pattern, r'\1', content, 0)
        
        pattern = re.compile(r"""
            (<\/?)verbatim>
            """, re.VERBOSE)
        content = re.sub(pattern, r'\1pre>', content, 0)
        return content
        
    def convert_bold (self, content):
        pattern = re.compile(r"""
            \*([^*]*)\*
            """, re.VERBOSE | re.MULTILINE | re.S)
        content = re.sub(pattern, r'<strong>\1</strong>', content, 0)
        
        pattern = re.compile(r"""
            (?P<heading>
                \<strong\>[^<]*\<\/strong\>
            )
            (?P<paragraph>
                [^<]*
            )
            (?P<remainder>
                \n\s*\n
            )
            """, re.VERBOSE | re.MULTILINE | re.S)
        content = re.sub(pattern, r'\g<heading><p>\g<paragraph></p>\g<remainder>', content, 0)
        
        return content
        
    def format_links(self, content, target, unique, attachment_path):
        if attachment_path != "":
            pattern = re.compile(r"""
                %ATTACHURL%
                (?P<content>[^\"\]]*)
                """, re.VERBOSE)
            
            result = pattern.search(content)
            if result is not None:
                self.url.append(attachment_path + result.group(1))
                content = re.sub(pattern, '[' + attachment_path + r'\g<content>]', content, 0)
        
        return content
        
def format(formatter, content):
    return formatter.format(content)

def format_internal_links(formatter, content, target, unique, attachment_path):
    return formatter.format_links(content, target, unique, attachment_path)
    
 
def fix_internal_processed(content, prefix, attachment_path):
    pattern = re.compile(r"""
        http://www.lib.umd.edu/twiki/bin/view/""" + prefix + r"""/
        (?P<content>[^"]*)
        """, re.VERBOSE)
            
    content = re.sub(pattern, r'[[' + prefix + r'-\g<content>]]', content, 0)
    
    pattern = re.compile(r"""
        http://www.lib.umd.edu/twiki/pub/""" + prefix + r"""/
        (?P<content>[^"]*)
        \"
        """, re.VERBOSE)
    attachment_path = attachment_path.partition('/')[0]
    content = re.sub(pattern, r'[[attachment-' + attachment_path + r'/\g<content>]]"', content, 0)
    
    f = open('temporary', 'w')
    f.write(content)
    f.close()
    content = commands.getoutput('python strip_xml.py temporary')
    
    

    return content

