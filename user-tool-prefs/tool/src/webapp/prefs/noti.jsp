<%-- HTML JSF tag libary --%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%-- Core JSF tag library --%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%-- Sakai JSF tag library --%>
<%@ taglib uri="http://sakaiproject.org/jsf/sakai" prefix="sakai" %>

<f:view>
	<sakai:view_container title="#{msgs.prefs_title}">
    <sakai:stylesheet path="/css/prefs.css"/>
	<sakai:view_content>
	
		<h:form id="options_form">

				
		<script type="text/javascript" language="JavaScript" src="/library/js/jquery.js"></script>
		<script type="text/javascript" language="JavaScript" src="/sakai-user-tool-prefs/js/prefs.js">// </script>
		<script type="text/javascript">
			$(document).ready(function(){
				setupPrefsGen();
				fixImplicitLabeling();
			})  
		</script>
				<%--h:outputText value="User ID: "/><h:inputText value="#{AdminPrefsTool.userId}" /--%>	
				<sakai:tool_bar>
			  <%--sakai:tool_bar_item action="#{UserPrefsTool.processActionRefreshFrmNoti}" value="Refresh" /--%>
 		    <sakai:tool_bar_item value="#{msgs.prefs_noti_title}" rendered="#{UserPrefsTool.noti_selection == 1}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionEdit}" value="#{msgs.prefs_tab_title}" rendered="#{UserPrefsTool.tab_selection == 1}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionTZFrmEdit}" value="#{msgs.prefs_timezone_title}" rendered="#{UserPrefsTool.timezone_selection == 1}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionLocFrmEdit}" value="#{msgs.prefs_lang_title}"rendered="#{UserPrefsTool.language_selection == 1}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionPrivFrmEdit}" value="#{msgs.prefs_privacy}"  rendered="#{UserPrefsTool.privacy_selection == 1}" />
 		    
 		    <sakai:tool_bar_item value="#{msgs.prefs_noti_title}" rendered="#{UserPrefsTool.noti_selection == 2}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionEdit}" value="#{msgs.prefs_tab_title}" rendered="#{UserPrefsTool.tab_selection == 2}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionTZFrmEdit}" value="#{msgs.prefs_timezone_title}" rendered="#{UserPrefsTool.timezone_selection == 2}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionLocFrmEdit}" value="#{msgs.prefs_lang_title}"rendered="#{UserPrefsTool.language_selection == 2}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionPrivFrmEdit}" value="#{msgs.prefs_privacy}"  rendered="#{UserPrefsTool.privacy_selection == 2}" />
 		    
 		    <sakai:tool_bar_item value="#{msgs.prefs_noti_title}" rendered="#{UserPrefsTool.noti_selection == 3}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionEdit}" value="#{msgs.prefs_tab_title}" rendered="#{UserPrefsTool.tab_selection == 3}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionTZFrmEdit}" value="#{msgs.prefs_timezone_title}" rendered="#{UserPrefsTool.timezone_selection == 3}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionLocFrmEdit}" value="#{msgs.prefs_lang_title}"rendered="#{UserPrefsTool.language_selection == 3}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionPrivFrmEdit}" value="#{msgs.prefs_privacy}"  rendered="#{UserPrefsTool.privacy_selection == 3}" />
 		    
 		    <sakai:tool_bar_item value="#{msgs.prefs_noti_title}" rendered="#{UserPrefsTool.noti_selection == 4}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionEdit}" value="#{msgs.prefs_tab_title}" rendered="#{UserPrefsTool.tab_selection == 4}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionTZFrmEdit}" value="#{msgs.prefs_timezone_title}" rendered="#{UserPrefsTool.timezone_selection == 4}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionLocFrmEdit}" value="#{msgs.prefs_lang_title}"rendered="#{UserPrefsTool.language_selection == 4}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionPrivFrmEdit}" value="#{msgs.prefs_privacy}"  rendered="#{UserPrefsTool.privacy_selection == 4}" />
 		    
 		    <sakai:tool_bar_item value="#{msgs.prefs_noti_title}" rendered="#{UserPrefsTool.noti_selection == 5}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionEdit}" value="#{msgs.prefs_tab_title}" rendered="#{UserPrefsTool.tab_selection == 5}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionTZFrmEdit}" value="#{msgs.prefs_timezone_title}" rendered="#{UserPrefsTool.timezone_selection == 5}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionLocFrmEdit}" value="#{msgs.prefs_lang_title}"rendered="#{UserPrefsTool.language_selection == 5}"/>
 		    <sakai:tool_bar_item action="#{UserPrefsTool.processActionPrivFrmEdit}" value="#{msgs.prefs_privacy}"  rendered="#{UserPrefsTool.privacy_selection == 5}" />
 		    
 			</sakai:tool_bar>
				
				<h3>
					<h:outputText value="#{msgs.prefs_noti_title}" />
					<h:panelGroup rendered="#{UserPrefsTool.notiUpdated}"  style="margin:0 3em;font-weight:normal">
						<jsp:include page="prefUpdatedMsg.jsp"/>	
					</h:panelGroup>
				</h3>

	
				<sakai:messages />
				

