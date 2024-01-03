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

package com.openexchange.mail.exportpdf.converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;

/**
 * {@link FileHolderConversionResult}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class FileHolderConversionResult implements MailExportConversionResult {

    private final ThresholdFileHolder fileHolder;
    private List<OXException> warnings;
    private final Status status;
    private final PDFAVersion pdfVersion;

    public FileHolderConversionResult(Status status, PDFAVersion pdfVersion, ThresholdFileHolder fileHolder) {
        this(status, pdfVersion, fileHolder, Collections.emptyList());
    }

    public FileHolderConversionResult(Status status, PDFAVersion pdfVersion, ThresholdFileHolder fileHolder, List<OXException> warnings) {
        super();
        this.status = status;
        this.pdfVersion = pdfVersion;
        this.fileHolder = fileHolder;
        this.warnings = warnings;
    }

    public ThresholdFileHolder getFileHolder() {
        return fileHolder;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void close() throws Exception {
        Streams.close(fileHolder);
    }

    @Override
    public List<OXException> getWarnings() {
        return warnings;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return fileHolder.getStream();
        } catch (OXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getContentType() {
        return fileHolder.getContentType();
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(fileHolder.getName());
    }

    @Override
    public PDFAVersion getPDFAVersion() {
        return pdfVersion;
    }

    @Override
    public void addWarnings(List<OXException> warnings) {
        if (this.warnings.isEmpty()) {
            this.warnings = new LinkedList<>();
        }
        this.warnings.addAll(warnings);
    }
}
