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

package com.openexchange.mail.exportpdf.impl;

import java.io.IOException;
import java.io.InputStream;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.exportpdf.MailExportExceptionCode;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;

/**
 * {@link DefaultMailExportMailPartContainer} - The default implementation of the mail part container.
 * Contains information regarding an attachment, and whether that attachment is inline or not.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DefaultMailExportMailPartContainer implements MailExportMailPartContainer {

    private final MailPart delegate;
    private final boolean inline;
    private final String id;
    private final String imageCID;

    /**
     * Default container
     *
     * @param mailPart The delegate mail part
     * @param inline whether the mail part is inline
     * @param id The id of the mail part
     * @param imageCID the image cid if the mail part happens to be an image
     */
    public DefaultMailExportMailPartContainer(MailPart mailPart, boolean inline, String id, String imageCID) {
        super();
        delegate = mailPart;
        this.inline = inline;
        this.id = id;
        this.imageCID = imageCID;
    }

    @Override
    public String getBaseContentType() {
        return delegate.getContentType().getBaseType();
    }

    @Override
    public InputStream getInputStream() throws OXException {
        return delegate.getInputStream();
    }

    @Override
    public long getSize() throws OXException {
        try {
            // Stream should not be closed at this stage. It will be handled in the clean-up phase, i.e. via the global 'closeables' instance
            return Streams.countInputStream(delegate.getInputStream());
        } catch (IOException e) {
            throw MailExportExceptionCode.IO_ERROR.create(e.getMessage(), e);
        }
    }

    @Override
    public String getTitle() {
        String title = delegate.getFirstHeader("Subject");
        if (Strings.isNotEmpty(title)) {
            return title;
        }

        title = delegate.getFileName();
        if (Strings.isNotEmpty(title)) {
            return title;
        }
        title = delegate.getContentDisposition().getFilenameParameter();
        if (Strings.isNotEmpty(title)) {
            return title;
        }
        return null;
    }

    @Override
    public String getFileName() {
        return delegate.getFileName();
    }

    @Override
    public boolean isInline() {
        return inline;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getImageCID() {
        return imageCID;
    }
}
