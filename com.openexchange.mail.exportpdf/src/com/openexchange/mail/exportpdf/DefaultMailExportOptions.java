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

package com.openexchange.mail.exportpdf;

/**
 * {@link DefaultMailExportOptions}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class DefaultMailExportOptions implements MailExportOptions {

    private final int accountId;
    private final String destinationFolderId;
    private final String mailFolderId;
    private final String mailId;
    private final boolean decrypt;
    private final String decryptionToken;
    private final String hostname;
    private final String pageFormat;
    private final boolean preferRichText;
    private final boolean appendAttachmentPreviews;
    private final boolean embedNonConvertibleAttachments;
    private final boolean embedAttachmentPreviews;
    private final boolean embedRawAttachments;
    private final Boolean includeExternalImages;

    /**
     * Initialises a new {@link DefaultMailExportOptions}.
     *
     * @param builder The builder instance
     */
    private DefaultMailExportOptions(Builder builder) {
        super();
        destinationFolderId = builder.destinationFolderId;
        accountId = builder.accountId;
        mailFolderId = builder.mailFolderId;
        mailId = builder.mailId;
        decrypt = builder.decrypt;
        decryptionToken = builder.decryptionToken;
        hostname = builder.hostname;
        pageFormat = builder.pageFormat;
        preferRichText = builder.preferRichText;
        appendAttachmentPreviews = builder.appendConvertibleNonInlineAttachmentsAsPreviews;
        embedNonConvertibleAttachments = builder.embedNonConvertibleNonInlineAttachments;
        embedRawAttachments = builder.embedConvertibleNonInlineAttachments;
        embedAttachmentPreviews = builder.embedConvertibleNonInlineAttachmentsAsPreviews;
        includeExternalImages = builder.includeExternalImages;

    }

    @Override
    public String getDestinationFolderId() {
        return destinationFolderId;
    }

    @Override
    public int getAccountId() {
        return accountId;
    }

    @Override
    public String getMailFolderId() {
        return mailFolderId;
    }

    @Override
    public String getMailId() {
        return mailId;
    }

    @Override
    public boolean isEncrypted() {
        return decrypt;
    }

    @Override
    public String getDecryptionToken() {
        return decryptionToken;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public String getPageFormat() {
        return pageFormat;
    }

    @Override
    public boolean preferRichText() {
        return preferRichText;
    }

    @Override
    public boolean appendAttachmentPreviews() {
        return appendAttachmentPreviews;
    }

    @Override
    public boolean embedNonConvertibleAttachments() {
        return embedNonConvertibleAttachments;
    }

    @Override
    public boolean embedAttachmentPreviews() {
        return embedAttachmentPreviews;
    }

    @Override
    public boolean embedRawAttachments() {
        return embedRawAttachments;
    }
    
    @Override
    public Boolean includeExternalImages() {
        return includeExternalImages;
    }

    public static final class Builder {

        private String destinationFolderId;
        private int accountId;
        private String mailFolderId;
        private String mailId;
        private boolean decrypt;
        private String decryptionToken;
        private String hostname;
        private String pageFormat;
        private boolean preferRichText = true;
        private boolean appendConvertibleNonInlineAttachmentsAsPreviews = true;
        private boolean embedNonConvertibleNonInlineAttachments;
        private boolean embedConvertibleNonInlineAttachmentsAsPreviews;
        private boolean embedConvertibleNonInlineAttachments = true;
        private Boolean includeExternalImages;

        /**
         * Initialises a new {@link DefaultMailExportOptions.Builder}.
         */
        public Builder() {
            super();
        }

        /**
         * Sets the destinationFolderId
         *
         * @param destinationFolderId The destinationFolderId to set
         * @return this builder instance for chained calls
         */
        public Builder withDestinationFolderId(String destinationFolderId) {
            this.destinationFolderId = destinationFolderId;
            return this;
        }

        /**
         * Sets the accountId
         *
         * @param accountId The accountId to set
         * @return this builder instance for chained calls
         */
        public Builder withAccountId(int accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the mailFolderId
         *
         * @param mailFolderId The mailFolderId to set
         * @return this builder instance for chained calls
         */
        public Builder withMailFolderId(String mailFolderId) {
            this.mailFolderId = mailFolderId;
            return this;
        }

        /**
         * Sets the mailId
         *
         * @param mailId The mailId to set
         * @return this builder instance for chained calls
         */
        public Builder withMailId(String mailId) {
            this.mailId = mailId;
            return this;
        }

        /**
         * Sets the decrypt flag
         *
         * @param decrypt the decrypt flag
         * @return this builder instance for chained calls
         */
        public Builder withDecryptFlag(boolean decrypt) {
            this.decrypt = decrypt;
            return this;
        }

        /**
         * Sets the decryption token
         *
         * @param decryptionToken the decryption token to set
         * @return this builder instance for chained calls
         */
        public Builder withDecryptionToken(String decryptionToken) {
            this.decryptionToken = decryptionToken;
            return this;
        }

        /**
         * Sets the hostname
         *
         * @param hostname The hostname
         * @return this builder instance for chained calls
         */
        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * Sets the page format
         *
         * @param pageFormat The page format
         * @return this builder instance for chained calls
         */
        public Builder withPageFormat(String pageFormat) {
            this.pageFormat = pageFormat;
            return this;
        }

        /**
         * Sets the rich text
         *
         * @param richText the flag
         * @return this builder instance for chained calls
         */
        public Builder withRichText(boolean richText) {
            this.preferRichText = richText;
            return this;
        }

        /**
         * Sets the flag
         *
         * @param flag the flag to set
         * @return this builder instance for chained calls
         */
        public Builder withAppendAttachmentPreviews(boolean flag) {
            this.appendConvertibleNonInlineAttachmentsAsPreviews = flag;
            return this;
        }

        /**
         * Sets the flag
         *
         * @param flag the flag to set
         * @return this builder instance for chained calls
         */
        public Builder withEmbedAttachmentPreviews(boolean flag) {
            this.embedConvertibleNonInlineAttachmentsAsPreviews = flag;
            return this;
        }

        /**
         * Sets the flag
         *
         * @param flag the flag to set
         * @return this builder instance for chained calls
         */
        public Builder withEmbedNonConvertibleAttachments(boolean flag) {
            this.embedNonConvertibleNonInlineAttachments = flag;
            return this;
        }

        /**
         * Sets the flag
         *
         * @param flag the flag to set
         * @return this builder instance for chained calls
         */
        public Builder withEmbedRawAttachments(boolean flag) {
            this.embedConvertibleNonInlineAttachments = flag;
            return this;
        }
        
        /**
         * Sets the flag
         *
         * @param flag the flag to set
         * @return this builder instance for chained calls
         */
        public Builder withIncludeExternalImages(Boolean flag) {
            this.includeExternalImages = flag;
            return this;
        }

        /**
         * Builds the {@link MailExportOptions} instance
         *
         * @return the {@link MailExportOptions} instance
         */
        public MailExportOptions build() {
            return new DefaultMailExportOptions(this);
        }

    }

}
