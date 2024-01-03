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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link CalendarFolderChooser}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8
 */
public class CalendarFolderChooser {

    private final CalendarSession session;
    private final CalendarStorage storage;

    private List<CalendarFolder> visibleFolders;

    /**
     * Initializes a new {@link CalendarFolderChooser}.
     *
     * @param performer The underyling query performer
     */
    protected CalendarFolderChooser(AbstractQueryPerformer performer) {
        this(performer.session, performer.storage);
    }

    /**
     * Initializes a new {@link CalendarFolderChooser}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     */
    protected CalendarFolderChooser(CalendarSession session, CalendarStorage storage) {
        super();
        this.session = session;
        this.storage = storage;
    }

    /**
     * Gets all calendar folders accessible by the current sesssion's user.
     * <p>
     * Folders are always sorted
     *
     * @return The folders, or an empty list if there are none
     */
    public List<CalendarFolder> getVisibleFolders() throws OXException {
        if (null == visibleFolders) {
            visibleFolders = Utils.getVisibleFolders(session);
        }
        return visibleFolders;
    }

    /**
     * Gets the identifiers of all calendar folders of certain types accessible by the current sesssion's user.
     *
     * @param types The types to restrict the returned folder ids to
     * @return The identifiers of the calendar folders
     */
    public List<String> getVisibleFolderIds(Type... types) throws OXException {
        List<CalendarFolder> visibleFolders = getVisibleFolders();
        List<String> folderIds = new ArrayList<String>(visibleFolders.size());
        for (CalendarFolder folder : visibleFolders) {
            if (null == types || Arrays.contains(types, folder.getType())) {
                folderIds.add(folder.getId());
            }
        }
        return folderIds;
    }

    /**
     * Chooses the most appropriate parent folder to render an event in for the current session's user. This is
     * <ul>
     * <li>the common parent folder for an event in a public folder, in case the user has appropriate folder permissions</li>
     * <li><code>null</code> for an event in a public folder, in case the user has no appropriate folder permissions</li>
     * <li>the user attendee's personal folder for an event in a non-public folder, in case the user is attendee of the event</li>
     * <li>another attendee's personal folder for an event in a non-public folder, in case the user does not attend on his own, but has appropriate folder permissions for this attendee's folder</li>
     * <li><code>null</code> for an event in a non-public folder, in case the user has no appropriate folder permissions for any of the attendees</li>
     * </ul>
     *
     * @param event The event to choose the folder for
     * @return The chosen folder, or <code>null</code> if there is none
     */
    public CalendarFolder chooseFolder(Event event) throws OXException {
        /*
         * check common folder permissions for events with a static parent folder (public folders)
         */
        if (null != event.getFolderId()) {
            CalendarFolder folder = findFolder(getVisibleFolders(), event.getFolderId());
            if (null != folder) {
                int readPermission = folder.getOwnPermission().getReadPermission();
                if (canReadEvent(event, readPermission)) {
                    return folder;
                }
            }
            return null;
        }
        /*
         * prefer user's personal folder if user is attendee
         */
        Attendee ownAttendee = find(event.getAttendees(), session.getUserId());
        if (null != ownAttendee) {
            return findFolder(getVisibleFolders(), ownAttendee.getFolderId());
        }
        /*
         * choose the most appropriate attendee folder, otherwise
         */
        CalendarFolder chosenFolder = chooseAttendeeFolder(event, event.getAttendees());
        if (null != chosenFolder) {
            return chosenFolder;
        }
        /*
         * lookup to which private or shared folders the event belongs to and retry to get an attendee folder
         */
        return chooseAttendeeFolder(event, storage.getAttendeeStorage().loadAttendees(event.getId(), //
            getVisibleFolders().stream().filter(f -> !PublicType.getInstance().equals(f.getType())).map(f -> f.getId()).collect(Collectors.toList()),//
            ATTENDEE_FOLDER_SEARCH));
    }

    /**
     * Chooses the most appropriate parent folder identifier to render an event in for the current session's user. This is
     * <ul>
     * <li>the common parent folder identifier for an event in a public folder, in case the user has appropriate folder permissions</li>
     * <li><code>null</code> for an event in a public folder, in case the user has no appropriate folder permissions</li>
     * <li>the user attendee's personal folder identifier for an event in a non-public folder, in case the user is attendee of the event</li>
     * <li>another attendee's personal folder identifier for an event in a non-public folder, in case the user does not attend on his own, but has appropriate folder permissions for this attendee's folder</li>
     * <li><code>null</code> for an event in a non-public folder, in case the user has no appropriate folder permissions for any of the attendees</li>
     * </ul>
     *
     * @param event The event to choose the folder identifier for
     * @return The chosen folder identifier, or <code>null</code> if there is none
     */
    public String chooseFolderID(Event event) throws OXException {
        CalendarFolder chosenFolder = chooseFolder(event);
        return null == chosenFolder ? null : chosenFolder.getId();
    }

    /**
     * Gets a certain calendar folder by its identifier, in case it is visible.
     *
     * @param folderId The identifier of the folder to get
     * @return The folder, or <code>null</code> if not accessible
     */
    public CalendarFolder lookupFolder(String folderId) throws OXException {
        return findFolder(getVisibleFolders(), folderId);
    }

    private static final AttendeeField[] ATTENDEE_FOLDER_SEARCH = { AttendeeField.FOLDER_ID };

    private boolean canReadEvent(Event event, int readPermission) {
        return Permission.READ_ALL_OBJECTS <= readPermission || Permission.READ_OWN_OBJECTS == readPermission && matches(event.getCreatedBy(), session.getUserId());
    }

    /**
     * Select the most appropriate attendee folder view for an event applicable for the session user.
     *
     * @param event The event to choose the attendee folder view for
     * @param attendees The attendees to consider
     * @return The attendee calendar folder, or <code>null</code> if there is none
     */
    public CalendarFolder chooseAttendeeFolder(Event event, List<Attendee> attendees) throws OXException {
        if (null != attendees) {
            for (Attendee attendee : attendees) {
                CalendarFolder folder = findFolder(getVisibleFolders(), attendee.getFolderId());
                if (null != folder) {
                    int readPermission = folder.getOwnPermission().getReadPermission();
                    if (canReadEvent(event, readPermission)) {
                        return folder; // rely on sorting order of visible folders
                    }
                }
            }
        }
        return null;
    }

    /**
     * Searches a userized folder in a collection of folders by its numerical identifier.
     *
     * @param folders The folders to search
     * @param id The identifier of the folder to lookup
     * @return The matching folder, or <code>null</code> if not found
     */
    private static CalendarFolder findFolder(Collection<CalendarFolder> folders, String id) {
        if (null != folders && null != id) {
            for (CalendarFolder folder : folders) {
                if (id.equals(folder.getId())) {
                    return folder;
                }
            }
        }
        return null;
    }

}
