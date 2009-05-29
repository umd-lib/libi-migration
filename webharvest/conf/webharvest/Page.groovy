package webharvest

import javax.persistence.*
import org.hibernate.cfg.*

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

    return clone
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
