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

package com.openexchange.mail.exportpdf.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;

/**
 * {@link EmptyMailExportConversionResult} - An empty mail export conversion result
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class EmptyMailExportConversionResult implements MailExportConversionResult {

    private List<OXException> warnings;
    private final String title;
    private final Status status;

    /**
     * Initializes a new {@link EmptyMailExportConversionResult}.
     * 
     * @param status The status to indicate
     */
    public EmptyMailExportConversionResult(Status status) {
        this(status, null, null);
    }

    /**
     * Initializes a new {@link EmptyMailExportConversionResult}.
     * 
     * @param status The status to indicate
     * @param warnings The warnings, or <code>null</code> to omit
     * @param title The title to indicate, or <code>null</code> if there is none
     */
    public EmptyMailExportConversionResult(Status status, List<OXException> warnings, String title) {
        super();
        this.warnings = warnings;
        this.status = status;
        this.title = title;
    }

    @Override
    public void close() throws Exception {
        // nothing to do
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public PDFAVersion getPDFAVersion() {
        return null;
    }

    @Override
    public List<OXException> getWarnings() {
        return null == warnings ? Collections.emptyList() : warnings;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Optional<String> getTitle() {
        return Strings.isEmpty(title) ? Optional.empty() : Optional.of(title);
    }

    @Override
    public void addWarnings(List<OXException> warnings) {
        if (this.warnings.isEmpty()) {
            this.warnings = new LinkedList<>();
        }
        this.warnings.addAll(warnings);
    }
}
