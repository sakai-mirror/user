/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.component.common.edu.person;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.InetOrgPerson;
import org.sakaiproject.api.common.edu.person.OrganizationalPerson;
import org.sakaiproject.api.common.edu.person.Person;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import java.util.Date;

/**
 * @author <a href="mailto:lance@indiana.edu">Lance Speelmon </a>
 */
public class SakaiPersonImpl extends EduPersonImpl implements Person, OrganizationalPerson, InetOrgPerson, SakaiPerson
{
	private static final Log LOG = LogFactory.getLog(SakaiPersonImpl.class);

	/**
	 * Empty constuctor for hibernate
	 */
	public SakaiPersonImpl()
	{
		super();
	}

	protected String pictureUrl;

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#getPictureUrl()
	 */
	public String getPictureUrl()
	{
		return pictureUrl;
	}

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#setPictureUrl(java.lang.String)
	 */
	public void setPictureUrl(String pictureURL)
	{
		this.pictureUrl = pictureURL;
	}

	protected Boolean systemPicturePreferred;

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#isSystemPicturePreferred()
	 */
	public Boolean isSystemPicturePreferred()
	{
		return this.systemPicturePreferred;
	}

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#setSystemPicturePreferred(java.lang.Boolean)
	 */
	public void setSystemPicturePreferred(Boolean systemPicturePreferred)
	{
		this.systemPicturePreferred = systemPicturePreferred;
	}

	protected String notes;

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#getNotes()
	 */
	public String getNotes()
	{
		return this.notes;
	}

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#setNotes(java.lang.String)
	 */
	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	protected String campus;

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#getCampus()
	 */
	public String getCampus()
	{
		return this.campus;
	}

	/*
	 * @see org.sakaiproject.service.profile.SakaiPerson#setCampus(java.lang.String)
	 */
	public void setCampus(String school)
	{
		this.campus = school;
	}

	/**
	 * Comment for <code>isPrivateInfoViewable</code>
	 */
	protected Boolean hidePrivateInfo;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.service.profile.SakaiPerson#getIsPrivateInfoViewable()
	 */
	public Boolean getHidePrivateInfo()
	{
		return hidePrivateInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.service.profile.SakaiPerson#setIsPrivateInfoViewable(java.lang.Boolean)
	 */
	public void setHidePrivateInfo(Boolean hidePrivateInfo)
	{
		this.hidePrivateInfo = hidePrivateInfo;
	}

	protected Boolean hidePublicInfo;

	/**
	 * @see org.sakaiproject.service.profile.SakaiPerson#getIsPublicInfoViewable()
	 */
	public Boolean getHidePublicInfo()
	{
		return hidePublicInfo;
	}

	/**
	 * @see org.sakaiproject.service.profile.SakaiPerson#setIsPublicInfoViewable(java.lang.Boolean)
	 */
	public void setHidePublicInfo(Boolean hidePublicInfo)
	{
		this.hidePublicInfo = hidePublicInfo;
	}

	private Boolean ferpaEnabled;

	/**
	 * @see org.sakaiproject.service.profile.SakaiPerson#getFerpaEnabled()
	 * @return Returns the ferpaEnabled.
	 */
	public Boolean getFerpaEnabled()
	{
		return ferpaEnabled;
	}

	/**
	 * @see org.sakaiproject.service.profile.SakaiPerson#setFerpaEnabled(Boolean)
	 * @param ferpaEnabled
	 *        The ferpaEnabled to set.
	 */
	public void setFerpaEnabled(Boolean ferpaEnabled)
	{
		this.ferpaEnabled = ferpaEnabled;
	}

	private Date dateOfBirth; // date of birth
	
	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	
	public void setDateOfBirth(Date d){
		dateOfBirth = d;
	}

	private Boolean locked = false;
	public Boolean getLocked() {
		// TODO Auto-generated method stub
		return locked;
	}

	public void setLocked(Boolean ld) {
		locked = ld ;
		
	}
	
}
