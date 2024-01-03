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
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.MailExportOptions;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import rst.pdfbox.layout.elements.render.RenderContext;

/**
 * {@link AttachmentWriter}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface AttachmentWriter extends Closeable {

    /**
     * Writes the specified result to the specified document
     * 
     * @param result The result to write
     * @param options The mail export options
     * @param renderCotnext The render context
     */
    void writeAttachment(MailExportConversionResult result, MailExportOptions options, RenderContext renderContext);

    /**
     * Whether this attachment writer can handle the specified result
     * 
     * @param result The result to handle
     * @return <code>true</code> if the result can be handled; <code>false</code> otherwise
     * @throws OXException if an error is occurred
     */
    boolean handles(MailExportConversionResult result) throws OXException;
}
