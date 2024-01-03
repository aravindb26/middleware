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

package com.openexchange.mail.compose;

import java.io.InputStream;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;

/**
 * {@link AttachmentDescriptionAndData} - A tuple of attachment description and attachment's data.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class AttachmentDescriptionAndData {

    /**
     * Creates a new instance of <code>AttachmentDescriptionAndData</code> from given input stream.
     *
     * @param data The attachment's data
     * @param attachmentDescription The attachment description
     * @return
     */
    public static AttachmentDescriptionAndData newInstanceFromInputStream(InputStream data, AttachmentDescription attachmentDescription) {
        return new AttachmentDescriptionAndData(attachmentDescription, data, null);
    }

    /**
     * Creates a new instance of <code>AttachmentDescriptionAndData</code> from given data provider.
     *
     * @param dataProvider The provider for attachment's data
     * @param attachmentDescription The attachment description
     * @return The newly created instance
     */
    public static AttachmentDescriptionAndData newInstanceFromDataProvider(DataProvider dataProvider, AttachmentDescription attachmentDescription) {
        return new AttachmentDescriptionAndData(attachmentDescription, null, dataProvider);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final AttachmentDescription attachmentDescription;
    private final InputStream data;
    private final DataProvider dataProvider;

    /**
     * Initializes a new {@link AttachmentDescriptionAndData}.
     *
     * @param attachmentDescription The attachment description
     * @param data The attachment's data
     */
    private AttachmentDescriptionAndData(AttachmentDescription attachmentDescription, InputStream data, DataProvider dataProvider) {
        super();
        this.attachmentDescription = attachmentDescription;
        this.data = data;
        this.dataProvider = dataProvider;
    }

    /**
     * Initializes a new {@link AttachmentDescriptionAndData}.
     *
     * @param attachmentDescription The attachment description
     * @param dataProvider The provider for attachment's data
     */
    private AttachmentDescriptionAndData(AttachmentDescription attachmentDescription, DataProvider dataProvider) {
        super();
        this.attachmentDescription = attachmentDescription;
        data = null;
        this.dataProvider = dataProvider;
    }

    /**
     * Gets the attachment description
     *
     * @return The attachment description
     */
    public AttachmentDescription getAttachmentDescription() {
        return attachmentDescription;
    }

    /**
     * Gets the attachment's data
     *
     * @return The attachment's data
     * @throws OXException If data cannot be returned
     */
    public InputStream getData() throws OXException {
        return dataProvider != null ? dataProvider.getData() : data;
    }

    /**
     * Closes attachment's data input stream if this instance has been initialized using
     * {@link #newInstanceFromInputStream(InputStream, AttachmentDescription) newInstanceFromInputStream()}.
     */
    public void closeIfNecessary() {
        if (data != null) {
            Streams.close(data);
        }
    }

}
