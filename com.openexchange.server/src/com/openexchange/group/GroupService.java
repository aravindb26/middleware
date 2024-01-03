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

package com.openexchange.group;

import java.util.Date;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.session.Session;
import com.openexchange.user.User;

/**
 * This service defines the API to the groups component.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
@SingletonService
public interface GroupService {

    /**
     * The name for the cache region holding group information.
     */
    public static final String CACHE_REGION_NAME = GroupStorage.CACHE_REGION_NAME;

    /**
     * Creates a group.
     *
     * @param context Context.
     * @param user User for permission checks.
     * @param group group to create.
     * @param checkI18nNames Whether to check i18n names
     * @throws OXException if some problem occurs.
     */
    void create(Context context, User user, Group group, boolean checkI18nNames) throws OXException;

    /**
     * Deletes a group.
     *
     * @param context Context.
     * @param user User for permission checks.
     * @param groupId unique identifier of the group to delete.
     * @param lastModified timestamp when the group to delete has last been read.
     * @throws OXException if some problem occurs.
     */
    void delete(Context context, User user, int groupId, Date lastModified) throws OXException;

    /**
     * Checks for existence of the denoted group.
     *
     * @param context The context
     * @param groupId The group identifier
     * @return <code>true</code> if group exists; otherwise <code>false</code>
     * @throws OXException If group existence cannot be checked
     */
    default boolean exists(Context context, int groupId) throws OXException {
        try {
            Group group = getGroup(context, groupId, false);
            return group != null;
        } catch (OXException e) {
            if (LdapExceptionCode.GROUP_NOT_FOUND.equals(e)) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Returns the denoted group.
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #getGroup(Session, int)} should be preferred.
     *
     * @param context The context
     * @param groupId The group identifier
     * @return The group
     * @throws OXException If group cannot be returned
     */
    Group getGroup(Context context, int groupId) throws OXException;

    /**
     * Returns the denoted group.
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #getGroup(Session, int, boolean)} should be preferred.
     *
     * @param context The context
     * @param groupId The group identifier
     * @param loadMembers Whether to load members or not.
     * @return The group
     * @throws OXException If group cannot be returned
     */
    Group getGroup(Context context, int groupId, boolean loadMembers) throws OXException;

    /**
     * Returns the all groups of a given context.
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #getGroups(Session, boolean)} should be preferred.
     *
     * @param ctx The context
     * @param loadMembers Whether to load members or not.
     * @return An array of groups
     * @throws OXException If group cannot be returned
     */
    Group[] getGroups(Context ctx, boolean loadMembers) throws OXException;

    /**
     * Returns the groups with the given identifiers
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #listGroups(Session, int[])} should be preferred.
     *
     * @param ctx The context
     * @param ids An array of group identifiers to get
     * @return An array of groups
     * @throws OXException If group cannot be returned
     */
    Group[] listGroups(Context ctx, int[] ids) throws OXException;

    /**
     * Searches for groups by their display name.
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #searchGroups(Session, String, boolean)} should be
     * preferred.
     *
     * @param context The context
     * @param pattern The pattern to search for
     * @param loadMembers <code>true</code> to load the members of found groups, <code>false</code>, otherwise
     * @return An array of groups that match the search pattern
     * @throws OXException if searching has some storage related problem.
     */
    Group[] search(Context context, String pattern, boolean loadMembers) throws OXException;

    /**
     * Lists all groups in the given context
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #getGroups(Session, boolean)} should be preferred.
     *
     * @param context The context
     * @param loadMembers Whether to load member or not
     * @return The groups in the given context
     * @throws OXException
     */
    Group[] listAllGroups(Context context, boolean loadMembers) throws OXException;

    /**
     * Gets all groups within the context which has been modified since the given date
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #listModifiedGroups(Session, Date)} should be preferred.
     *
     * @param context The context
     * @param modifiedSince The boundary date
     * @return An array of groups which has been modified since the given date
     * @throws OXException
     */
    Group[] listModifiedGroups(Context context, Date modifiedSince) throws OXException;

    /**
     * Gets all groups within the context which has been deleted since the given date
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #listDeletedGroups(Session, Date)} should be preferred.
     *
     * @param context The context
     * @param deletedSince The boundary date
     * @return An array of groups which has been deleted since the given date
     * @throws OXException
     */
    Group[] listDeletedGroups(Context context, Date deletedSince) throws OXException;

    /**
     * Updates a group.
     * <p/>
     * <b>Note:</b> If the request originates from a client, the method {@link #update(Session, Group, Date, boolean)} should be preferred.
     *
     * @param context Context.
     * @param user User for permission checks.
     * @param group group to update.
     * @param lastRead timestamp when the group to update has last been read.
     * @param checkI18nNames Whether to check i18n names
     * @throws OXException if some problem occurs.
     */
    void update(Context context, User user, Group group, Date lastRead, boolean checkI18nNames) throws OXException;

    /**
     * Similar to {@link #getGroup(Context, int)}, but implicitly translates the display name of system groups based on the session
     * user's locale.
     *
     * @param session The user session
     * @param groupId The group identifier
     * @return The group
     * @throws OXException If group cannot be returned
     */
    Group getGroup(Session session, int groupId) throws OXException;

    /**
     * Similar to {@link #getGroup(Context, int, boolean)}, but implicitly translates the display name of system groups based on the
     * session user's locale.
     * 
     * Returns the denoted group.
     *
     * @param session The user session
     * @param groupId The group identifier
     * @param loadMembers Whether to load members or not.
     * @return The group
     * @throws OXException If group cannot be returned
     */
    Group getGroup(Session session, int groupId, boolean loadMembers) throws OXException;

    /**
     * Similar to {@link #search(Context, String, boolean)} but sorts the results according to the use count, and display names of system
     * groups are translated based on the session user's locale.
     * <p/>
     * Additionally, groups that are configured to be hidden are filtered implicitly from the results.
     *
     * @param session The user session
     * @param pattern this pattern will be searched in the displayName of the group.
     * @param loadMembers - switch whether members should be loaded too (decreases performance, don't use if not needed)
     * @return an array of groups that match the search pattern sorted by use count.
     * @throws OXException if searching has some storage related problem.
     */
    Group[] searchGroups(Session session, String pattern, boolean loadMembers) throws OXException;

    /**
     * Similar to {@link #getGroups(Context, boolean)} but sorts the results according to the use count, and display names of system
     * groups are translated based on the session user's locale.
     * <p/>
     * Additionally, groups that are configured to be hidden are filtered implicitly from the results.
     *
     * @param session The user session
     * @param loadMembers Whether to load members or not.
     * @return An array of groups
     * @throws OXException If group cannot be returned
     */
    Group[] getGroups(Session session, boolean loadMembers) throws OXException;

    /**
     * Similar to {@link #listGroups(Context, int[])}, but implicitly translates the display name of system groups based on the session
     * user's locale.
     *
     * @param session The user session
     * @param ids An array of group identifiers to get
     * @return An array of groups
     * @throws OXException If group cannot be returned
     */
    Group[] listGroups(Session session, int[] ids) throws OXException;

    /**
     * Similar to {@link #listModifiedGroups(Context, Date)}, but implicitly translates the display name of system groups based on the session
     * user's locale.
     * Gets all groups within the context which has been modified since the given date
     *
     * @param session The user session
     * @param modifiedSince The boundary date
     * @return An array of groups which has been modified since the given date
     * @throws OXException
     */
    Group[] listModifiedGroups(Session session, Date modifiedSince) throws OXException;

    /**
     * Similar to {@link #listModifiedGroups(Context, Date)}, but implicitly translates the display name of system groups based on the session
     * user's locale.
     *
     * @param session The user session
     * @param deletedSince The boundary date
     * @return An array of groups which has been deleted since the given date
     * @throws OXException
     */
    Group[] listDeletedGroups(Session session, Date deletedSince) throws OXException;

    /**
     * Similar to {@link #update(Context, User, Group, Date, boolean)}, but implicitly ignores the translated display name in updates for
     * system groups if needed.
     *
     * @param session The user session
     * @param group group to update.
     * @param lastRead timestamp when the group to update has last been read.
     * @param checkI18nNames Whether to check i18n names
     * @throws OXException if some problem occurs.
     */
    void update(Session session, Group group, Date lastRead, boolean checkI18nNames) throws OXException;

}
