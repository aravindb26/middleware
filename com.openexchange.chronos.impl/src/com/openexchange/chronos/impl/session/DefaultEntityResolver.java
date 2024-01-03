/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.chronos.impl.session;

import static com.openexchange.chronos.common.CalendarUtils.getURI;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.optEMailAddress;
import static com.openexchange.chronos.common.CalendarUtils.optTimeZone;
import static com.openexchange.java.Autoboxing.Coll2i;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Autoboxing.i2I;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.ResourceId;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tools.alias.UserAliasUtility;
import com.openexchange.java.Strings;
import com.openexchange.java.util.TimeZones;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.ResourcePermissionUtility;
import com.openexchange.resource.ResourceService;
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link DefaultEntityResolver}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DefaultEntityResolver implements EntityResolver {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultEntityResolver.class);

    private final ServiceLookup services;
    private final Context context;
    private final Map<Integer, Group> knownGroups;
    private final Map<Integer, User> knownUsers;
    private final Map<Integer, Resource> knownResources;
    private final Map<Integer, FolderObject> knownFolders;

    /**
     * Initializes a new {@link DefaultEntityResolver}.
     *
     * @param session The underlying session
     * @param services A service lookup reference
     */
    public DefaultEntityResolver(ServerSession session, ServiceLookup services) {
        this(session.getContext(), services);
        knownUsers.put(I(session.getUserId()), session.getUser());
    }

    /**
     * Initializes a new {@link DefaultEntityResolver}.
     *
     * @param contextId The context identifier
     * @param services A service lookup reference
     */
    public DefaultEntityResolver(int contextId, ServiceLookup services) throws OXException {
        this(services.getService(ContextService.class).loadContext(contextId), services);
    }

    /**
     * Initializes a new {@link DefaultEntityResolver}.
     *
     * @param context The context
     * @param services A service lookup reference
     */
    public DefaultEntityResolver(Context context, ServiceLookup services) {
        super();
        this.services = services;
        this.context = context;
        knownUsers = new HashMap<Integer, User>();
        knownGroups = new HashMap<Integer, Group>();
        knownResources = new HashMap<Integer, Resource>();
        knownFolders = new HashMap<Integer, FolderObject>();
    }

    @Override
    public List<Attendee> prepare(List<Attendee> attendees) throws OXException {
        return prepare(attendees, false);
    }

    @Override
    public List<Attendee> prepare(List<Attendee> attendees, boolean resolveResourceIds) throws OXException {
        if (null != attendees) {
            for (Attendee attendee : attendees) {
                /*
                 * try and resolve external calendar user address to internal calendar user, enhance with static properties
                 */
                applyEntityData(resolveExternals(attendee, attendee.getCuType(), resolveResourceIds, null));
            }
        }
        return attendees;
    }

    @Override
    public Attendee prepare(Attendee attendee, boolean resolveResourceIds) throws OXException {
        if (null != attendee) {
            /*
             * copy attendee, then try and resolve external calendar user address to internal calendar user & enhance with static properties
             */
            Attendee attendeeCopy = AttendeeMapper.getInstance().copy(attendee, null, (AttendeeField[]) null);
            return applyEntityData(resolveExternals(attendeeCopy, attendeeCopy.getCuType(), resolveResourceIds, null));
        }
        return attendee;
    }

    @Override
    public List<Attendee> prepare(List<Attendee> attendees, int[] resolvableEntities) throws OXException {
        if (null != attendees) {
            for (Attendee attendee : attendees) {
                /*
                 * try and resolve external calendar user address to internal calendar user, enhance with static properties
                 */
                applyEntityData(resolveExternals(attendee, attendee.getCuType(), true, resolvableEntities));
            }
        }
        return attendees;
    }

    @Override
    public <T extends CalendarUser> T prepare(T calendarUser, CalendarUserType cuType) throws OXException {
        return prepare(calendarUser, cuType, false, null);
    }

    @Override
    public <T extends CalendarUser> T prepare(T calendarUser, CalendarUserType cuType, int[] resolvableEntities) throws OXException {
        return prepare(calendarUser, cuType, false, resolvableEntities);
    }

    public <T extends CalendarUser> T prepare(T calendarUser, CalendarUserType cuType, boolean resolveResourceIds, int[] resolvableEntities) throws OXException {
        if (null == calendarUser) {
            return null;
        }
        /*
         * try and resolve external calendar user address to internal calendar user, enhance with static properties
         */
        return applyEntityData(resolveExternals(calendarUser, cuType, resolveResourceIds, resolvableEntities), cuType);
    }

    @Override
    public int[] getGroupMembers(int groupID) throws OXException {
        return getGroup(groupID).getMember();
    }

    @Override
    public int[] getBookingDelegates(int resourceID) throws OXException {
        Set<Integer> userIds = new HashSet<Integer>();
        ResourcePermission[] permissions = getResource(resourceID, null).getPermissions();
        if (null != permissions) {
            for (ResourcePermission permission : permissions) {
                if (SchedulingPrivilege.DELEGATE.equals(permission.getSchedulingPrivilege())) {
                    if (permission.isGroup()) {
                        for (int member : getGroup(permission.getEntity()).getMember()) {
                            userIds.add(I(member));
                        }
                    } else {
                        userIds.add(I(permission.getEntity()));
                    }
                }
            }
        }
        return Coll2i(userIds);
    }

    @Override
    public int[] getResourcesWithDelegatePrivilege(int userID) throws OXException {
        /*
         * lookup resource the user has delegate scheduling privileges for
         */
        Resource[] resources = services.getServiceSafe(ResourceService.class)
            .searchResourcesWithPrivilege(getUser(userID, null), context, SchedulingPrivilege.DELEGATE);
        if (null == resources || 0 == resources.length) {
            return new int[0];
        }
        /*
         * cache loaded resources and generate resulting array
         */
        int[] resourceIds = new int[resources.length];
        for (int i = 0; i < resourceIds.length; i++) {
            Resource resource = resources[i];
            knownResources.put(I(resource.getIdentifier()), resource);
            resourceIds[i] = resource.getIdentifier();
        }
        return resourceIds;
    }

    @Override
    public SchedulingPrivilege getSchedulingPrivilege(int resourceID, int userID) throws OXException {
        return ResourcePermissionUtility.getEffectivePrivilege(getResource(resourceID, null), getUser(userID, null));
    }

    @Override
    public TimeZone getTimeZone(int userID) throws OXException {
        return optTimeZone(getUser(userID, null).getTimeZone(), TimeZones.UTC);
    }

    @Override
    public Locale getLocale(int userID) throws OXException {
        return getUser(userID, null).getLocale();
    }

    @Override
    public int getContactId(int userID) throws OXException {
        return getUser(userID, null).getContactId();
    }

    @Override
    public Attendee prepareUserAttendee(int userID) throws OXException {
        return applyEntityData(new Attendee(), getUser(userID, null));
    }

    @Override
    public Attendee prepareGroupAttendee(int groupID) throws OXException {
        return applyEntityData(new Attendee(), getGroup(groupID));
    }

    @Override
    public Attendee prepareResourceAttendee(int resourceID) throws OXException {
        return applyEntityData(new Attendee(), getResource(resourceID, null));
    }

    @Override
    public CalendarUserType probeCUType(int entity) throws OXException {
        if (null != optUser(entity, null)) {
            return CalendarUserType.INDIVIDUAL;
        }
        if (null != optGroup(entity)) {
            return CalendarUserType.GROUP;
        }
        if (null != optResource(entity, null)) {
            return CalendarUserType.RESOURCE;
        }
        return null;
    }

    @Override
    public <T extends CalendarUser> T applyEntityData(T calendarUser, int userID) throws OXException {
        return applyEntityData(calendarUser, getUser(userID, null));
    }

    @Override
    public Attendee applyEntityData(Attendee attendee, Connection connection) throws OXException {
        if (null == attendee) {
            LOG.warn("Ignoring attempt to apply entity data for passed null reference");
            return attendee;
        }
        return applyEntityData(attendee, attendee.getCuType(), connection);
    }

    @Override
    public <T extends CalendarUser> T applyEntityData(T calendarUser, CalendarUserType cuType, Connection connection) throws OXException {
        if (null == calendarUser) {
            LOG.warn("Ignoring attempt to apply entity data for passed null reference");
            return calendarUser;
        }
        if (isInternal(calendarUser, cuType)) {
            /*
             * apply known entity data for internal attendees
             */
            if (CalendarUserType.GROUP.equals(cuType)) {
                applyEntityData(calendarUser, getGroup(calendarUser.getEntity()));
            } else if (CalendarUserType.RESOURCE.equals(cuType) || CalendarUserType.ROOM.equals(cuType)) {
                applyEntityData(calendarUser, getResource(calendarUser.getEntity(), connection));
            } else {
                applyEntityData(calendarUser, getUser(calendarUser.getEntity(), connection));
            }
        } else {
            /*
             * copy over email address for external attendees
             */
            if (null == calendarUser.getEMail()) {
                calendarUser.setEMail(optEMailAddress(calendarUser.getUri()));
            }
        }
        /*
         * do the same with a proxy calendar user in "sent-by"
         */
        if (null != calendarUser.getSentBy()) {
            try {
                applyEntityData(calendarUser.getSentBy(), CalendarUserType.INDIVIDUAL);
            } catch (OXException e) {
                if (CalendarExceptionCodes.INVALID_CALENDAR_USER.equals(e)) {
                    LOG.debug("Ignoring invalid proxy {} for SENT-BY property of {}.", calendarUser.getSentBy(), calendarUser, e);
                    calendarUser.setSentBy(null);
                } else {
                    throw e;
                }
            }
        }
        return calendarUser;
    }

    @Override
    public void prefetch(List<Attendee> attendees) {
        Set<Integer> usersToLoad = new HashSet<Integer>();
        Set<Integer> groupsToLoad = new HashSet<Integer>();
        Set<Integer> resourcesToLoad = new HashSet<Integer>();
        for (Attendee attendee : attendees) {
            if (isInternal(attendee)) {
                Integer id = I(attendee.getEntity());
                if (CalendarUserType.GROUP.equals(attendee.getCuType())) {
                    if (false == knownGroups.containsKey(id)) {
                        groupsToLoad.add(id);
                    }
                } else if (CalendarUserType.RESOURCE.equals(attendee.getCuType()) || CalendarUserType.ROOM.equals(attendee.getCuType())) {
                    if (false == knownResources.containsKey(id)) {
                        resourcesToLoad.add(id);
                    }
                } else {
                    if (false == knownUsers.containsKey(id)) {
                        usersToLoad.add(id);
                    }
                }
            }
        }
        if (0 < resourcesToLoad.size()) {
            for (Integer resourceID : resourcesToLoad) {
                try {
                    knownResources.put(resourceID, loadResource(i(resourceID), null));
                } catch (OXException e) {
                    LOG.debug("Error loading resource with id {}, skipping during pre-fetch.", resourceID, e);
                }
            }
        }
        if (0 < groupsToLoad.size()) {
            for (Integer groupID : groupsToLoad) {
                try {
                    Group group = loadGroup(i(groupID));
                    knownGroups.put(groupID, group);
                    usersToLoad.addAll(java.util.Arrays.asList(i2I(group.getMember())));
                } catch (OXException e) {
                    LOG.debug("Error loading resource with id {}, skipping during pre-fetch.", groupID, e);
                }
            }
        }
        if (0 < usersToLoad.size()) {
            try {
                User[] users = loadUsers(I2i(usersToLoad));
                for (User user : users) {
                    knownUsers.put(I(user.getId()), user);
                }
            } catch (OXException e) {
                LOG.debug("Error loading users with ids {}, skipping during pre-fetch.", usersToLoad, e);
            }
        }
    }

    @Override
    public int getContextID() {
        return context.getContextId();
    }

    @Override
    public void invalidate() {
        knownGroups.clear();
        knownResources.clear();
        knownUsers.clear();
        knownFolders.clear();
    }

    private Group getGroup(int entity) throws OXException {
        Integer id = I(entity);
        Group group = knownGroups.get(id);
        if (null == group) {
            group = loadGroup(entity);
            knownGroups.put(id, group);
        }
        return group;
    }

    private Group optGroup(int entity) throws OXException {
        Integer id = I(entity);
        Group group = knownGroups.get(id);
        if (null == group) {
            try {
                group = loadGroup(entity);
            } catch (OXException e) {
                if (CalendarExceptionCodes.INVALID_CALENDAR_USER.equals(e)) {
                    return null;
                }
                throw e;
            }
            knownGroups.put(id, group);
        }
        return group;
    }

    private User getUser(int entity, Connection optConnection) throws OXException {
        Integer id = I(entity);
        User user = knownUsers.get(id);
        if (null == user) {
            user = loadUser(entity, optConnection);
            knownUsers.put(id, user);
        }
        return user;
    }

    private User optUser(int entity, Connection optConnection) throws OXException {
        Integer id = I(entity);
        User user = knownUsers.get(id);
        if (null == user) {
            try {
                user = loadUser(entity, optConnection);
            } catch (OXException e) {
                if (CalendarExceptionCodes.INVALID_CALENDAR_USER.equals(e)) {
                    return null;
                }
                throw e;
            }
            knownUsers.put(id, user);
        }
        return user;
    }

    private Resource getResource(int entity, Connection optConnection) throws OXException {
        Integer id = I(entity);
        Resource resource = knownResources.get(id);
        if (null == resource) {
            resource = loadResource(entity, optConnection);
            knownResources.put(id, resource);
        }
        return resource;
    }

    private Resource optResource(int entity, Connection optConnection) throws OXException {
        Integer id = I(entity);
        Resource resource = knownResources.get(id);
        if (null == resource) {
            try {
                resource = loadResource(entity, optConnection);
            } catch (OXException e) {
                if (CalendarExceptionCodes.INVALID_CALENDAR_USER.equals(e)) {
                    return null;
                }
                throw e;
            }
            knownResources.put(id, resource);
        }
        return resource;
    }


    /**
     * Gets a folder by its identifier.
     *
     * @param id The identifier of the folder to get
     * @return The folder
     * @throws OXException
     */
    public FolderObject getFolder(int id) throws OXException {
        return getFolder(id, null);
    }

    /**
     * Gets a folder by its identifier.
     *
     * @param id The identifier of the folder to get
     * @param optConnection An optional connection to the database to use, or <code>null</code> to acquire one dynamically
     * @return The folder
     * @throws OXException
     */
    public FolderObject getFolder(int id, Connection optConnection) throws OXException {
        Integer iD = I(id);
        FolderObject folder = knownFolders.get(iD);
        if (null == folder) {
            folder = loadFolder(id, optConnection);
            knownFolders.put(iD, folder);
        }
        return folder;
    }

    /**
     * Optionally gets a folder by its identifier, if it exists.
     *
     * @param id The identifier of the folder to get
     * @return The folder, or <code>null</code> it doesn't exist
     * @throws OXException
     */
    public FolderObject optFolder(int id) throws OXException {
        try {
            return getFolder(id);
        } catch (OXException e) {
            if (CalendarExceptionCodes.FOLDER_NOT_FOUND.equals(e)) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Gets all visible calendar folders for a specific user.
     *
     * @param userId The identifier of the user to get the visible folders for
     * @param optConnection An optional connection to the database to use, or <code>null</code> to acquire one dynamically
     * @return The folders, or an empty list if there are none
     * @throws OXException
     */
    public List<FolderObject> getVisibleFolders(int userId, Connection optConnection) throws OXException {
        List<FolderObject> folders = loadVisibleFolders(userId, optConnection);
        if (null == folders) {
            return Collections.emptyList();
        }
        for (FolderObject folder : folders) {
            knownFolders.put(I(folder.getObjectID()), folder);
        }
        return folders;
    }

    private <T extends CalendarUser> T applyEntityData(T calendarUser, User user) throws OXException {
        if (hasCalendar(user.getId()) && false == user.isGuest()) {
            /*
             * apply entity data for internal calendar user
             */
            calendarUser.setEntity(user.getId());
            if (Strings.isEmpty(calendarUser.getUri())) {
                calendarUser.setUri(getCalAddress(user));
                calendarUser.setEMail(getEMail(user));
            } else {
                calendarUser.setUri(Check.calendarAddressMatches(calendarUser.getUri(), context.getContextId(), user));
                String email = optEMailAddress(calendarUser.getUri());
                calendarUser.setEMail(null != email ? email : getEMail(user));
            }
        } else {
            /*
             * user without calendar access, handle like an external calendar user
             */
            if (Strings.isEmpty(calendarUser.getUri())) {
                calendarUser.setUri(getURI(getEMail(user)));
                calendarUser.setEMail(getEMail(user));
            } else {
                String email = optEMailAddress(calendarUser.getUri());
                calendarUser.setEMail(null != email ? email : getEMail(user));
            }
            if (calendarUser instanceof Attendee attendee) {
                attendee.removeEntity();
            } else {
                calendarUser.setEntity(-1);
            }
            LOG.debug("User {} is guest or has no calendar access, treating {} as external attendee.", I(user.getId()), calendarUser);
        }
        if (calendarUser instanceof Attendee attendee) {
            attendee.setCuType(CalendarUserType.INDIVIDUAL);
        }
        calendarUser.setCn(Strings.isNotEmpty(calendarUser.getCn()) ? calendarUser.getCn() : user.getDisplayName());
        return calendarUser;
    }

    private <T extends CalendarUser> T applyEntityData(T calendarUser, Group group) {
        calendarUser.setEntity(group.getIdentifier());
        calendarUser.setCn(group.getDisplayName());
        calendarUser.setUri(ResourceId.forGroup(context.getContextId(), group.getIdentifier()));
        if (calendarUser instanceof Attendee attendee) {
            attendee.setCuType(CalendarUserType.GROUP);
            attendee.setPartStat(ParticipationStatus.ACCEPTED);
        }
        return calendarUser;
    }

    private <T extends CalendarUser> T applyEntityData(T calendarUser, Resource resource) throws OXException {
        calendarUser.setEntity(resource.getIdentifier());
        calendarUser.setCn(Strings.isNotEmpty(calendarUser.getCn()) ? calendarUser.getCn() : resource.getDisplayName());
        if (Strings.isEmpty(calendarUser.getUri())) {
            calendarUser.setUri(getCalAddress(resource));
            calendarUser.setEMail(getEMail(resource));
        } else {
            calendarUser.setUri(Check.calendarAddressMatches(calendarUser.getUri(), context.getContextId(), resource));
            String email = optEMailAddress(calendarUser.getUri());
            calendarUser.setEMail(null != email ? email : getEMail(resource));
        }
        if (calendarUser instanceof Attendee attendee) {
            attendee.setCuType(CalendarUserType.RESOURCE);
            if (false == hasBookingDelegate(resource)) {
                attendee.setComment(resource.getDescription());
            }
        }
        return calendarUser;
    }

    private <T extends CalendarUser> T resolveExternals(T calendarUser, CalendarUserType cuType, boolean resolveResourceIds, int[] resolvableEntities) throws OXException {
        if (null != calendarUser) {
            if (false == isInternal(calendarUser, cuType)) {
                ResourceId resourceId = resolveExternals(calendarUser.getUri(), resolveResourceIds, true, cuType);
                if (null != resourceId && (null == resolvableEntities || com.openexchange.tools.arrays.Arrays.contains(resolvableEntities, resourceId.getEntity()))) {
                    /*
                     * resolved successfully; cross-check calendar user type, auto-correct if required
                     */
                    if ((calendarUser instanceof Attendee)) {
                        Attendee attendee = (Attendee) calendarUser;
                        if (false == resourceId.getCalendarUserType().matches(attendee.getCuType())) {
                            LOG.warn("Wrong calendar user type {} for internal entity {} ({}), auto-correcting to {}.",
                                attendee.getCuType(), I(attendee.getEntity()), attendee.getUri(), resourceId.getCalendarUserType());
                            attendee.setCuType(resourceId.getCalendarUserType());
                        }
                    } else if (false == resourceId.getCalendarUserType().matches(cuType)) {
                        throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(calendarUser.getUri(), I(calendarUser.getEntity()), cuType);
                    }
                    calendarUser.setEntity(resourceId.getEntity());
                }
            }
            resolveExternals(calendarUser.getSentBy(), CalendarUserType.INDIVIDUAL, resolveResourceIds, resolvableEntities);
        }
        return calendarUser;
    }

    /**
     * Tries to resolve a calendar user address URI to its corresponding internal resource identifier.
     *
     * @param uri The URI to resolve
     * @param resolveResourceIds <code>true</code> to resolve (internal) resource identifiers, <code>false</code>, otherwise
     * @param considerAliases <code>true</code> to consider user aliases when resolving <code>mailto:</code>-URIs, <code>false</code>, otherwise
     * @param suggestedCuType The suggested calendar user type to use
     * @return The resolved resource identifier, or <code>null</code> if not found
     */
    private ResourceId resolveExternals(String uri, boolean resolveResourceIds, boolean considerAliases, CalendarUserType suggestedCuType) throws OXException {
        if (Strings.isEmpty(uri)) {
            return null;
        }
        /*
         * try to interpret directly as resource id first
         */
        ResourceId resourceId = ResourceId.parse(uri);
        if (null != resourceId) {
            /*
             * resource id matched, take over if exists and not organized externally, otherwise assume external calendar user
             */
            if (resolveResourceIds && context.getContextId() == resourceId.getContextID()) {
                try {
                    return Check.exists(this, resourceId);
                } catch (OXException e) {
                    LOG.warn("Calendar user {} not found in context {}.", uri, I(context.getContextId()), e);
                }
            }
            return null;
        }
        /*
         * try lookup by e-mail address, otherwise, preferring the provided cu-type
         */
        String mail = optEMailAddress(uri);
        if (null != mail) {
            if (CalendarUserType.INDIVIDUAL.matches(suggestedCuType)) {
                resourceId = lookupUserByMail(mail, considerAliases);
                if (null == resourceId) {
                    resourceId = lookupResourceByMail(mail);
                }
            } else if (CalendarUserType.RESOURCE.matches(suggestedCuType) || CalendarUserType.ROOM.matches(suggestedCuType)) {
                resourceId = lookupResourceByMail(mail);
            }
        }
        return resourceId;
    }

    private ResourceId lookupUserByMail(String mail, boolean considerAliases) throws OXException {
        for (User knownUser : knownUsers.values()) {
            if (mail.equalsIgnoreCase(getEMail(knownUser)) || considerAliases && UserAliasUtility.isAlias(mail, knownUser.getAliases())) {
                return new ResourceId(context.getContextId(), knownUser.getId(), CalendarUserType.INDIVIDUAL);
            }
        }
        User user;
        try {
            user = services.getServiceSafe(UserService.class).searchUser(mail, context, considerAliases);
        } catch (OXException e) {
            if ("USR-0014".equals(e.getErrorCode())) {
                user = null;
            } else {
                throw e;
            }
        }
        if (null != user) {
            knownUsers.put(I(user.getId()), user);
            return new ResourceId(context.getContextId(), user.getId(), CalendarUserType.INDIVIDUAL);
        }
        return null;
    }

    private ResourceId lookupResourceByMail(String mail) throws OXException {
        for (Resource knownResource : knownResources.values()) {
            if (mail.equalsIgnoreCase(getEMail(knownResource))) {
                return new ResourceId(context.getContextId(), knownResource.getIdentifier(), CalendarUserType.RESOURCE);
            }
        }
        Resource[] resources = services.getServiceSafe(ResourceService.class).searchResourcesByMail(mail, context);
        if (null != resources && 0 < resources.length) {
            for (Resource resource : resources) {
                knownResources.put(I(resource.getIdentifier()), resource);
                if (mail.equalsIgnoreCase(getEMail(resource))) {
                    return new ResourceId(context.getContextId(), resource.getIdentifier(), CalendarUserType.RESOURCE);
                }
            }
        }
        return null;
    }

    private Resource loadResource(int entity, Connection optConnection) throws OXException {
        try {
            ResourceService resourceService = services.getServiceSafe(ResourceService.class);
            return null == optConnection ? resourceService.getResource(entity, context) : resourceService.getResource(optConnection, entity, context);
        } catch (OXException e) {
            if ("RES-0012".equals(e.getErrorCode())) {
                throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(e, String.valueOf(entity), I(entity), CalendarUserType.RESOURCE);
            }
            throw e;
        }
    }

    private Group loadGroup(int entity) throws OXException {
        try {
            return services.getServiceSafe(GroupService.class).getGroup(context, entity);
        } catch (OXException e) {
            if ("GRP-0017".equals(e.getErrorCode())) {
                throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(e, String.valueOf(entity), I(entity), CalendarUserType.GROUP);
            }
            throw e;
        }
    }

    private User loadUser(int entity, Connection optConnection) throws OXException {
        try {
            UserStorage userStorage = UserStorage.getInstance();
            return null == optConnection ? userStorage.getUser(entity, context) : userStorage.loadIfAbsent(entity, context, optConnection);
        } catch (OXException e) {
            if ("USR-0010".equals(e.getErrorCode())) {
                throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(e, String.valueOf(entity), I(entity), CalendarUserType.INDIVIDUAL);
            }
            throw e;
        }
    }

    private User[] loadUsers(int[] entities) throws OXException {
        try {
            return services.getServiceSafe(UserService.class).getUser(context, entities);
        } catch (OXException e) {
            if ("USR-0010".equals(e.getErrorCode())) {
                if (null != e.getLogArgs() && 0 < e.getLogArgs().length) {
                    Object arg = e.getLogArgs()[0];
                    throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(e, arg, arg, CalendarUserType.INDIVIDUAL);
                }
                throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(e, java.util.Arrays.toString(entities), I(0), CalendarUserType.INDIVIDUAL);
            }
            throw e;
        }
    }

    private FolderObject loadFolder(int id, Connection optConnection) throws OXException {
        try {
            return new OXFolderAccess(optConnection, context).getFolderObject(id);
        } catch (OXException e) {
            if ("FLD-0008".equals(e.getErrorCode())) {
                throw CalendarExceptionCodes.FOLDER_NOT_FOUND.create(e, String.valueOf(id));
            }
            throw e;
        }
    }

    private List<FolderObject> loadVisibleFolders(int userId, Connection optConnection) throws OXException {
        SearchIterator<FolderObject> iterator = null;
        try {
            iterator = OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfModule(
                userId, getUser(userId, optConnection).getGroups(), null, FolderObject.CALENDAR, context, optConnection);
            return SearchIterators.asList(iterator);
        } finally {
            SearchIterators.close(iterator);
        }
    }

    /**
     * Gets a value indicating whether the given user currently has calendar access or not
     *
     * @param userId The identifier of the user to check
     * @return <code>true</code> if the user has calendar access, <code>false</code> otherwise
     * @throws OXException In case {@link UserPermissionService} is missing
     */
    private boolean hasCalendar(int userId) throws OXException {
        try {
            return services.getServiceSafe(UserPermissionService.class).getUserPermissionBits(userId, context).hasCalendar();
        } catch (OXException e) {
            if ("USS-0003".equals(e.getErrorCode())) {
                throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(e, String.valueOf(userId), I(userId), CalendarUserType.INDIVIDUAL);
            }
            throw e;
        }
    }

    /**
     * Gets the calendar addresses for a user (which includes the user's resource identifier, as well as his e-mail addresses as
     * <code>mailto</code>-URIs), wrapped into a case-insensitive tree-set.
     *
     * @param user The user
     * @return The calendar addresses
     */
    //    private Set<String> getCalAddresses(User user) {
    //        TreeSet<String> addresses = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    //        if (null != user.getMail()) {
    //            addresses.add(getURI(user.getMail()));
    //        }
    //        if (null != user.getAliases()) {
    //            for (String alias : user.getAliases()) {
    //                addresses.add(getURI(alias));
    //            }
    //        }
    //        addresses.add(ResourceId.forUser(getContextID(), user.getId()));
    //        return addresses;
    //    }

    /**
     * Gets the calendar address for a user (as <code>mailto</code>-URI).
     *
     * @param user The user
     * @return The calendar address
     */
    private static String getCalAddress(User user) {
        return getURI(getEMail(user));
    }

    /**
     * Gets the calendar address for a resource (as <code>mailto</code>-URI).
     *
     * @param resource The resource
     * @return The calendar address
     */
    private static String getCalAddress(Resource resource) {
        return getURI(getEMail(resource));
    }

    /**
     * Gets the e-mail address for a user.
     *
     * @param user The user
     * @return The e-mail address
     */
    private static String getEMail(User user) {
        return user.getMail();
    }

    /**
     * Gets the e-mail address for a resource.
     *
     * @param resource The resource
     * @return The e-mail address
     */
    private static String getEMail(Resource resource) {
        return resource.getMail();
    }

    /**
     * Gets a value indicating whether the supplied resource contains at least one booking delegate.
     * 
     * @param resource The resource to check
     * @return <code>true</code> if there'a at least one booking delegate, <code>false</code>, otherwise
     */
    private static boolean hasBookingDelegate(Resource resource) {
        ResourcePermission[] permissions = resource.getPermissions();
        if (null != permissions) {
            for (ResourcePermission permission : permissions) {
                if (SchedulingPrivilege.DELEGATE.equals(permission.getSchedulingPrivilege())) {
                    return true;
                }
            }
        }
        return false;
    }

}
