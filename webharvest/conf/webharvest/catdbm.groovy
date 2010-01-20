package webharvest

import org.apache.log4j.Logger;

import org.dom4j.Node


class catdbm extends usmai {

  def private static log = Logger.getInstance(catdbm.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public catdbm() {
    super()

    baseUrl = new URL('http://usmai.umd.edu/catdbmaint/')

    followable = [
      ['http://usmai.umd.edu/catdbmaint/.*'],
      ['http://usmai.umd.edu/cpc/Bib_Standardsrev2.doc'],
      ['http://usmai.umd.edu/cpc/USMAI_table_change_process.pdf'],
      ['http://usmai.umd.edu/cpc/usurped_records.doc'],
      ['http://usmai.umd.edu/cpc/SingleRec_Processing.doc'],
      ['http://usmai.umd.edu/cpc/NewTitles_Bestprac.doc'],
      ['http://usmai.umd.edu/cpc/authorities/implementation.html'],
      ['http://usmai.umd.edu/cpc/Appendix_A.doc'],
      ['http://usmai.umd.edu/cpc/usmai_inst_codes.html'],
      ['http://usmai.umd.edu/cpc/cat_contacts.html'],
      ['http://usmai.umd.edu/cpc/cat_workshop.html'],
      ['http://usmai.umd.edu/cpc/record_forum.html'],
    ]

  }
}
