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

package com.openexchange.ajax.parser;

import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import com.openexchange.groupware.attach.AttachmentBatch;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;

public class JSONAttachmentMetadata implements AttachmentMetadata {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JSONAttachmentMetadata.class);

    private final JSONObject json;

    private AttachmentBatch batch;

    public JSONAttachmentMetadata(JSONObject json) {
        super();
        this.json = new JSONObject(json);
    }

    public JSONAttachmentMetadata(final String jsonString) throws JSONException {
        json = JSONServices.parseObject(jsonString);
    }

    public JSONAttachmentMetadata() {
        json = new JSONObject();
    }

    @Override
    public int getCreatedBy() {
        if (json.has(AttachmentField.CREATED_BY_LITERAL.getName())) {
            return json.optInt(AttachmentField.CREATED_BY_LITERAL.getName());
        }
        return -1;
    }

    @Override
    public void setCreatedBy(final int createdBy) {
        try {
            json.put(AttachmentField.CREATED_BY_LITERAL.getName(), createdBy);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public Date getCreationDate() {
        if (!json.has(AttachmentField.CREATION_DATE_LITERAL.getName())) {
            return null;
        }
        return new Date(json.optLong(AttachmentField.CREATION_DATE_LITERAL.getName()));
    }

    @Override
    public void setCreationDate(final Date creationDate) {
        if (creationDate == null && json.has(AttachmentField.CREATION_DATE_LITERAL.getName())) {
            json.remove(AttachmentField.CREATION_DATE_LITERAL.getName());
        } else if (creationDate != null) {
            try {
                json.put(AttachmentField.CREATION_DATE_LITERAL.getName(), creationDate.getTime());
            } catch (JSONException e) {
                LOG.debug("", e);
            }
        }
    }

    @Override
    public String getFileMIMEType() {
        if (!json.has(AttachmentField.FILE_MIMETYPE_LITERAL.getName())) {
            return null;
        }
        return json.optString(AttachmentField.FILE_MIMETYPE_LITERAL.getName());
    }

    @Override
    public void setFileMIMEType(final String fileMIMEType) {
        if (fileMIMEType == null && json.has(AttachmentField.FILE_MIMETYPE_LITERAL.getName())) {
            json.remove(AttachmentField.FILE_MIMETYPE_LITERAL.getName());
            return;
        }
        try {
            json.put(AttachmentField.FILE_MIMETYPE_LITERAL.getName(), fileMIMEType);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public String getFilename() {
        if (!json.has(AttachmentField.FILENAME_LITERAL.getName())) {
            return null;
        }
        return json.optString(AttachmentField.FILENAME_LITERAL.getName());
    }

    @Override
    public void setFilename(final String filename) {
        if (filename == null && json.has(AttachmentField.FILENAME_LITERAL.getName())) {
            json.remove(AttachmentField.FILENAME_LITERAL.getName());
            return;
        }
        try {
            json.put(AttachmentField.FILENAME_LITERAL.getName(), filename);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public long getFilesize() {
        if (!json.has(AttachmentField.FILE_SIZE_LITERAL.getName())) {
            return 0;
        }
        return json.optLong(AttachmentField.FILE_SIZE_LITERAL.getName());

    }

    @Override
    public void setFilesize(final long filesize) {
        try {
            json.put(AttachmentField.FILE_SIZE_LITERAL.getName(), filesize);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public int getAttachedId() {
        if (!json.has(AttachmentField.ATTACHED_ID_LITERAL.getName())) {
            return -1;
        }
        return json.optInt(AttachmentField.ATTACHED_ID_LITERAL.getName());

    }

    @Override
    public void setAttachedId(final int objectId) {
        try {
            json.put(AttachmentField.ATTACHED_ID_LITERAL.getName(), objectId);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public boolean getRtfFlag() {
        return json.optBoolean(AttachmentField.RTF_FLAG_LITERAL.getName());
    }

    @Override
    public void setRtfFlag(final boolean rtfFlag) {
        try {
            json.put(AttachmentField.RTF_FLAG_LITERAL.getName(), rtfFlag);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public int getModuleId() {
        if (!json.has(AttachmentField.MODULE_ID_LITERAL.getName())) {
            return -1;
        }
        return json.optInt(AttachmentField.MODULE_ID_LITERAL.getName());

    }

    @Override
    public void setModuleId(final int moduleId) {
        try {
            json.put(AttachmentField.MODULE_ID_LITERAL.getName(), moduleId);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public int getId() {
        if (!json.has(AttachmentField.ID_LITERAL.getName())) {
            return -1;
        }
        return json.optInt(AttachmentField.ID_LITERAL.getName());
    }

    @Override
    public void setId(final int id) {
        try {
            json.put(AttachmentField.ID_LITERAL.getName(), id);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public void setFolderId(final int folderId) {
        try {
            json.put(AttachmentField.FOLDER_ID_LITERAL.getName(), folderId);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public int getFolderId() {
        if (!json.has(AttachmentField.FOLDER_ID_LITERAL.getName())) {
            return -1;
        }
        return json.optInt(AttachmentField.FOLDER_ID_LITERAL.getName());
    }

    @Override
    public String toString() {
        return json.toString();
    }

    public String toJSONString() {
        return json.toString();
    }

    @Override
    public void setComment(final String string) {
        if (null == string) {
            try {
                json.put(AttachmentField.COMMENT_LITERAL.getName(), JSONObject.NULL);
                return;
            } catch (JSONException e) {
                LOG.debug("", e);
            }
        }
        try {
            json.put(AttachmentField.COMMENT_LITERAL.getName(), string);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public String getComment() {
        return json.optString(AttachmentField.COMMENT_LITERAL.getName());
    }

    @Override
    public String getFileId() {
        return json.optString(AttachmentField.FILE_ID_LITERAL.getName());
    }

    @Override
    public void setFileId(final String string) {
        if (null == string) {
            try {
                json.put(AttachmentField.FILE_ID_LITERAL.getName(), JSONObject.NULL);
                return;
            } catch (JSONException e) {
                LOG.debug("", e);
            }
        }
        try {
            json.put(AttachmentField.FILE_ID_LITERAL.getName(), string);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public void setAttachmentBatch(AttachmentBatch batch) {
        this.batch = batch;
    }

    @Override
    public AttachmentBatch getAttachmentBatch() {
        return batch;
    }

    @Override
    public String getChecksum() {
        return json.optString(AttachmentField.CHECKSUM_LITERAL.getName());
    }

    @Override
    public void setChecksum(String checksum) {
        if (null == checksum) {
            try {
                json.put(AttachmentField.CHECKSUM_LITERAL.getName(), JSONObject.NULL);
                return;
            } catch (JSONException e) {
                LOG.debug("", e);
            }
        }
        try {
            json.put(AttachmentField.CHECKSUM_LITERAL.getName(), checksum);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }

    @Override
    public String getUri() {
        return json.optString(AttachmentField.URI_LITERAL.getName());
    }

    @Override
    public void setUri(String uri) {
        if (null == uri) {
            try {
                json.put(AttachmentField.URI_LITERAL.getName(), JSONObject.NULL);
                return;
            } catch (JSONException e) {
                LOG.debug("", e);
            }
        }
        try {
            json.put(AttachmentField.URI_LITERAL.getName(), uri);
        } catch (JSONException e) {
            LOG.debug("", e);
        }
    }
}
