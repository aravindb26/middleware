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

package com.openexchange.mail.exportpdf.impl.pdf;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.openexchange.mail.exportpdf.converter.MailExportConversionResult;
import com.openexchange.mail.exportpdf.converter.PDFAVersion;

/**
 * {@link PDFVersionParser} - Parses the {@link PDFAVersion} from PDF data
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class PDFAVersionParser {

    private static final Logger LOG = LoggerFactory.getLogger(PDFAVersionParser.class);

    /**
     * {@link PDFAMetadataHandler} - A SAX handler to parse the PDF/A identification from the PDF'x XMP metadata
     */
    private static class PDFAMetadataHandler extends DefaultHandler {

        private static final String PDFA_CONFORMANCE_TAG = "pdfaid:conformance";
        private static final String PDFA_PART_TAG = "pdfaid:part";

        private StringBuilder data;
        private int part;
        private String conformance;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (data != null) {
                data.append(ch, start, length);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (PDFA_CONFORMANCE_TAG.equals(qName) || PDFA_PART_TAG.equals(qName)) {
                data = new StringBuilder();
            } else {
                data = null;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (data == null) {
                return;
            }

            if (PDFA_CONFORMANCE_TAG.equals(qName)) {
                conformance = data.toString();
            } else if (PDFA_PART_TAG.equals(qName)) {
                try {
                    part = Integer.parseInt(data.toString());
                } catch (NumberFormatException e) {
                    throw new SAXException(e.getMessage(), e);
                }
            }

            data = null;
        }
    }

    /**
     * Initialises a new {@link PDFAVersionParser}.
     */
    private PDFAVersionParser() {
        super();
    }

    /**
     * Parses the {@link PDFAVersion} from the given {@link InputStream}
     *
     * @param s The {@link InputStream} to parse the {@link PDFAVersion} from
     * @return The {@link PDFAVersion} parsed from the given {@link InputStream}
     */
    public static PDFAVersion parseVersion(InputStream s) {
        try (PDDocument document = PDDocument.load(s)) {
            /* Check if metadata are present */
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metadata = catalog.getMetadata();
            if (metadata == null) {
                /* We do not have any meta data indicating a PDF/A document */
                return PDFAVersion.UNKNOWN;
            }

            /* parse the PDF/A identification from the metadata */
            try (COSInputStream metaDataStream = metadata.createInputStream()) {
                SAXParser xmlParser = SAXParserFactory.newInstance().newSAXParser();
                var handler = new PDFAMetadataHandler();
                xmlParser.parse(metaDataStream, handler);
                return PDFAVersion.parse(I(handler.part), handler.conformance);
            } catch (Exception e) {
                LOG.error("Unable to parse PDF/A meta data: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            LOG.error("Unable to read PDF/A version from PDF data: " + e.getMessage(), e);
        }
        return PDFAVersion.UNKNOWN;
    }

    /**
     * Parses the {@link PDFAVersion} from the given {@link MailExportConversionResult}
     *
     * @param result The {@link MailExportConversionResult} to parse the {@link PDFAVersion} from
     * @return The {@link PDFAVersion} parsed from the given {@link MailExportConversionResult}
     */
    public static PDFAVersion parseVersion(MailExportConversionResult result) {
        try (InputStream data = result.getInputStream()) {
            return parseVersion(data);
        } catch (IOException e) {
            LOG.error("Unable to read PDF/A version from PDF data: " + e.getMessage(), e);
            return PDFAVersion.UNKNOWN;
        }
    }
}
