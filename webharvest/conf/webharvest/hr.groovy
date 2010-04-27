package webharvest

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Node


class hr extends Config {

  def private static log = Logger.getInstance(hr.name);


  /**********************************************************************/
  /**
   * Constructor.
   */

  public hr() {
    super()

    baseUrl = new URL('http://www.lib.umd.edu/PASD/hrpolicies.html')

    addTodo('http://www.lib.umd.edu/PASD/hrservices.html')
    addTodo('http://www.lib.umd.edu/PASD/hrforms.html')
    addTodo('http://www.lib.umd.edu/PASD/hrcollaborative.html')
    addTodo('http://www.lib.umd.edu/groups/learning/PRD_form_new_format_nonexempt_.docx')
    addTodo('http://www.lib.umd.edu/groups/learning/PRDperformance.doc')

    followable = [
      ['^http://www.lib.umd.edu/PASD/.*',        
        '^http://www.lib.umd.edu/PASD/asd.html',
        '^http://www.lib.umd.edu/PASD/LPO/TRAVEL/.*',
        '^http://www.lib.umd.edu/PASD/LPO/ga_job_announcement2009.html',
        '^http://www.lib.umd.edu/PASD/hrcontact.html',
        '^http://www.lib.umd.edu/PASD/hremployment.html',
        '^http://www.lib.umd.edu/PASD/hrhome.html',
        '^http://www.lib.umd.edu/PASD/hrhours.html', 
        '^http://www.lib.umd.edu/PASD/hrstaff.html',
        '^http://www.lib.umd.edu/PASD/studentapplication.doc',  
        '^http://www.lib.umd.edu/PASD/LPO/assessingmentoringrelationship.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/checklistformentoring.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/craftingapersonalmissionstatement.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/menteeguide.html',
        '^http://www.lib.umd.edu/PASD/LPO/menteematrix.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/mentgl.doc',
        '^http://www.lib.umd.edu/PASD/LPO/mentorforms.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentorguide.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/menteeform.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mentorform.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpbenefits.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpforms.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpmatrix1.jpg',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpmenteeguide.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpmentorguide.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpmentors.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpover.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpqanda.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mpresources.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfmembers.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes052002.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes061402.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes072202.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes102802.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes112502.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoring/mtfminutes120902.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoringbenefits2.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoringoverview.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentoringresources.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentorlist2.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentorminutes.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentorquestions.html',
        '^http://www.lib.umd.edu/PASD/LPO/mentortaskforce.html',
        '^http://www.lib.umd.edu/PASD/LPO/reportonmentoring05112005.doc',
        '^http://www.lib.umd.edu/PASD/LPO/toptenreasonstohaveamentor.pdf',
        '^http://www.lib.umd.edu/PASD/LPO/verbalnonverbalmentoring.pdf',
     ],
      '^http://www.lib.umd.edu/PUB/workforce_planning/workforce.planning.10.08.docx',  
      '^http://www.lib.umd.edu/groups/learning/2009_allstaffself.doc',  
      '^http://www.lib.umd.edu/groups/learning/PRD_form_new_format_exempt_.doc',  
      '^http://www.lib.umd.edu/groups/learning/graduate_assist_cover.doc',  
      '^http://www.lib.umd.edu/groups/learning/prd_staffguidelines.doc',  
      '^http://www.lib.umd.edu/groups/learning/exempt_staff_cover.doc',  
      '^http://www.lib.umd.edu/groups/learning/graduate_assist_cover.doc',  
      '^http://www.lib.umd.edu/groups/learning/prd_staff.doc',  
      '^http://www.lib.umd.edu/groups/learning/ga_selfevaluation.doc',  
      '^http://www.lib.umd.edu/groups/learning/no1.doc',  
      '^http://www.lib.umd.edu/groups/learning/no2.doc',  
      '^http://www.lib.umd.edu/groups/learning/no3.doc',  
      '^http://www.lib.umd.edu/groups/learning/no4.doc',  
      '^http://www.lib.umd.edu/groups/learning/timesheet.pdf',  
      '^http://www.lib.umd.edu/groups/learning/offsiteworkfinal.pdf',  
    ]



    def x = new URL('http://www.lib.umd.edu/PUB/workforce_planning/workforce+planning+10.08.docx');
    buildUrls[x] = x
    ctypes[x] = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  }
}
