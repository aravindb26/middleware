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

package com.openexchange.mail.exportpdf.gotenberg.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import com.openexchange.exception.OXException;
import com.openexchange.gotenberg.client.GotenbergClient.Document;
import com.openexchange.mail.exportpdf.MailExportMailPartContainer;

/**
 * {@link GotenbergDocument}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class GotenbergDocument implements Document {

    protected final MailExportMailPartContainer mailPart;

    /**
     * Initializes a new {@link GotenbergDocument}.
     *
     * @param mailPart The {@link MailExportMailPartContainer} to create the document from
     */
    public GotenbergDocument(MailExportMailPartContainer mailPart) {
        super();
        this.mailPart = Objects.requireNonNull(mailPart, "mailPart must not be null");
    }

    @Override
    public String getName() {
        return mailPart.getFileName();
    }

    @Override
    public String getContentType() {
        return mailPart.getBaseContentType();
    }

    @Override
    public InputStream getData() throws IOException {
        try {
            return mailPart.getInputStream();
        } catch (OXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return "GotenbergDocument [name=" + getName() + ", contentType=" + getContentType() + "]";
    }

}
