package webharvest

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Transient

/**
 * A web location.  Not necessarily text/html.
 */

// Grabbing hibernate will also grab annotations
// TODO: grab annotations directly
@Grapes([
    @Grab(group='org.hibernate', module='hibernate-annotations', version='3.4.0.GA'),
])

@Entity class Page implements Cloneable {
  @Id
  public String surl     // string representation of url

  public URL url         // url
  public String ctype    // content type
  public String created  // node creation time
  public String name     // node author name
  public String title    // node title
  public String type     // node type
  public String uniq     // node unique identifier
  public String body     // content body
  public File download   // downloaded file name
  public int depth       // depth of link, baseUrl is 0
  @Transient public Page fromPage   // linked from these pages
  
  public static ignoreQuery = true  // ignore query string when comparing urls?


  /**********************************************************************
  /**
   * Clone this object.
   */

  protected Object clone() {
    def clone = new Page()
    clone.url     = this.url     
    clone.surl    = this.surl    
    clone.ctype   = this.ctype   
    clone.created = this.created 
    clone.name    = this.name    
    clone.title   = this.title   
    clone.type    = this.type    
    clone.uniq    = this.uniq    
    clone.download = this.download
    clone.fromPage = this.fromPage
    clone.depth    = this.depth

    return clone
  }


  /**********************************************************************/
  /**
   * Determines if two page objects are the same.  Exclude the anchor in
   * the url.
   */

  boolean equals(Object o) {
    if (o == null) return false

    if (!(o instanceof Page)) return false

    if (!url.sameFile(o.url)) return false

    return (ignoreQuery ? true : url.query.equals(o.url.query))
  }


  /**********************************************************************/
  /**
   * Get the query params as a map
   */

  public Map getQuery() {

    def q = [:]

    url.query?.split('&').each {
      def (k, v) = it.split('=')
      q[k] = v
    }

    return q
  }

  
  /**********************************************************************/
  /**
   * Get the url.
   */

  public URL getUrl() {
    return url
  }


  /**********************************************************************/
  /**
   * Get the url with the anchor removed.
   */

  public URL getUrlNoAnchor() {
    return new URL(url.protocol, url.host, url.port, url.path)
  }


  /**********************************************************************/
  /**
   * Set the url; make a string copy for lookup in hibernate.
   */

  public void setUrl(URL url) {
    this.url = url
    surl = url.toString()
  }


  /**********************************************************************/
  /**
   * String representation.
   */

  public String toString() {
    return "${surl}; ${ctype}"
  }

}
