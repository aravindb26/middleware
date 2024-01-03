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

package com.openexchange.tika.util;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TikaUtils} - A utility class encapsulating Tika functionality
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class TikaUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TikaUtils.class);

    private static final Tika TIKA;
    static {
        TIKA = new Tika(TikaConfig.getDefaultConfig());
    }

    /**
     * Private constructor to avoid instantiation
     */
    private TikaUtils() {}

    /**
     * Detects the MIME type from given input stream using <a href="http://tika.apache.org/">Apache Tika - a content analysis toolkit</a>.
     *
     * @param stream The input stream
     * @return The detected mime type as string or <code>null</code> if the provided InputStream is <code>null</code> or "application/octet-stream" in case of errors
     */
    public static String detectMimeType(final InputStream stream) {
        if (null == stream) {
            return null;
        }

        InputStream in = stream;
        InputStream inToUse = null;
        try {
            Detector detector = TIKA.getDetector();
            if (false == (detector instanceof CompositeDetector)) {
                if (in.markSupported()) {
                    return detector.detect(in, new Metadata()).toString();
                }
                return detector.detect(new BufferedInputStream(in), new Metadata()).toString();
            }

            if (in.markSupported()) {
                inToUse = in;
                in = null;
            } else {
                inToUse = new BufferedInputStream(in);
                in = null;
            }
            for (Detector d : ((CompositeDetector) detector).getDetectors()) {
                if (d instanceof MimeTypes) {
                    MediaType mediaType = d.detect(inToUse, new Metadata());
                    if (null != mediaType && false == MediaType.OCTET_STREAM.equals(mediaType)) {
                        return mediaType.toString();
                    }
                }
            }

            // As last resort
            return TIKA.detect(inToUse);
        } catch (IOException e) {
            LOG.error("An error occurred while detecting the mime type.", e);
        } finally {
            close(inToUse, in);
        }
        return "application/octet-stream";
    }

    private static void close(final Closeable... closeables) {
        if (null != closeables) {
            for (final Closeable toClose : closeables) {
                if (null != toClose) {
                    try {
                        toClose.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    }

    /**
     * Tries to convert the given (HTML) input stream to text.
     *
     * @param inputStream the HTML content to convert
     * @return String representation of the provided HTML content or <code>null</code> in case of errors
     */
    public static String html2text(final InputStream inputStream) {
        try {
            final Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_ENCODING, "UTF-8");
            metadata.set(Metadata.CONTENT_TYPE, "text/html");
            return TIKA.parseToString(inputStream, metadata);
        } catch (IOException | TikaException e) {
            LOG.error("Error during html2text conversion.", e);
        }
        return null;
    }
}
