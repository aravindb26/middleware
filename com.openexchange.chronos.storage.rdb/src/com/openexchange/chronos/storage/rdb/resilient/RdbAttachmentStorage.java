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

package com.openexchange.chronos.storage.rdb.resilient;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.exception.ProblemSeverity;
import com.openexchange.chronos.storage.AttachmentStorage;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link RdbAttachmentStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RdbAttachmentStorage extends RdbResilientStorage implements AttachmentStorage {

    private final AttachmentStorage delegate;

    /**
     * Initializes a new {@link RdbAttachmentStorage}.
     *
     * @param services A service lookup reference
     * @param delegate The delegate storage
     * @param handleTruncations <code>true</code> to automatically handle data truncation warnings, <code>false</code>, otherwise
     * @param handleIncorrectStrings <code>true</code> to automatically handle incorrect string warnings, <code>false</code>, otherwise
     * @param unsupportedDataThreshold The threshold defining up to which severity unsupported data errors can be ignored, or <code>null</code> to not ignore any
     *            unsupported data error at all
     */
    public RdbAttachmentStorage(ServiceLookup services, AttachmentStorage delegate, boolean handleTruncations, boolean handleIncorrectStrings, ProblemSeverity unsupportedDataThreshold) {
        super(services, delegate, handleTruncations, handleIncorrectStrings, unsupportedDataThreshold);
        this.delegate = delegate;
    }

    @Override
    public void insertAttachments(Session session, String folderID, String eventID, List<Attachment> attachments) throws OXException {
        delegate.insertAttachments(session, folderID, eventID, attachments);
    }

    @Override
    public Map<String, List<Attachment>> loadAttachments(String[] eventIDs) throws OXException {
        return delegate.loadAttachments(eventIDs);
    }

    @Override
    public Set<String> hasAttachments(String[] eventIds) throws OXException {
        return delegate.hasAttachments(eventIds);
    }

    @Override
    public List<Attachment> loadAttachments(String eventID) throws OXException {
        return delegate.loadAttachments(eventID);
    }

    @Override
    public void deleteAttachments(Session session, String folderID, String eventID) throws OXException {
        delegate.deleteAttachments(session, folderID, eventID);
    }

    @Override
    public void deleteAttachments(Session session, String folderID, List<String> eventIDs) throws OXException {
        delegate.deleteAttachments(session, folderID, eventIDs);
    }

    @Override
    public void deleteAttachments(Session session, String folderID, String eventID, List<Attachment> attachments) throws OXException {
        delegate.deleteAttachments(session, folderID, eventID, attachments);
    }

    @Override
    public InputStream loadAttachmentData(int managedId) throws OXException {
        return delegate.loadAttachmentData(managedId);
    }

    @Override
    public String resolveAttachmentId(int managedId) throws OXException {
        return delegate.resolveAttachmentId(managedId);
    }

}
