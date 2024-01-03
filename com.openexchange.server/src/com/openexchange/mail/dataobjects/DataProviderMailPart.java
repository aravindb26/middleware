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


package com.openexchange.mail.dataobjects;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.HeaderCollection;


/**
 * {@link DataProviderMailPart} - The mail part implementation accepting arbitrary data provider and delegating to a given mail part.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class DataProviderMailPart extends MailPart {

    private static final long serialVersionUID = 4798424499998347220L;

    /** Provides binary data for a mail part */
    public static interface DataProvider {

        /**
         * Gets the binary data of a mail part.
         *
         * @return The binary data as a stream.
         * @throws OXException If stream cannot be returned
         */
        InputStream getInputStream() throws OXException;

        /**
         * Gets the data's size (if known).
         *
         * @return The size in bytes or <code>-1</code>
         */
        long getSize();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final String TEXT = "text/";

    private final MailPart mailPart;
    private final DataProvider dataProvider;
    private transient Object cachedContent;
    private transient DataSource dataSource;

    /**
     * Initializes a new {@link DataProviderMailPart}.
     *
     * @param mailPart The mail part to delegate to
     * @param dataProvider The data provider
     */
    public DataProviderMailPart(MailPart mailPart, DataProvider dataProvider) {
        super();
        this.mailPart = mailPart;
        this.dataProvider = dataProvider;
    }

    private DataSource getDataSource() {
        /*
         * Lazy creation
         */
        if (null == dataSource) {
            ContentType contentType = getContentType();
            if (contentType.startsWith(TEXT) && contentType.getCharsetParameter() == null) {
                contentType.setCharsetParameter(MailProperties.getInstance().getDefaultMimeCharset());
            }
            dataSource = new DataProviderDataSource(dataProvider, contentType.toString());
        }
        return dataSource;
    }

    @Override
    public Object getContent() throws OXException {
        if (cachedContent != null) {
            return cachedContent;
        }
        ContentType contentType = getContentType();
        if (contentType.startsWith(TEXT)) {
            try {
                String charset = contentType.getCharsetParameter();
                if (Strings.isEmpty(charset)) {
                    charset = MailProperties.getInstance().getDefaultMimeCharset();
                }
                cachedContent = Streams.stream2string(getInputStream(), charset);
                return cachedContent;
            } catch (IOException e) {
                throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
            }
        }
        return null;
    }

    @Override
    public DataHandler getDataHandler() throws OXException {
        return new DataHandler(getDataSource());
    }

    @Override
    public InputStream getInputStream() throws OXException {
        return dataProvider.getInputStream();
    }

    @Override
    public int getEnclosedCount() throws OXException {
        return NO_ENCLOSED_PARTS;
    }

    @Override
    public boolean hasEnclosedParts() throws OXException {
        return false;
    }

    @Override
    public MailPart getEnclosedMailPart(int index) throws OXException {
        return null;
    }

    @Override
    public void loadContent() throws OXException {
        // Nothing
    }

    @Override
    public void prepareForCaching() {
        // Nothing
    }

    @Override
    public boolean containsSize() {
        return true;
    }

    @Override
    public long getSize() {
        return dataProvider.getSize();
    }

    @Override
    public void setSize(long size) {
        // Nothing
    }

    @Override
    public void removeSize() {
        // Nothing
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public ContentType getContentType() {
        return mailPart.getContentType();
    }

    @Override
    public boolean containsContentType() {
        return mailPart.containsContentType();
    }

    @Override
    public void removeContentType() {
        mailPart.removeContentType();
    }

    @Override
    public void setContentType(ContentType contentType) {
        mailPart.setContentType(contentType);
    }

    @Override
    public void setContentType(String contentType) throws OXException {
        mailPart.setContentType(contentType);
    }

    @Override
    public ContentDisposition getContentDisposition() {
        return mailPart.getContentDisposition();
    }

    @Override
    public boolean containsContentDisposition() {
        return mailPart.containsContentDisposition();
    }

    @Override
    public void removeContentDisposition() {
        mailPart.removeContentDisposition();
    }

    @Override
    public void setContentDisposition(String disposition) throws OXException {
        mailPart.setContentDisposition(disposition);
    }

    @Override
    public void setContentDisposition(ContentDisposition disposition) {
        mailPart.setContentDisposition(disposition);
    }

    @Override
    public String getFileName() {
        return mailPart.getFileName();
    }

    @Override
    public boolean containsFileName() {
        return mailPart.containsFileName();
    }

    @Override
    public void removeFileName() {
        mailPart.removeFileName();
    }

    @Override
    public void setFileName(String fileName) {
        mailPart.setFileName(fileName);
    }

    @Override
    public void addHeader(String name, String value) {
        mailPart.addHeader(name, value);
    }

    @Override
    public String toString() {
        return mailPart.toString();
    }

    @Override
    public void setHeader(String name, String value) {
        mailPart.setHeader(name, value);
    }

    @Override
    public void addHeaders(HeaderCollection headers) {
        mailPart.addHeaders(headers);
    }

    @Override
    public boolean containsHeaders() {
        return mailPart.containsHeaders();
    }

    @Override
    public void removeHeaders() {
        mailPart.removeHeaders();
    }

    @Override
    public int getHeadersSize() {
        return mailPart.getHeadersSize();
    }

    @Override
    public Iterator<Entry<String, String>> getHeadersIterator() {
        return mailPart.getHeadersIterator();
    }

    @Override
    public boolean containsHeader(String name) {
        return mailPart.containsHeader(name);
    }

    @Override
    public String[] getHeader(String name) {
        return mailPart.getHeader(name);
    }

    @Override
    public String getFirstHeader(String name) {
        return mailPart.getFirstHeader(name);
    }

    @Override
    public String getHeader(String name, String delimiter) {
        return mailPart.getHeader(name, delimiter);
    }

    @Override
    public String getHeader(String name, char delimiter) {
        return mailPart.getHeader(name, delimiter);
    }

    @Override
    public HeaderCollection getHeaders() {
        return mailPart.getHeaders();
    }

    @Override
    public Iterator<Entry<String, String>> getNonMatchingHeaders(String[] nonMatchingHeaders) {
        return mailPart.getNonMatchingHeaders(nonMatchingHeaders);
    }

    @Override
    public Iterator<Entry<String, String>> getMatchingHeaders(String[] matchingHeaders) {
        return mailPart.getMatchingHeaders(matchingHeaders);
    }

    @Override
    public void removeHeader(String name) {
        mailPart.removeHeader(name);
    }

    @Override
    public boolean hasHeaders(String... names) {
        return mailPart.hasHeaders(names);
    }

    @Override
    public String getContentId() {
        return mailPart.getContentId();
    }

    @Override
    public boolean containsContentId() {
        return mailPart.containsContentId();
    }

    @Override
    public void removeContentId() {
        mailPart.removeContentId();
    }

    @Override
    public void setContentId(String contentId) {
        mailPart.setContentId(contentId);
    }

    @Override
    public String getSequenceId() {
        return mailPart.getSequenceId();
    }

    @Override
    public boolean containsSequenceId() {
        return mailPart.containsSequenceId();
    }

    @Override
    public void removeSequenceId() {
        mailPart.removeSequenceId();
    }

    @Override
    public void setSequenceId(String sequenceId) {
        mailPart.setSequenceId(sequenceId);
    }

    @Override
    public MailPath getMsgref() {
        return mailPart.getMsgref();
    }

    @Override
    public boolean containsMsgref() {
        return mailPart.containsMsgref();
    }

    @Override
    public void removeMsgref() {
        mailPart.removeMsgref();
    }

    @Override
    public void setMsgref(MailPath msgref) {
        mailPart.setMsgref(msgref);
    }

    @Override
    public void writeTo(OutputStream out) throws OXException {
        mailPart.writeTo(out);
    }

    @Override
    public String getSource() throws OXException {
        return mailPart.getSource();
    }

    @Override
    public byte[] getSourceBytes() throws OXException {
        return mailPart.getSourceBytes();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class DataProviderDataSource implements DataSource {

        private final DataProvider dataProvider;
        private final String contentType;

        /**
         * Initializes a new {@link DataProviderDataSource}.
         *
         * @param dataProvider The data provider
         * @param contentType The MIME type
         */
        DataProviderDataSource(DataProvider dataProvider, String contentType) {
            super();
            this.dataProvider = dataProvider;
            this.contentType = contentType;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return dataProvider.getInputStream();
            } catch (OXException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                throw new IOException(e);
            }
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

}
