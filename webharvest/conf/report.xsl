<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	    			
  <!-- Report on the contents of a nodes xml file -->
  
  <xsl:output method="text"/>
  
  <xsl:param name="list" select="'false'"/>
  
  <xsl:template match="/nodes">
  
    <xsl:if test="$list = 'true'">
      <xsl:text>&#10;Nodes&#10;</xsl:text>
      <xsl:call-template name="listNodes"/>
      <xsl:text>&#10;</xsl:text>
    </xsl:if>
  
    <xsl:text>Statistics&#10;</xsl:text>
  
    <xsl:text>  Nodes:       </xsl:text>
    <xsl:value-of select="count(//node)"/>
    <xsl:text>&#10;</xsl:text>
  
    <xsl:text>  Attachments: </xsl:text>
    <xsl:value-of select="count(//node/data/attachment)"/>
    <xsl:text>&#10;</xsl:text>
  
  </xsl:template>
  
  
  <xsl:template name="listNodes">
    <xsl:param name="indent" select="'  '"/>
  
    <xsl:for-each select="./node">
      <xsl:value-of select="$indent"/>
      <xsl:value-of select="@unique"/>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="@title"/>
      <xsl:text>)&#10;</xsl:text>

      <xsl:call-template name="listNodes">
        <xsl:with-param name="indent" select="concat($indent, '  ')"/>
      </xsl:call-template>

    </xsl:for-each>

  </xsl:template>

</xsl:stylesheet>