<%--(gsilver) selectOneRadio renders a table but will not accept a summary attribute. Need mechanism to tell screen readers that the table is a layour table.	 --%>
				<p class="instruction"><h:outputText value="#{msgs.noti_inst_second}"/></p>
			<fieldset><legend><h:outputText value="#{msgs.noti_ann}"/></legend>
				 <h:selectOneRadio value="#{UserPrefsTool.selectedAnnItem}" layout="pageDirection" styleClass="indnt2">
    			 <f:selectItem itemValue="3" itemLabel="#{UserPrefsTool.msgNotiAnn3}"/>
    			 <f:selectItem itemValue="2" itemLabel="#{UserPrefsTool.msgNotiAnn2}"/>
    			 <f:selectItem itemValue="1" itemLabel="#{UserPrefsTool.msgNotiAnn1}"/>
  			     </h:selectOneRadio>
            </fieldset>
  			
   			<fieldset><legend><h:outputText value="#{msgs.noti_rsrc}"/></legend>
				<h:selectOneRadio value="#{UserPrefsTool.selectedRsrcItem}" layout="pageDirection" styleClass="indnt2">
    			<f:selectItem itemValue="3" itemLabel="#{UserPrefsTool.msgNotiRsrc3}"/>
    			<f:selectItem itemValue="2" itemLabel="#{UserPrefsTool.msgNotiRsrc2}"/>
    			<f:selectItem itemValue="1" itemLabel="#{UserPrefsTool.msgNotiRsrc1}"/>
  			</h:selectOneRadio>
        	</fieldset>
         
			<f:subview id="syllabus" rendered="#{!UserPrefsTool.researchCollab}">
  			<fieldset><legend><h:outputText value="#{msgs.noti_syll}"/></legend>
				<h:selectOneRadio value="#{UserPrefsTool.selectedSyllItem}" layout="pageDirection" styleClass="indnt2">
    			<f:selectItem itemValue="3" itemLabel="#{UserPrefsTool.msgNotiSyll3}"/>
    			<f:selectItem itemValue="2" itemLabel="#{UserPrefsTool.msgNotiSyll2}"/>
    			<f:selectItem itemValue="1" itemLabel="#{UserPrefsTool.msgNotiSyll1}"/>
  			</h:selectOneRadio>
        	</fieldset>
  			
  			<fieldset><legend><h:outputText value="#{msgs.noti_mail}"/></legend>
				<h:selectOneRadio value="#{UserPrefsTool.selectedMailItem}" layout="pageDirection" styleClass="indnt2">
    			<f:selectItem itemValue="3" itemLabel="#{UserPrefsTool.msgNotiMail3}"/>
    			<f:selectItem itemValue="2" itemLabel="#{UserPrefsTool.msgNotiMail2}"/>
    			<f:selectItem itemValue="1" itemLabel="#{UserPrefsTool.msgNotiMail1}"/>
  			</h:selectOneRadio>
        	</fieldset>
			</f:subview>
  				
				<p class="act">
				<h:commandButton accesskey="s" id="submit" styleClass="active formButton" value="#{msgs.update_pref}" action="#{UserPrefsTool.processActionNotiSave}"></h:commandButton>
				<h:commandButton accesskey="x" id="cancel" styleClass="formButton"  value="#{msgs.cancel_pref}" action="#{UserPrefsTool.processActionNotiCancel}"></h:commandButton>
				<h:commandButton type="button"  styleClass="dummy blocked" value="#{msgs.update_pref}" style="display:none"></h:commandButton>
				<h:commandButton type="button"  styleClass="dummy blocked" value="#{msgs.cancel_pref}" style="display:none"></h:commandButton>
				</p>	
		 </h:form>
	</sakai:view_content>
	</sakai:view_container>
</f:view>
