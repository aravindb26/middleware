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

package com.openexchange.drive.events.ms;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.hazelcast.nio.serialization.VersionedPortable;
import com.openexchange.drive.events.DriveContentChange;
import com.openexchange.drive.events.DriveEvent;
import com.openexchange.drive.events.DriveEventImpl;
import com.openexchange.hazelcast.serialization.CustomPortable;


/**
 * {@link PortableDriveEvent}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class PortableDriveEvent implements CustomPortable, VersionedPortable {

    /** The unique portable class ID of the {@link PortableDriveEvent} */
    static final int CLASS_ID = 2;

    /**
     * The class version for {@link PortableDriveEvent}.
     * <p>
     * This number should be incremented whenever fields are added;
     * see <a href="http://docs.hazelcast.org/docs/latest-development/manual/html/Serialization/Implementing_Portable_Serialization/Versioning_for_Portable_Serialization.html">here</a> for reference.
     */
    static final int CLASS_VERSION = 3;

    private static final String PARAMETER_CONTEXT_ID = "c";
    private static final String PARAMETER_PUSH_TOKEN = "p";
    private static final String PARAMETER_FOLDERS_PER_USER = "fpu";
    private static final String PARAMETER_CONTENT_CHANGES = "cc";
    private static final String PARAMETER_CONTENT_CHANGES_ONLY = "cco";

    /** The class definition for PortableDriveEvent */
    static ClassDefinition CLASS_DEFINITION = new ClassDefinitionBuilder(FACTORY_ID, CLASS_ID, CLASS_VERSION)
        .addIntField(PARAMETER_CONTEXT_ID)
        .addStringField(PARAMETER_PUSH_TOKEN)
        .addStringField(PARAMETER_FOLDERS_PER_USER)
        .addPortableArrayField(PARAMETER_CONTENT_CHANGES, PortableFolderContentChange.CLASS_DEFINITION)
        .addBooleanField(PARAMETER_CONTENT_CHANGES_ONLY)
    .build();

    private int contextID;
    private String pushToken;
    private Map<Integer, Set<String>> affectedFoldersPerUser;
    private List<DriveContentChange> contentChanges;
    private boolean contentChangesOnly;

    /**
     * Initializes a new {@link PortableDriveEvent}.
     */
    public PortableDriveEvent() {
        super();
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public int getClassVersion() {
        return CLASS_VERSION;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeInt(PARAMETER_CONTEXT_ID, contextID);
        writer.writeString(PARAMETER_PUSH_TOKEN, pushToken);
        writer.writeString(PARAMETER_FOLDERS_PER_USER, encode(affectedFoldersPerUser));
        writer.writePortableArray(PARAMETER_CONTENT_CHANGES, PortableFolderContentChange.wrap(contentChanges));
        writer.writeBoolean(PARAMETER_CONTENT_CHANGES_ONLY, contentChangesOnly);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        contextID = reader.readInt(PARAMETER_CONTEXT_ID);
        pushToken = reader.readString(PARAMETER_PUSH_TOKEN);
        affectedFoldersPerUser = decode(reader.readString(PARAMETER_FOLDERS_PER_USER));
        contentChanges = PortableFolderContentChange.unwrap(reader.readPortableArray(PARAMETER_CONTENT_CHANGES));
        contentChangesOnly = reader.readBoolean(PARAMETER_CONTENT_CHANGES_ONLY);
    }

    public static PortableDriveEvent wrap(DriveEvent driveEvent) {
        if (null == driveEvent) {
            return null;
        }
        PortableDriveEvent portableEvent = new PortableDriveEvent();
        portableEvent.contextID = driveEvent.getContextID();
        portableEvent.pushToken = driveEvent.getPushTokenReference();
        portableEvent.affectedFoldersPerUser = driveEvent.getAffectedFoldersPerUser();
        portableEvent.contentChanges = driveEvent.getContentChanges();
        portableEvent.contentChangesOnly = driveEvent.isContentChangesOnly();
        return portableEvent;
    }

    public static DriveEvent unwrap(PortableDriveEvent portableEvent) {
        return new DriveEventImpl(portableEvent.contextID, portableEvent.affectedFoldersPerUser, portableEvent.contentChanges, portableEvent.contentChangesOnly, true, portableEvent.pushToken);
    }

    private static Map<Integer, Set<String>> decode(String encodedAffectedFoldersPerUser) throws IOException {
        try {
            JSONObject jsonFoldersPerUser = JSONServices.parseObject(encodedAffectedFoldersPerUser);
            Map<Integer, Set<String>> affectedFoldersPerUser = new HashMap<Integer, Set<String>>(jsonFoldersPerUser.length());
            for (Entry<String, Object> entry : jsonFoldersPerUser.entrySet()) {
                JSONArray jsonArray = ((JSONArray) entry.getValue());
                Set<String> folderIds = new HashSet<String>(jsonArray.length());
                for (Object value : jsonArray) {
                    folderIds.add((String) value);
                }
                affectedFoldersPerUser.put(Integer.valueOf(entry.getKey()), folderIds);
            }
            return affectedFoldersPerUser;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static String encode(Map<Integer, Set<String>> affectedFoldersPerUser) throws IOException {
        try {
            JSONObject jsonFoldersPerUser = new JSONObject(affectedFoldersPerUser.size());
            for (Entry<Integer, Set<String>> entry : affectedFoldersPerUser.entrySet()) {
                jsonFoldersPerUser.put(entry.getKey().toString(), new JSONArray(entry.getValue()));
            }
            return jsonFoldersPerUser.toString();
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

}
