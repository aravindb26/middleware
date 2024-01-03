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

package com.openexchange.group.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.Groups;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Autoboxing;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class GroupServiceImpl implements GroupService {

    private final GroupStorage storage;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link GroupServiceImpl}.
     */
    public GroupServiceImpl(GroupStorage storage, ServiceLookup services) {
        this.storage = storage;
        this.services = services;
    }

    @Override
    public void create(Context context, User user, Group group, boolean checkI18nNames) throws OXException {
        Create create = new Create(context, user, group, checkI18nNames);
        create.perform();
    }

    @Override
    public void delete(Context context, User user, int groupId, Date lastRead) throws OXException {
        Delete delete = new Delete(context, user, groupId, lastRead);
        delete.perform();
    }

    @Override
    public boolean exists(Context ctx, int groupId) throws OXException {
        return storage.exists(groupId, ctx);
    }

    @Override
    public Group getGroup(Context ctx, int groupId) throws OXException {
        return storage.getGroup(groupId, ctx);
    }

    @Override
    public Group getGroup(Context ctx, int groupId, boolean loadMembers) throws OXException {
        return storage.getGroup(groupId, loadMembers, ctx);
    }

    @Override
    public Group[] search(Context ctx, String pattern, boolean loadMembers) throws OXException {
        return storage.searchGroups(pattern, loadMembers, ctx);
    }

    @Override
    public Group[] getGroups(Context ctx, boolean loadMembers) throws OXException {
        return storage.getGroups(loadMembers, ctx);
    }

    @Override
    public Group[] listModifiedGroups(Context context, Date modifiedSince) throws OXException {
        return storage.listModifiedGroups(modifiedSince, context);
    }

    @Override
    public Group[] listDeletedGroups(Context context, Date deletedSince) throws OXException {
        return storage.listDeletedGroups(deletedSince, context);
    }

    @Override
    public Group[] searchGroups(Session session, String pattern, boolean loadMembers) throws OXException {
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        Group[] groups = storage.searchGroups(serverSession.getContext(), pattern, loadMembers, serverSession.getUser().getLocale());
        return translateDisplayNames(session, sortGroupsByUseCount(session, removeHiddenGroups(session, groups)));
    }

    @Override
    public Group[] getGroups(Session session, boolean loadMembers) throws OXException {
        Group[] groups = storage.getGroups(loadMembers, ServerSessionAdapter.valueOf(session).getContext());
        return translateDisplayNames(session, sortGroupsByUseCount(session, removeHiddenGroups(session, groups)));
    }

    @Override
    public Group getGroup(Session session, int groupId) throws OXException {
        return translateDisplayName(session, getGroup(ServerSessionAdapter.valueOf(session).getContext(), groupId));
    }

    @Override
    public Group getGroup(Session session, int groupId, boolean loadMembers) throws OXException {
        return translateDisplayName(session, getGroup(ServerSessionAdapter.valueOf(session).getContext(), groupId, loadMembers));
    }

    @Override
    public Group[] listGroups(Session session, int[] ids) throws OXException {
        return translateDisplayNames(session, listGroups(ServerSessionAdapter.valueOf(session).getContext(), ids));
    }

    @Override
    public Group[] listModifiedGroups(Session session, Date modifiedSince) throws OXException {
        return translateDisplayNames(session, listModifiedGroups(ServerSessionAdapter.valueOf(session).getContext(), modifiedSince));
    }

    @Override
    public Group[] listDeletedGroups(Session session, Date deletedSince) throws OXException {
        return translateDisplayNames(session, listDeletedGroups(ServerSessionAdapter.valueOf(session).getContext(), deletedSince));
    }

    @Override
    public void update(Session session, Group group, Date lastRead, boolean checkI18nNames) throws OXException {
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        Group groupUpdate = group;
        if (groupUpdate.isDisplayNameSet()) {
            Locale locale = serverSession.getUser().getLocale();
            if (false == LocaleTools.DEFAULT_LOCALE.equals(locale)) {
                /*
                 * revert unchanged localized display name in update as needed prior passing down to storage
                 */
                StringHelper stringHelper = StringHelper.valueOf(locale);
                if (GroupStorage.GROUP_ZERO_IDENTIFIER == groupUpdate.getIdentifier() && 
                    stringHelper.getString(Groups.ALL_USERS).equals(groupUpdate.getDisplayName())) {
                    groupUpdate.setDisplayName(Groups.ALL_USERS);
                } else if (GroupStorage.GUEST_GROUP_IDENTIFIER == groupUpdate.getIdentifier() &&
                    stringHelper.getString(Groups.GUEST_GROUP).equals(groupUpdate.getDisplayName())) {
                    groupUpdate.setDisplayName(Groups.GUEST_GROUP);
                } else if (stringHelper.getString(Groups.STANDARD_GROUP).equals(groupUpdate.getDisplayName())) {
                    groupUpdate.setDisplayName(Groups.STANDARD_GROUP);
                }
            }
        }
        update(serverSession.getContext(), serverSession.getUser(), groupUpdate, lastRead, checkI18nNames);
    }

    private Group[] sortGroupsByUseCount(Session session, Group[] groups) throws OXException {
        PrincipalUseCountService usecountService = services.getOptionalService(PrincipalUseCountService.class);
        if (usecountService == null) {
            return groups;
        }

        Integer[] principalIds = new Integer[groups.length];
        int x = 0;
        for (Group group : groups) {
            principalIds[x++] = Autoboxing.I(group.getIdentifier());
        }
        Map<Integer, Integer> useCounts = usecountService.get(session, principalIds);
        List<GroupAndUseCount> listToSort = new ArrayList<>(useCounts.size());
        x = 0;
        for (Group group : groups) {
            listToSort.add(new GroupAndUseCount(group, useCounts.get(Autoboxing.I(group.getIdentifier()))));
        }
        Collections.sort(listToSort);

        Group[] result = new Group[listToSort.size()];
        x = 0;
        for (GroupAndUseCount tmp : listToSort) {
            result[x++] = tmp.getGroup();
        }
        return result;
    }

    private static class GroupAndUseCount implements Comparable<GroupAndUseCount> {

        private final Group group;
        private final Integer usecount;

        /**
         * Initializes a new {@link GroupServiceImpl.GroupAndUseCount}.
         */
        public GroupAndUseCount(Group group, Integer usecount) {
            this.group = group;
            this.usecount = usecount;
        }

        /**
         * Gets the group
         *
         * @return The group
         */
        public Group getGroup() {
            return group;
        }

        /**
         * Gets the usecount
         *
         * @return The usecount
         */
        public Integer getUsecount() {
            return usecount;
        }

        @Override
        public int compareTo(GroupAndUseCount o) {
            return -this.usecount.compareTo(o.getUsecount());
        }

    }

    @Override
    public void update(Context context, User user, Group group, Date lastRead, boolean checkI18nNames) throws OXException {
        Update update = new Update(context, user, group, lastRead, checkI18nNames);
        update.perform();
    }

    @Override
    public Group[] listAllGroups(Context context, boolean loadMembers) throws OXException {
        return storage.getGroups(loadMembers, context);
    }

    @Override
    public Group[] listGroups(Context ctx, int[] ids) throws OXException {
        return storage.getGroup(ids, ctx);
    }

    /**
     * Translates the display name of a certain system group as needed, based on the session user's locale.
     * 
     * @param session The user session
     * @param group The group to translate the display name in
     * @return The passed group, with translated display name
     */
    private static Group translateDisplayName(Session session, Group group) {
        return translateDisplayNames(session, new Group[] { group })[0];
    }

    /**
     * Translates the display names of certain system groups as needed, based on the session user's locale.
     * 
     * @param session The user session
     * @param groups The groups to translate the display name in
     * @return The passed array of groups, with translated display names
     */
    private static Group[] translateDisplayNames(Session session, Group[] groups) {
        if (null == groups || 0 == groups.length) {
            return groups;
        }
        try {
            Locale locale = ServerSessionAdapter.valueOf(session).getUser().getLocale();
            if (LocaleTools.DEFAULT_LOCALE.equals(locale)) {
                return groups;
            }
            StringHelper stringHelper = StringHelper.valueOf(locale);
            for (Group group : groups) {
                translateDisplayName(stringHelper, group);
            }
        } catch (OXException e) {
            org.slf4j.LoggerFactory.getLogger(GroupServiceImpl.class).warn(
                "Unexpected error getting translation for group names, falling back to defaults.", e);
        }
        return groups;
    }

    private static void translateDisplayName(StringHelper stringHelper, Group group) {
        if (GroupStorage.GROUP_ZERO_IDENTIFIER == group.getIdentifier()) {
            group.setDisplayName(stringHelper.getString(Groups.ALL_USERS));
        } else if (GroupStorage.GUEST_GROUP_IDENTIFIER == group.getIdentifier()) {
            group.setDisplayName(stringHelper.getString(Groups.GUEST_GROUP));
        } else if (GroupStorage.GROUP_STANDARD_SIMPLE_NAME.equals(group.getSimpleName())) {
            group.setDisplayName(stringHelper.getString(Groups.STANDARD_GROUP));
        }
    }

    /**
     * Filters all groups that are configured to be hidden from <i>all</i>- and <i>search</i>-responses.
     *
     * @param session The current user's session
     * @param groups The groups to filter
     * @return The (possibly filtered) groups
     */
    private Group[] removeHiddenGroups(Session session, Group[] groups) {
        if (null == groups || 0 == groups.length) {
            return groups;
        }
        LeanConfigurationService configService = services.getOptionalService(LeanConfigurationService.class);
        if (null == configService) {
            org.slf4j.LoggerFactory.getLogger(GroupServiceImpl.class).warn("Configuration not found, unable to filter hidden groups.");
            return groups;
        }
        boolean filtered = false;
        List<Group> filteredGroups = new ArrayList<Group>(groups.length);
        for (Group group : groups) {
            if (GroupStorage.GROUP_ZERO_IDENTIFIER == group.getIdentifier()) {
                if (configService.getBooleanProperty(session.getUserId(), session.getContextId(), GroupProperty.HIDE_ALL_USERS)) {
                    filtered = true;
                    continue;
                }
            } else if (GroupStorage.GUEST_GROUP_IDENTIFIER == group.getIdentifier()) {
                if (configService.getBooleanProperty(session.getUserId(), session.getContextId(), GroupProperty.HIDE_ALL_GUESTS)) {
                    filtered = true;
                    continue;
                }
            } else if (GroupStorage.GROUP_STANDARD_SIMPLE_NAME.equals(group.getSimpleName())) {
                if (configService.getBooleanProperty(session.getUserId(), session.getContextId(), GroupProperty.HIDE_STANDARD_GROUP)) {
                    filtered = true;
                    continue;
                }
            }
            filteredGroups.add(group);
        }
        return filtered ? filteredGroups.toArray(new Group[filteredGroups.size()]) : groups;
    }

}
