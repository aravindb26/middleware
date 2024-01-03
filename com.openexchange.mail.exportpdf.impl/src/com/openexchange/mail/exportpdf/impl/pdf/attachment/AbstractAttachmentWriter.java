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

package com.openexchange.mail.exportpdf.impl.pdf.attachment;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converters.MailExportConverterUtil;
import com.openexchange.session.Session;

/**
 * {@link AbstractAttachmentWriter}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
abstract class AbstractAttachmentWriter implements AttachmentWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAttachmentWriter.class);
    final List<Closeable> closeables;
    final Session session;
    final LeanConfigurationService leanConfigService;
    private final Property fileExtensions;

    /**
     * Initialises a new {@link AbstractAttachmentWriter}.
     */
    AbstractAttachmentWriter(Session session, LeanConfigurationService leanConfigService, Property fileExtensions) {
        super();
        this.session = session;
        this.leanConfigService = leanConfigService;
        this.fileExtensions = fileExtensions;
        closeables = new LinkedList<>();
    }

    /**
     * Checks if the input stream on the specified result is null
     *
     * @param inputStream The input stream
     * @param title The optional title. for logging purposes
     * @return <code>true</code> if the stream is <code>null</code>; <code>false</code> otherwise
     */
    boolean checkNullStream(InputStream inputStream, Optional<String> title) {
        if (null != inputStream) {
            return false;
        }
        String titleS = title.orElse("<TILE NOT AVAILABLE>");
        LOG.debug("No stream found for attachment {}", titleS);
        return true;
    }

    @Override
    public void close() throws IOException {
        Streams.closeAutoCloseables(closeables);
    }

    @Override
    public boolean handles(MailExportConversionResult result) throws OXException {
        String value = leanConfigService.getProperty(session.getUserId(), session.getContextId(), fileExtensions);
        Set<String> enabledFileExtensions = Strings.splitByComma(value, new HashSet<>());
        return MailExportConverterUtil.handles(result.getContentType(), result.getTitle().orElse(""), enabledFileExtensions);
    }
}
