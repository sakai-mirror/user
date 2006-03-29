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

package org.sakaiproject.user.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entity.api.Edit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserAlreadyDefinedException;
import org.sakaiproject.user.api.UserDirectoryProvider;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserEdit;
import org.sakaiproject.user.api.UserFactory;
import org.sakaiproject.user.api.UserIdInvalidException;
import org.sakaiproject.user.api.UserLockedException;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.api.UserPermissionException;
import org.sakaiproject.user.api.UsersShareEmailUDP;
import org.sakaiproject.util.BaseResourceProperties;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.util.StorageUser;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.webapp.api.SessionBindingEvent;
import org.sakaiproject.webapp.api.SessionBindingListener;
import org.sakaiproject.webapp.api.SessionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>
 * BaseUserDirectoryService is a Sakai user directory services implementation.
 * </p>
 */
public abstract class BaseUserDirectoryService implements UserDirectoryService, StorageUser, UserFactory
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(BaseUserDirectoryService.class);

	/** Storage manager for this service. */
	protected Storage m_storage = null;

	/** The initial portion of a relative access point URL. */
	protected String m_relativeAccessPoint = null;

	/** An anon. user. */
	protected User m_anon = null;

	/** A user directory provider. */
	protected UserDirectoryProvider m_provider = null;

	/** Key for current service caching of current user */
	protected final String M_curUserKey = getClass().getName() + ".currentUser";

	/** A cache of calls to the service and the results. */
	protected Cache m_callCache = null;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Abstractions, etc.
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Construct storage for this service.
	 */
	protected abstract Storage newStorage();

	/**
	 * Access the partial URL that forms the root of resource URLs.
	 * 
	 * @param relative
	 *        if true, form within the access path only (i.e. starting with /content)
	 * @return the partial URL that forms the root of resource URLs.
	 */
	protected String getAccessPoint(boolean relative)
	{
		return (relative ? "" : m_serverConfigurationService.getAccessUrl()) + m_relativeAccessPoint;
	}

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The user id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String userReference(String id)
	{
		// clean up the id
		id = cleanId(id);

		return getAccessPoint(true) + Entity.SEPARATOR + id;
	}

	/**
	 * Access the user id extracted from a user reference.
	 * 
	 * @param ref
	 *        The user reference string.
	 * @return The the user id extracted from a user reference.
	 */
	protected String userId(String ref)
	{
		String start = getAccessPoint(true) + Entity.SEPARATOR;
		int i = ref.indexOf(start);
		if (i == -1) return ref;
		String id = ref.substring(i + start.length());
		return id;
	}

	/**
	 * Check security permission.
	 * 
	 * @param lock
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if allowd, false if not
	 */
	protected boolean unlockCheck(String lock, String resource)
	{
		if (!m_securityService.unlock(lock, resource))
		{
			return false;
		}

		return true;
	}

	/**
	 * Check security permission.
	 * 
	 * @param lock1
	 *        The lock id string.
	 * @param lock2
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if either allowed, false if not
	 */
	protected boolean unlockCheck2(String lock1, String lock2, String resource)
	{
		if (!m_securityService.unlock(lock1, resource))
		{
			if (!m_securityService.unlock(lock2, resource))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Check security permission.
	 * 
	 * @param lock
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @exception UserPermissionException
	 *            Thrown if the user does not have access
	 */
	protected void unlock(String lock, String resource) throws UserPermissionException
	{
		if (!unlockCheck(lock, resource))
		{
			throw new UserPermissionException(m_sessionManager.getCurrentSessionUserId(), lock, resource);
		}
	}

	/**
	 * Check security permission.
	 * 
	 * @param lock1
	 *        The lock id string.
	 * @param lock2
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @exception UserPermissionException
	 *            Thrown if the user does not have access to either.
	 */
	protected void unlock2(String lock1, String lock2, String resource) throws UserPermissionException
	{
		if (!unlockCheck2(lock1, lock2, resource))
		{
			throw new UserPermissionException(m_sessionManager.getCurrentSessionUserId(), lock1 + "/" + lock2, resource);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Configuration: set the user directory provider helper service.
	 * 
	 * @param provider
	 *        the user directory provider helper service.
	 */
	public void setProvider(UserDirectoryProvider provider)
	{
		m_provider = provider;
	}

	/** The # seconds to cache gets. 0 disables the cache. */
	protected int m_cacheSeconds = 0;

	/**
	 * Set the # minutes to cache a get.
	 * 
	 * @param time
	 *        The # minutes to cache a get (as an integer string).
	 */
	public void setCacheMinutes(String time)
	{
		m_cacheSeconds = Integer.parseInt(time) * 60;
	}

	/** The # seconds to cache gets. 0 disables the cache. */
	protected int m_cacheCleanerSeconds = 0;

	/**
	 * Set the # minutes between cache cleanings.
	 * 
	 * @param time
	 *        The # minutes between cache cleanings. (as an integer string).
	 */
	public void setCacheCleanerMinutes(String time)
	{
		m_cacheCleanerSeconds = Integer.parseInt(time) * 60;
	}

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		m_serverConfigurationService = service;
	}

	/** Dependency: EntityManager. */
	protected EntityManager m_entityManager = null;

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		m_entityManager = service;
	}

	/** Configuration: case sensitive user id. */
	protected boolean m_caseSensitiveId = false;

	/**
	 * Configuration: case sensitive user id
	 * 
	 * @param value
	 *        The case sensitive user ide.
	 */
	public void setCaseSensitiveId(String value)
	{
		m_caseSensitiveId = new Boolean(value).booleanValue();
	}

	/** Dependency: SecurityService. */
	protected SecurityService m_securityService = null;

	/**
	 * Dependency: SecurityService.
	 */
	public void setSecurityService(SecurityService service)
	{
		m_securityService = service;
	}

	/** Dependency: FunctionManager. */
	protected FunctionManager m_functionManager = null;

	/**
	 * Dependency: FunctionManager.
	 * 
	 * @param manager
	 *        The FunctionManager.
	 */
	public void setFunctionManager(FunctionManager manager)
	{
		m_functionManager = manager;
	}

	/** Dependency: the session manager. */
	protected SessionManager m_sessionManager = null;

	/**
	 * Dependency - set the session manager.
	 * 
	 * @param value
	 *        The session manager.
	 */
	public void setSessionManager(SessionManager manager)
	{
		m_sessionManager = manager;
	}

	/** Dependency: MemoryService. */
	protected MemoryService m_memoryService = null;

	/**
	 * Dependency: MemoryService.
	 * 
	 * @param service
	 *        The MemoryService.
	 */
	public void setMemoryService(MemoryService service)
	{
		m_memoryService = service;
	}

	/** Dependency: EventTrackingService. */
	protected EventTrackingService m_eventTrackingService = null;

	/**
	 * Dependency: EventTrackingService.
	 * 
	 * @param service
	 *        The EventTrackingService.
	 */
	public void setEventTrackingService(EventTrackingService service)
	{
		m_eventTrackingService = service;
	}

	/** Dependency: the current manager. */
	protected ThreadLocalManager m_threadLocalManager = null;

	/**
	 * Dependency - set the current manager.
	 * 
	 * @param value
	 *        The current manager.
	 */
	public void setThreadLocalManager(ThreadLocalManager manager)
	{
		m_threadLocalManager = manager;
	}

	/** Dependency: AuthzGroupService. */
	protected AuthzGroupService m_authzGroupService = null;

	/**
	 * Dependency: AuthzGroupService.
	 * 
	 * @param service
	 *        The AuthzGroupService.
	 */
	public void setAuthzGroupService(AuthzGroupService service)
	{
		m_authzGroupService = service;
	}

	/** Dependency: TimeService. */
	protected TimeService m_timeService = null;

	/**
	 * Dependency: TimeService.
	 * 
	 * @param service
	 *        The TimeService.
	 */
	public void setTimeService(TimeService service)
	{
		m_timeService = service;
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			m_relativeAccessPoint = REFERENCE_ROOT;

			// construct storage and read
			m_storage = newStorage();
			m_storage.open();

			// make an anon. user
			m_anon = new BaseUserEdit("");

			// <= 0 indicates no caching desired
			if ((m_cacheSeconds > 0) && (m_cacheCleanerSeconds > 0))
			{
				// build a synchronized map for the call cache, automatiaclly checking for expiration every 15 mins, expire on user events, too.
				m_callCache = m_memoryService.newHardCache(m_cacheCleanerSeconds, userReference(""));
			}

			// register as an entity producer
			m_entityManager.registerEntityProducer(this, REFERENCE_ROOT);

			// register functions
			m_functionManager.registerFunction(SECURE_ADD_USER);
			m_functionManager.registerFunction(SECURE_REMOVE_USER);
			m_functionManager.registerFunction(SECURE_UPDATE_USER_OWN);
			m_functionManager.registerFunction(SECURE_UPDATE_USER_ANY);

			// if no provider was set, see if we can find one
			if (m_provider == null)
			{
				m_provider = (UserDirectoryProvider) ComponentManager.get(UserDirectoryProvider.class.getName());
			}

			M_log.info("init(): provider: " + ((m_provider == null) ? "none" : m_provider.getClass().getName())
					+ " - caching minutes: " + m_cacheSeconds / 60 + " - cache cleaner minutes: " + m_cacheCleanerSeconds / 60);
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * Returns to uninitialized state. You can use this method to release resources thet your Service allocated when Turbine shuts down.
	 */
	public void destroy()
	{
		m_storage.close();
		m_storage = null;
		m_provider = null;
		m_anon = null;

		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * UserDirectoryService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public User getUser(String id) throws UserNotDefinedException
	{
		// clean up the id
		id = cleanId(id);

		if (id == null) throw new UserNotDefinedException("null");

		// see if we've done this already in this thread
		String ref = userReference(id);
		UserEdit user = (UserEdit) m_threadLocalManager.get(ref);

		// if not
		if (user == null)
		{
			// check the cache
			if ((m_callCache != null) && (m_callCache.containsKey(ref)))
			{
				user = (UserEdit) m_callCache.get(ref);
			}

			else
			{
				// find our user record, and use it if we have it
				user = findUser(id);

				if ((user == null) && (m_provider != null))
				{
					// make a new edit to hold the provider's info, hoping it will be filled in
					user = new BaseUserEdit((String) null);
					((BaseUserEdit) user).m_id = id;

					if (!m_provider.getUser(user))
					{
						// it was not provided for, so clear back to null
						user = null;
					}
				}

				// if found, save it for later use in this thread
				if (user != null)
				{
					m_threadLocalManager.set(ref, user);

					// cache
					if (m_callCache != null) m_callCache.put(ref, user, m_cacheSeconds);
				}
			}
		}

		// if not found
		if (user == null)
		{
			throw new UserNotDefinedException(id);
		}

		// track it - Note: we are not tracking user access -ggolden
		// m_eventTrackingService.post(m_eventTrackingService.newEvent(SECURE_ACCESS_USER, user.getReference()));

		return user;
	}

	/**
	 * @inheritDoc
	 */
	public User getUserByEid(String eid) throws UserNotDefinedException
	{
		// TODO: cheating!

		// NOTE: need to cache by eid as well as id... -ggolden
		return getUser(eid);
	}

	/**
	 * @inheritDoc
	 */
	public List getUsers(Collection ids)
	{
		// TODO: make this more efficient? -ggolden

		// User objects to return
		List rv = new Vector();

		// a list of User (edits) setup to check with the provider
		Collection fromProvider = new Vector();

		// for each requested id
		for (Iterator i = ids.iterator(); i.hasNext();)
		{
			String id = (String) i.next();

			// clean up the id
			id = cleanId(id);

			// skip nulls
			if (id == null) continue;

			// see if we've done this already in this thread
			String ref = userReference(id);
			UserEdit user = (UserEdit) m_threadLocalManager.get(ref);

			// if not
			if (user == null)
			{
				// check the cache
				if ((m_callCache != null) && (m_callCache.containsKey(ref)))
				{
					user = (UserEdit) m_callCache.get(ref);
				}

				else
				{
					// find our user record, and use it if we have it
					user = findUser(id);

					// if we didn't find it locally, collect a list of externals to get
					if ((user == null) && (m_provider != null))
					{
						// make a new edit to hold the provider's info; the provider will either fill this in, if known, or remove it from the collection
						BaseUserEdit providerUser = new BaseUserEdit((String) null);
						providerUser.m_id = id;
						fromProvider.add(providerUser);
					}

					// if found, save it for later use in this thread
					if (user != null)
					{
						m_threadLocalManager.set(ref, user);

						// cache
						if (m_callCache != null) m_callCache.put(ref, user, m_cacheSeconds);
					}
				}
			}

			// if we found a user for this id, add it to the return
			if (user != null)
			{
				rv.add(user);
			}
		}

		// check the provider
		if ((m_provider != null) && (!fromProvider.isEmpty()))
		{
			m_provider.getUsers(fromProvider);

			// for each User in the collection that was filled in (and not removed) by the provider, cache and return it
			for (Iterator i = fromProvider.iterator(); i.hasNext();)
			{
				User user = (User) i.next();

				// cache, thread and call cache
				String ref = user.getReference();
				m_threadLocalManager.set(ref, user);
				if (m_callCache != null) m_callCache.put(ref, user, m_cacheSeconds);

				// add to return
				rv.add(user);
			}
		}

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public User getCurrentUser()
	{
		String id = m_sessionManager.getCurrentSessionUserId();

		// check current service caching - discard if the session user is different
		User rv = (User) m_threadLocalManager.get(M_curUserKey);
		if ((rv != null) && (rv.getId().equals(id))) return rv;

		try
		{
			rv = getUser(id);
		}
		catch (UserNotDefinedException e)
		{
			rv = getAnonymousUser();
		}

		// cache in the current service
		m_threadLocalManager.set(M_curUserKey, rv);

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowUpdateUser(String id)
	{
		// clean up the id
		id = cleanId(id);

		// is this the user's own?
		if (id.equals(m_sessionManager.getCurrentSessionUserId()))
		{
			// own or any
			return unlockCheck2(SECURE_UPDATE_USER_OWN, SECURE_UPDATE_USER_ANY, userReference(id));
		}

		else
		{
			// just any
			return unlockCheck(SECURE_UPDATE_USER_ANY, userReference(id));
		}
	}

	/**
	 * @inheritDoc
	 */
	public UserEdit editUser(String id) throws UserNotDefinedException, UserPermissionException, UserLockedException
	{
		// clean up the id
		id = cleanId(id);

		if (id == null) throw new UserNotDefinedException("null");

		// is this the user's own?
		String function = null;
		if (id.equals(m_sessionManager.getCurrentSessionUserId()))
		{
			// own or any
			unlock2(SECURE_UPDATE_USER_OWN, SECURE_UPDATE_USER_ANY, userReference(id));
			function = SECURE_UPDATE_USER_OWN;
		}
		else
		{
			// just any
			unlock(SECURE_UPDATE_USER_ANY, userReference(id));
			function = SECURE_UPDATE_USER_ANY;
		}

		// check for existance
		if (!m_storage.check(id))
		{
			throw new UserNotDefinedException(id);
		}

		// ignore the cache - get the user with a lock from the info store
		UserEdit user = m_storage.edit(id);
		if (user == null) throw new UserLockedException(id);

		((BaseUserEdit) user).setEvent(function);

		return user;
	}

	/**
	 * @inheritDoc
	 */
	public void commitEdit(UserEdit user)
	{
		// check for closed edit
		if (!user.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn("commitEdit(): closed UserEdit", e);
			}
			return;
		}

		// update the properties
		addLiveUpdateProperties((BaseUserEdit) user);

		// complete the edit
		m_storage.commit(user);

		// track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(((BaseUserEdit) user).getEvent(), user.getReference(), true));

		// close the edit object
		((BaseUserEdit) user).closeEdit();

		// %%% sync with portal service, i.e. user's portal page, any others? -ggolden
	}

	/**
	 * @inheritDoc
	 */
	public void cancelEdit(UserEdit user)
	{
		// check for closed edit
		if (!user.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn("cancelEdit(): closed UserEdit", e);
			}
			return;
		}

		// release the edit lock
		m_storage.cancel(user);

		// close the edit object
		((BaseUserEdit) user).closeEdit();
	}

	/**
	 * @inheritDoc
	 */
	public List getUsers()
	{
		List users = m_storage.getAll();
		return users;
	}

	/**
	 * @inheritDoc
	 */
	public List getUsers(int first, int last)
	{
		List all = m_storage.getAll(first, last);

		return all;
	}

	/**
	 * @inheritDoc
	 */
	public int countUsers()
	{
		return m_storage.count();
	}

	/**
	 * @inheritDoc
	 */
	public List searchUsers(String criteria, int first, int last)
	{
		return m_storage.search(criteria, first, last);
	}

	/**
	 * @inheritDoc
	 */
	public int countSearchUsers(String criteria)
	{
		return m_storage.countSearch(criteria);
	}

	/**
	 * @inheritDoc
	 */
	public Collection findUsersByEmail(String email)
	{
		// check internal users
		Collection users = m_storage.findUsersByEmail(email);

		// add in provider users
		if (m_provider != null)
		{
			// support UDP that has multiple users per email
			if (m_provider instanceof UsersShareEmailUDP)
			{
				Collection udpUsers = ((UsersShareEmailUDP) m_provider).findUsersByEmail(email, this);
				if (udpUsers != null) users.addAll(udpUsers);
			}

			// check for one
			else
			{
				// make a new edit to hold the provider's info
				UserEdit edit = new BaseUserEdit((String) null);
				if (m_provider.findUserByEmail(edit, email))
				{
					users.add((edit));
				}
			}
		}

		return users;
	}

	/**
	 * @inheritDoc
	 */
	public User getAnonymousUser()
	{
		return m_anon;
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowAddUser(String id)
	{
		// clean up the id
		id = cleanId(id);

		return unlockCheck(SECURE_ADD_USER, userReference(id));
	}

	/**
	 * @inheritDoc
	 */
	public UserEdit addUser(String id) throws UserIdInvalidException, UserAlreadyDefinedException, UserPermissionException
	{
		// clean up the id
		id = cleanId(id);

		// check for a valid user name
		Validator.checkUserId(id);

		// check security (throws if not permitted)
		unlock(SECURE_ADD_USER, userReference(id));

		// reserve a user with this id from the info store - if it's in use, this will return null
		UserEdit user = m_storage.put(id);
		if (user == null)
		{
			throw new UserAlreadyDefinedException(id);
		}

		((BaseUserEdit) user).setEvent(SECURE_ADD_USER);

		return user;
	}

	/**
	 * @inheritDoc
	 */
	public User addUser(String id, String firstName, String lastName, String email, String pw, String type,
			ResourceProperties properties) throws UserIdInvalidException, UserAlreadyDefinedException, UserPermissionException
	{
		// get it added
		UserEdit edit = addUser(id);

		// fill in the fields
		edit.setLastName(lastName);
		edit.setFirstName(firstName);
		edit.setEmail(email);
		edit.setPassword(pw);
		edit.setType(type);

		ResourcePropertiesEdit props = edit.getPropertiesEdit();
		if (properties != null)
		{
			props.addAll(properties);
		}

		// no live props!

		// get it committed - no further security check
		m_storage.commit(edit);

		// track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(((BaseUserEdit) edit).getEvent(), edit.getReference(), true));

		// close the edit object
		((BaseUserEdit) edit).closeEdit();

		return edit;
	}

	/**
	 * @inheritDoc
	 */
	public UserEdit mergeUser(Element el) throws UserIdInvalidException, UserAlreadyDefinedException, UserPermissionException
	{
		// construct from the XML
		User userFromXml = new BaseUserEdit(el);

		// check for a valid user name
		Validator.checkResourceId(userFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_USER, userFromXml.getReference());

		// reserve a user with this id from the info store - if it's in use, this will return null
		UserEdit user = m_storage.put(userFromXml.getId());
		if (user == null)
		{
			throw new UserAlreadyDefinedException(userFromXml.getId());
		}

		// transfer from the XML read user object to the UserEdit
		((BaseUserEdit) user).set(userFromXml);

		((BaseUserEdit) user).setEvent(SECURE_ADD_USER);

		return user;
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowRemoveUser(String id)
	{
		// clean up the id
		id = cleanId(id);

		return unlockCheck(SECURE_REMOVE_USER, userReference(id));
	}

	/**
	 * @inheritDoc
	 */
	public void removeUser(UserEdit user) throws UserPermissionException
	{
		// check for closed edit
		if (!user.isActiveEdit())
		{
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				M_log.warn("removeUser(): closed UserEdit", e);
			}
			return;
		}

		// check security (throws if not permitted)
		unlock(SECURE_REMOVE_USER, user.getReference());

		// complete the edit
		m_storage.remove(user);

		// track it
		m_eventTrackingService.post(m_eventTrackingService.newEvent(SECURE_REMOVE_USER, user.getReference(), true));

		// %%% sync with portal service, i.e. user's portal page, any others? -ggolden

		// close the edit object
		((BaseUserEdit) user).closeEdit();

		// remove any realm defined for this resource
		try
		{
			m_authzGroupService.removeAuthzGroup(m_authzGroupService.getAuthzGroup(user.getReference()));
		}
		catch (AuthzPermissionException e)
		{
			M_log.warn("removeUser: removing realm for : " + user.getReference() + " : " + e);
		}
		catch (GroupNotDefinedException ignore)
		{
		}
	}

	/**
	 * @inheritDoc
	 */
	public User authenticate(String eid, String password)
	{
		// clean up the id
		eid = cleanId(eid);
		if (eid == null) return null;

		boolean authenticated = false;

		// do we have a record for this user?
		UserEdit user = findUserByEid(eid);

		// TODO: when we implement eid / complete user records with unique UUID based id, we need to clean the following up:
		// we need to create a user record with this eid as an eid, with a new UUID id... -ggolden
		if (user == null)
		{
			String id = eid;
			if (m_provider != null && m_provider.createUserRecord(id))
			{
				try
				{
					user = addUser(id);
				}
				catch (UserIdInvalidException e)
				{
					M_log.debug("authenticate(): Id invalid: " + id);
				}
				catch (UserAlreadyDefinedException e)
				{
					M_log.debug("authenticate(): Id used: " + id);
				}
				catch (UserPermissionException e)
				{
					M_log.debug("authenticate(): UserPermissionException for adding user " + id);
				}
			}
		}

		if (m_provider != null && m_provider.authenticateWithProviderFirst(eid))
		{
			// 1. check provider
			authenticated = authenticateViaProvider(eid, user, password);
			if (!authenticated && user != null)
			{
				// 2. check our user record, if any, if not yet authenticated
				authenticated = user.checkPassword(password);
			}
		}
		else
		{
			// 1. check our user record, if any, if not yet authenticated
			if (user != null)
			{
				authenticated = user.checkPassword(password);
			}

			// 2. check our provider, if any, if not yet authenticated
			if (!authenticated && m_provider != null)
			{
				authenticated = authenticateViaProvider(eid, user, password);
			}
		}

		// if authenticated, get the user record to return - we might already have it
		User rv = null;
		if (authenticated)
		{
			rv = user;
			if (rv == null)
			{
				try
				{
					rv = getUserByEid(eid);
				}
				catch (UserNotDefinedException e)
				{
					// we might have authenticated by provider, but don't have proper
					// user "existance" (i.e. provider existance or internal user records) to let the user in -ggolden
					M_log.info("authenticate(): attempt by unknown user id: " + eid);
				}
				catch (Throwable e)
				{
					// we might have authenticated by provider, but don't have proper
					// user "existance" (i.e. provider existance or internal user records) to let the user in -ggolden
					M_log.warn("authenticate(): could not getUser() after auth: " + eid + " : " + e);
				}
			}

			// cache the user (if we didn't go through the getUser() above, which would have cached it
			else
			{
				if (m_callCache != null) m_callCache.put(userReference(rv.getId()), rv, m_cacheSeconds);
			}
		}

		return rv;
	}

	/**
	 * Authenticate user by provider information
	 * 
	 * @param id
	 *        The id string
	 * @param user
	 *        The UserEdit object
	 * @param password
	 *        The password string
	 * @return
	 */
	protected boolean authenticateViaProvider(String id, UserEdit user, String password)
	{
		boolean authenticated = m_provider.authenticateUser(id, user, password);
		// m_logger.info(" *** UserDirectory.authenticate: id: " + id + " result of PUDP.authenticateUser: " + authenticated);

		// some providers want to update the user record on authentication - if so, we need to save it
		if ((authenticated) && (m_provider.updateUserAfterAuthentication()) && user != null)
		{
			// save user
			BaseUserEdit edit = (BaseUserEdit) m_storage.edit(id);
			if (edit != null)
			{
				edit.setAll(user);
				edit.setEvent(SECURE_UPDATE_USER_ANY);

				m_storage.commit(edit);

				m_eventTrackingService.post(m_eventTrackingService.newEvent(edit.getEvent(), edit.getReference(), true));
			}
			else
			{
				M_log.warn("authenticate(): could not save user after auth: " + id);
			}
		}
		return authenticated;
	}

	/**
	 * @inheritDoc
	 */
	public void destroyAuthentication()
	{
		// let the provider know
		if (m_provider != null)
		{
			m_provider.destroyAuthentication();
		}
	}

	/**
	 * Find the user object, in cache or storage (only - no provider check).
	 * 
	 * @param id
	 *        The user id.
	 * @return The user object found in cache or storage, or null if not found.
	 */
	protected BaseUserEdit findUser(String id)
	{
		BaseUserEdit user = (BaseUserEdit) m_storage.get(id);

		return user;
	}

	/**
	 * Find the user object, by the eid (not id) in cache or storage (only - no provider check).
	 * 
	 * @param id
	 *        The user id.
	 * @return The user object found in cache or storage, or null if not found.
	 */
	protected BaseUserEdit findUserByEid(String eid)
	{
		// TODO: implement this!
		return findUser(eid);
	}

	/**
	 * Create the live properties for the user.
	 */
	protected void addLiveProperties(BaseUserEdit edit)
	{
		String current = m_sessionManager.getCurrentSessionUserId();

		edit.m_createdUserId = current;
		edit.m_lastModifiedUserId = current;

		Time now = m_timeService.newTime();
		edit.m_createdTime = now;
		edit.m_lastModifiedTime = (Time) now.clone();
	}

	/**
	 * Update the live properties for a user for when modified.
	 */
	protected void addLiveUpdateProperties(BaseUserEdit edit)
	{
		String current = m_sessionManager.getCurrentSessionUserId();

		edit.m_lastModifiedUserId = current;
		edit.m_lastModifiedTime = m_timeService.newTime();
	}

	/**
	 * Adjust the id - trim it to null, and lower case IF we are case insensitive.
	 * 
	 * @param id
	 *        The id to clean up.
	 * @return A cleaned up id.
	 */
	protected String cleanId(String id)
	{
		if (!m_caseSensitiveId)
		{
			return StringUtil.trimToNullLower(id);
		}

		return StringUtil.trimToNull(id);
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * EntityProducer implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public String getLabel()
	{
		return "user";
	}

	/**
	 * @inheritDoc
	 */
	public boolean willArchiveMerge()
	{
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public boolean willImport()
	{
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public HttpAccess getHttpAccess()
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		// for user access
		if (reference.startsWith(REFERENCE_ROOT))
		{
			String id = null;

			// we will get null, service, userId
			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);

			if (parts.length > 2)
			{
				id = parts[2];
			}

			ref.set(SERVICE_NAME, null, id, null, null);

			return true;
		}

		return false;
	}

	/**
	 * @inheritDoc
	 */
	public String getEntityDescription(Reference ref)
	{
		// double check that it's mine
		if (SERVICE_NAME != ref.getType()) return null;

		String rv = "User: " + ref.getReference();

		try
		{
			User user = getUser(ref.getId());
			rv = "User: " + user.getDisplayName();
		}
		catch (UserNotDefinedException e)
		{
		}
		catch (NullPointerException e)
		{
		}

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity getEntity(Reference ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Collection getEntityAuthzGroups(Reference ref)
	{
		// double check that it's mine
		if (SERVICE_NAME != ref.getType()) return null;

		Collection rv = new Vector();

		// for user access: user and template realms
		try
		{
			rv.add(userReference(ref.getId()));

			ref.addUserTemplateAuthzGroup(rv, m_sessionManager.getCurrentSessionUserId());
		}
		catch (NullPointerException e)
		{
			M_log.warn("getEntityRealms(): " + e);
		}

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public String getEntityUrl(Reference ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		return "";
	}

	/**
	 * @inheritDoc
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans,
			Set userListAllowImport)
	{
		return "";
	}

	/**
	 * @inheritDoc
	 */
	public void importEntities(String fromContext, String toContext, List ids)
	{
	}

	/**
	 * @inheritDoc
	 */
	public void syncWithSiteChange(Object site, EntityProducer.ChangeType change)
	{
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * UserFactory implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public UserEdit newUser()
	{
		return new BaseUserEdit((String) null);
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * UserEdit implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * <p>
	 * BaseUserEdit is an implementation of the UserEdit object.
	 * </p>
	 */
	public class BaseUserEdit implements UserEdit, SessionBindingListener
	{
		/** The event code for this edit. */
		protected String m_event = null;

		/** Active flag. */
		protected boolean m_active = false;

		/** The user id. */
		protected String m_id = null;

		/** The user first name. */
		protected String m_firstName = null;

		/** The user last name. */
		protected String m_lastName = null;

		/** The user email address. */
		protected String m_email = null;

		/** The user password. */
		protected String m_pw = null;

		/** The properties. */
		protected ResourcePropertiesEdit m_properties = null;

		/** The user type. */
		protected String m_type = null;

		/** The created user id. */
		protected String m_createdUserId = null;

		/** The last modified user id. */
		protected String m_lastModifiedUserId = null;

		/** The time created. */
		protected Time m_createdTime = null;

		/** The time last modified. */
		protected Time m_lastModifiedTime = null;

		/**
		 * Construct.
		 * 
		 * @param id
		 *        The user id.
		 */
		public BaseUserEdit(String id)
		{
			m_id = id;

			// setup for properties
			ResourcePropertiesEdit props = new BaseResourcePropertiesEdit();
			m_properties = props;

			// if the id is not null (a new user, rather than a reconstruction)
			// and not the anon (id == "") user,
			// add the automatic (live) properties
			if ((m_id != null) && (m_id.length() > 0)) addLiveProperties(this);
		}

		/**
		 * Construct from another User object.
		 * 
		 * @param user
		 *        The user object to use for values.
		 */
		public BaseUserEdit(User user)
		{
			setAll(user);
		}

		/**
		 * Construct from information in XML.
		 * 
		 * @param el
		 *        The XML DOM Element definining the user.
		 */
		public BaseUserEdit(Element el)
		{
			// setup for properties
			m_properties = new BaseResourcePropertiesEdit();

			m_id = cleanId(el.getAttribute("id"));
			m_firstName = StringUtil.trimToNull(el.getAttribute("first-name"));
			m_lastName = StringUtil.trimToNull(el.getAttribute("last-name"));
			setEmail(StringUtil.trimToNull(el.getAttribute("email")));
			m_pw = el.getAttribute("pw");
			m_type = StringUtil.trimToNull(el.getAttribute("type"));
			m_createdUserId = StringUtil.trimToNull(el.getAttribute("created-id"));
			m_lastModifiedUserId = StringUtil.trimToNull(el.getAttribute("modified-id"));

			String time = StringUtil.trimToNull(el.getAttribute("created-time"));
			if (time != null)
			{
				m_createdTime = m_timeService.newTimeGmt(time);
			}

			time = StringUtil.trimToNull(el.getAttribute("modified-time"));
			if (time != null)
			{
				m_lastModifiedTime = m_timeService.newTimeGmt(time);
			}

			// the children (roles, properties)
			NodeList children = el.getChildNodes();
			final int length = children.getLength();
			for (int i = 0; i < length; i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element) child;

				// look for properties
				if (element.getTagName().equals("properties"))
				{
					// re-create properties
					m_properties = new BaseResourcePropertiesEdit(element);

					// pull out some properties into fields to convert old (pre 1.38) versions
					if (m_createdUserId == null)
					{
						m_createdUserId = m_properties.getProperty("CHEF:creator");
					}
					if (m_lastModifiedUserId == null)
					{
						m_lastModifiedUserId = m_properties.getProperty("CHEF:modifiedby");
					}
					if (m_createdTime == null)
					{
						try
						{
							m_createdTime = m_properties.getTimeProperty("DAV:creationdate");
						}
						catch (Exception ignore)
						{
						}
					}
					if (m_lastModifiedTime == null)
					{
						try
						{
							m_lastModifiedTime = m_properties.getTimeProperty("DAV:getlastmodified");
						}
						catch (Exception ignore)
						{
						}
					}
					m_properties.removeProperty("CHEF:creator");
					m_properties.removeProperty("CHEF:modifiedby");
					m_properties.removeProperty("DAV:creationdate");
					m_properties.removeProperty("DAV:getlastmodified");
				}
			}
		}

		/**
		 * ReConstruct.
		 * 
		 * @param id
		 *        The id.
		 * @param email
		 *        The email.
		 * @param firstName
		 *        The first name.
		 * @param lastName
		 *        The last name.
		 * @param type
		 *        The type.
		 * @param pw
		 *        The password.
		 * @param createdBy
		 *        The createdBy property.
		 * @param createdOn
		 *        The createdOn property.
		 * @param modifiedBy
		 *        The modified by property.
		 * @param modifiedOn
		 *        The modified on property.
		 */
		public BaseUserEdit(String id, String email, String firstName, String lastName, String type, String pw, String createdBy,
				Time createdOn, String modifiedBy, Time modifiedOn)
		{
			m_id = id;
			m_firstName = firstName;
			m_lastName = lastName;
			m_type = type;
			setEmail(email);
			m_pw = pw;
			m_createdUserId = createdBy;
			m_lastModifiedUserId = modifiedBy;
			m_createdTime = createdOn;
			m_lastModifiedTime = modifiedOn;

			// setup for properties, but mark them lazy since we have not yet established them from data
			BaseResourcePropertiesEdit props = new BaseResourcePropertiesEdit();
			props.setLazy(true);
			m_properties = props;
		}

		/**
		 * Take all values from this object.
		 * 
		 * @param user
		 *        The user object to take values from.
		 */
		protected void setAll(User user)
		{
			m_id = user.getId();
			m_firstName = user.getFirstName();
			m_lastName = user.getLastName();
			m_type = user.getType();
			setEmail(user.getEmail());
			m_pw = ((BaseUserEdit) user).m_pw;
			m_createdUserId = ((BaseUserEdit) user).m_createdUserId;
			m_lastModifiedUserId = ((BaseUserEdit) user).m_lastModifiedUserId;
			if (((BaseUserEdit) user).m_createdTime != null) m_createdTime = (Time) ((BaseUserEdit) user).m_createdTime.clone();
			if (((BaseUserEdit) user).m_lastModifiedTime != null)
				m_lastModifiedTime = (Time) ((BaseUserEdit) user).m_lastModifiedTime.clone();

			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(user.getProperties());
			((BaseResourcePropertiesEdit) m_properties).setLazy(((BaseResourceProperties) user.getProperties()).isLazy());
		}

		/**
		 * @inheritDoc
		 */
		public Element toXml(Document doc, Stack stack)
		{
			Element user = doc.createElement("user");

			if (stack.isEmpty())
			{
				doc.appendChild(user);
			}
			else
			{
				((Element) stack.peek()).appendChild(user);
			}

			stack.push(user);

			user.setAttribute("id", getId());
			if (m_firstName != null) user.setAttribute("first-name", m_firstName);
			if (m_lastName != null) user.setAttribute("last-name", m_lastName);
			if (m_type != null) user.setAttribute("type", m_type);
			user.setAttribute("email", getEmail());
			user.setAttribute("pw", m_pw);
			user.setAttribute("created-id", m_createdUserId);
			user.setAttribute("modified-id", m_lastModifiedUserId);
			user.setAttribute("created-time", m_createdTime.toString());
			user.setAttribute("modified-time", m_lastModifiedTime.toString());

			// properties
			getProperties().toXml(doc, stack);

			stack.pop();

			return user;
		}

		/**
		 * @inheritDoc
		 */
		public String getId()
		{
			if (m_id == null) return "";
			return m_id;
		}

		/**
		 * @inheritDoc
		 */
		public String getEid()
		{
			// TODO: implement me, no cheating!
			return getId();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl()
		{
			return getAccessPoint(false) + m_id;
		}

		/**
		 * @inheritDoc
		 */
		public String getReference()
		{
			return userReference(m_id);
		}

		/**
		 * @inheritDoc
		 */
		public String getReference(String rootProperty)
		{
			return getReference();
		}

		/**
		 * @inheritDoc
		 */
		public String getUrl(String rootProperty)
		{
			return getUrl();
		}

		/**
		 * @inheritDoc
		 */
		public ResourceProperties getProperties()
		{
			// if lazy, resolve
			if (((BaseResourceProperties) m_properties).isLazy())
			{
				((BaseResourcePropertiesEdit) m_properties).setLazy(false);
				m_storage.readProperties(this, m_properties);
			}

			return m_properties;
		}

		/**
		 * @inheritDoc
		 */
		public User getCreatedBy()
		{
			try
			{
				return getUser(m_createdUserId);
			}
			catch (Exception e)
			{
				return getAnonymousUser();
			}
		}

		/**
		 * @inheritDoc
		 */
		public User getModifiedBy()
		{
			try
			{
				return getUser(m_lastModifiedUserId);
			}
			catch (Exception e)
			{
				return getAnonymousUser();
			}
		}

		/**
		 * @inheritDoc
		 */
		public Time getCreatedTime()
		{
			return m_createdTime;
		}

		/**
		 * @inheritDoc
		 */
		public Time getModifiedTime()
		{
			return m_lastModifiedTime;
		}

		/**
		 * @inheritDoc
		 */
		public String getDisplayName()
		{
			StringBuffer buf = new StringBuffer(128);
			if (m_firstName != null) buf.append(m_firstName);
			if (m_lastName != null)
			{
				buf.append(" ");
				buf.append(m_lastName);
			}

			if (buf.length() == 0) return getId();

			return buf.toString();
		}

		/**
		 * @inheritDoc
		 */
		public String getFirstName()
		{
			if (m_firstName == null) return "";
			return m_firstName;
		}

		/**
		 * @inheritDoc
		 */
		public String getLastName()
		{
			if (m_lastName == null) return "";
			return m_lastName;
		}

		/**
		 * @inheritDoc
		 */
		public String getSortName()
		{
			StringBuffer buf = new StringBuffer(128);
			if (m_lastName != null) buf.append(m_lastName);
			if (m_firstName != null)
			{
				buf.append(", ");
				buf.append(m_firstName);
			}

			if (buf.length() == 0) return getId();

			return buf.toString();
		}

		/**
		 * @inheritDoc
		 */
		public String getEmail()
		{
			if (m_email == null) return "";
			return m_email;
		}

		/**
		 * @inheritDoc
		 */
		public String getType()
		{
			return m_type;
		}

		/**
		 * @inheritDoc
		 */
		public boolean checkPassword(String pw)
		{
			pw = StringUtil.trimToNull(pw);

			// if we have no password, or none is given, we fail
			if ((m_pw == null) || (pw == null)) return false;

			// encode this password
			String encoded = OneWayHash.encode(pw);

			if (m_pw.equals(encoded)) return true;

			return false;
		}

		/**
		 * @inheritDoc
		 */
		public boolean equals(Object obj)
		{
			if (!(obj instanceof User)) return false;
			return ((User) obj).getId().equals(getId());
		}

		/**
		 * @inheritDoc
		 */
		public int hashCode()
		{
			return getId().hashCode();
		}

		/**
		 * @inheritDoc
		 */
		public int compareTo(Object obj)
		{
			if (!(obj instanceof User)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their sort names
			int compare = getSortName().compareTo(((User) obj).getSortName());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((User) obj).getId());
			}

			return compare;
		}

		/**
		 * Clean up.
		 */
		protected void finalize()
		{
			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
		}

		/**
		 * @inheritDoc
		 */
		public void setId(String id)
		{
			if (m_id == null)
			{
				m_id = id;
			}
		}

		/**
		 * @inheritDoc
		 */
		public void setEid(String eid)
		{
			// TODO: implement me!
		}

		/**
		 * @inheritDoc
		 */
		public void setFirstName(String name)
		{
			m_firstName = name;
		}

		/**
		 * @inheritDoc
		 */
		public void setLastName(String name)
		{
			m_lastName = name;
		}

		/**
		 * @inheritDoc
		 */
		public void setEmail(String email)
		{
			m_email = email;
		}

		/**
		 * @inheritDoc
		 */
		public void setPassword(String pw)
		{
			// encode this password
			String encoded = OneWayHash.encode(pw);

			m_pw = encoded;
		}

		/**
		 * @inheritDoc
		 */
		public void setType(String type)
		{
			m_type = type;
		}

		/**
		 * Take all values from this object.
		 * 
		 * @param user
		 *        The user object to take values from.
		 */
		protected void set(User user)
		{
			setAll(user);
		}

		/**
		 * Access the event code for this edit.
		 * 
		 * @return The event code for this edit.
		 */
		protected String getEvent()
		{
			return m_event;
		}

		/**
		 * Set the event code for this edit.
		 * 
		 * @param event
		 *        The event code for this edit.
		 */
		protected void setEvent(String event)
		{
			m_event = event;
		}

		/**
		 * @inheritDoc
		 */
		public ResourcePropertiesEdit getPropertiesEdit()
		{
			// if lazy, resolve
			if (((BaseResourceProperties) m_properties).isLazy())
			{
				((BaseResourcePropertiesEdit) m_properties).setLazy(false);
				m_storage.readProperties(this, m_properties);
			}

			return m_properties;
		}

		/**
		 * Enable editing.
		 */
		protected void activate()
		{
			m_active = true;
		}

		/**
		 * @inheritDoc
		 */
		public boolean isActiveEdit()
		{
			return m_active;
		}

		/**
		 * Close the edit object - it cannot be used after this.
		 */
		protected void closeEdit()
		{
			m_active = false;
		}

		/**
		 * Check this User object to see if it is selected by the criteria.
		 * 
		 * @param criteria
		 *        The critera.
		 * @return True if the User object is selected by the criteria, false if not.
		 */
		protected boolean selectedBy(String criteria)
		{
			if (StringUtil.containsIgnoreCase(getSortName(), criteria) || StringUtil.containsIgnoreCase(getDisplayName(), criteria)
					|| StringUtil.containsIgnoreCase(getId(), criteria) || StringUtil.containsIgnoreCase(getEmail(), criteria))
			{
				return true;
			}

			return false;
		}

		/******************************************************************************************************************************************************************************************************************************************************
		 * SessionBindingListener implementation
		 *****************************************************************************************************************************************************************************************************************************************************/

		/**
		 * @inheritDoc
		 */
		public void valueBound(SessionBindingEvent event)
		{
		}

		/**
		 * @inheritDoc
		 */
		public void valueUnbound(SessionBindingEvent event)
		{
			if (M_log.isDebugEnabled()) M_log.debug("valueUnbound()");

			// catch the case where an edit was made but never resolved
			if (m_active)
			{
				cancelEdit(this);
			}
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Storage
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected interface Storage
	{
		/**
		 * Open.
		 */
		public void open();

		/**
		 * Close.
		 */
		public void close();

		/**
		 * Check if a user by this id exists.
		 * 
		 * @param id
		 *        The user id.
		 * @return true if a user by this id exists, false if not.
		 */
		public boolean check(String id);

		/**
		 * Get the user with this id, or null if not found.
		 * 
		 * @param id
		 *        The user id.
		 * @return The user with this id, or null if not found.
		 */
		public UserEdit get(String id);

		/**
		 * Get the users with this email, or return empty if none found.
		 * 
		 * @param id
		 *        The user email.
		 * @return The Collection (User) of users with this email, or an empty collection if none found.
		 */
		public Collection findUsersByEmail(String email);

		/**
		 * Get all users.
		 * 
		 * @return The List (UserEdit) of all users.
		 */
		public List getAll();

		/**
		 * Get all the users in record range.
		 * 
		 * @param first
		 *        The first record position to return.
		 * @param last
		 *        The last record position to return.
		 * @return The List (BaseUserEdit) of all users.
		 */
		public List getAll(int first, int last);

		/**
		 * Count all the users.
		 * 
		 * @return The count of all users.
		 */
		public int count();

		/**
		 * Search for users with id or email, first or last name matching criteria, in range.
		 * 
		 * @param criteria
		 *        The search criteria.
		 * @param first
		 *        The first record position to return.
		 * @param last
		 *        The last record position to return.
		 * @return The List (BaseUserEdit) of all alias.
		 */
		public List search(String criteria, int first, int last);

		/**
		 * Count all the users with id or email, first or last name matching criteria.
		 * 
		 * @param criteria
		 *        The search criteria.
		 * @return The count of all aliases with id or target matching criteria.
		 */
		public int countSearch(String criteria);

		/**
		 * Add a new user with this id.
		 * 
		 * @param id
		 *        The user id.
		 * @return The locked User object with this id, or null if the id is in use.
		 */
		public UserEdit put(String id);

		/**
		 * Get a lock on the user with this id, or null if a lock cannot be gotten.
		 * 
		 * @param id
		 *        The user id.
		 * @return The locked User with this id, or null if this records cannot be locked.
		 */
		public UserEdit edit(String id);

		/**
		 * Commit the changes and release the lock.
		 * 
		 * @param user
		 *        The user to commit.
		 */
		public void commit(UserEdit user);

		/**
		 * Cancel the changes and release the lock.
		 * 
		 * @param user
		 *        The user to commit.
		 */
		public void cancel(UserEdit user);

		/**
		 * Remove this user.
		 * 
		 * @param user
		 *        The user to remove.
		 */
		public void remove(UserEdit user);

		/**
		 * Read properties from storage into the edit's properties.
		 * 
		 * @param edit
		 *        The user to read properties for.
		 */
		public void readProperties(UserEdit edit, ResourcePropertiesEdit props);
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * StorageUser implementation (no container)
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public Entity newContainer(String ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity newContainer(Element element)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity newContainer(Entity other)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Entity newResource(Entity container, String id, Object[] others)
	{
		return new BaseUserEdit(id);
	}

	/**
	 * @inheritDoc
	 */
	public Entity newResource(Entity container, Element element)
	{
		return new BaseUserEdit(element);
	}

	/**
	 * @inheritDoc
	 */
	public Entity newResource(Entity container, Entity other)
	{
		return new BaseUserEdit((User) other);
	}

	/**
	 * @inheritDoc
	 */
	public Edit newContainerEdit(String ref)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newContainerEdit(Element element)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newContainerEdit(Entity other)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newResourceEdit(Entity container, String id, Object[] others)
	{
		BaseUserEdit e = new BaseUserEdit(id);
		e.activate();
		return e;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newResourceEdit(Entity container, Element element)
	{
		BaseUserEdit e = new BaseUserEdit(element);
		e.activate();
		return e;
	}

	/**
	 * @inheritDoc
	 */
	public Edit newResourceEdit(Entity container, Entity other)
	{
		BaseUserEdit e = new BaseUserEdit((User) other);
		e.activate();
		return e;
	}

	/**
	 * @inheritDoc
	 */
	public Object[] storageFields(Entity r)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isDraft(Entity r)
	{
		return false;
	}

	/**
	 * @inheritDoc
	 */
	public String getOwnerId(Entity r)
	{
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Time getDate(Entity r)
	{
		return null;
	}
}
