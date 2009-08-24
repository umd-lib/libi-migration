<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	    			
  <!-- Report on "empty" nodes -->
  
  <xsl:output method="text"/>
  
  <xsl:template match="node">
    <xsl:if test="@type != 'image' and count(data/attachment) = 0 and string-length(data/body) &lt; 100">
      <xsl:value-of select="string-length(data/body)"/>
      <xsl:value-of select="@unique"/>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="@title"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
