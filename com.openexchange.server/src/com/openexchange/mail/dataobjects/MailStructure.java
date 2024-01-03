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

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;

/**
 * {@link MailStructure} - Represents the structure of a mail message,
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class MailStructure {

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder for an instance of <code>MailStructure</code> */
    public static class Builder {

        private ContentType contentType;
        private ContentDisposition contentDisposition;
        private long size;
        private String contentId;
        private final List<MailStructure> bodies;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
            size = -1;
            bodies = new ArrayList<>(4);
        }

        /**
         * Sets the content type
         *
         * @param contentType The content type to set
         * @return This builder
         */
        public Builder withContentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the content disposition
         *
         * @param contentDisposition The content disposition to set
         * @return This builder
         */
        public Builder withContentDisposition(ContentDisposition contentDisposition) {
            this.contentDisposition = contentDisposition;
            return this;
        }

        /**
         * Sets the size
         *
         * @param size The size to set or <code>-1</code>
         * @return This builder
         */
        public Builder withSize(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the Content-Id header value
         *
         * @param contentId The Content-Id header value to set
         * @return This builder
         */
        public Builder withContentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        /**
         * Adds specified body to this structure.
         *
         * @param body The body to add
         * @return This builder
         */
        public Builder addBody(MailStructure body) {
            bodies.add(body);
            return this;
        }

        /**
         * Removes specified body from this structure.
         *
         * @param body The body to remove
         * @return This builder
         */
        public Builder removeBody(MailStructure body) {
            bodies.remove(body);
            return this;
        }

        /**
         * Builds the instance of <code>MailStructure</code> from this builder's arguments.
         *
         * @return The instance of <code>MailStructure</code>
         */
        public MailStructure build() {
            return new MailStructure(contentType, contentDisposition, size, contentId, bodies);
        }

    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final ContentType contentType;
    private final ContentDisposition contentDisposition;
    private final long size;
    private final String contentId;
    private final List<MailStructure> bodies;

    /**
     * Initializes a new {@link MailStructure}.
     *
     * @param contentType The content type
     * @param contentDisposition The content disposition
     * @param size The size or <code>-1</code>
     * @param contentId The Content-Id header value
     * @param bodies The nested bodies
     */
    MailStructure(ContentType contentType, ContentDisposition contentDisposition, long size, String contentId, List<MailStructure> bodies) {
        super();
        this.contentType = contentType;
        this.contentDisposition = contentDisposition;
        this.size = size;
        this.contentId = contentId;
        this.bodies = bodies == null ? ImmutableList.of() : ImmutableList.copyOf(bodies);
    }

    /**
     * Gets the content type
     *
     * @return The content type
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Gets the content disposition
     *
     * @return The content disposition
     */
    public ContentDisposition getContentDisposition() {
        return contentDisposition;
    }

    /**
     * Gets the size
     *
     * @return The size
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets the Content-Id header value
     *
     * @return The Content-Id header value
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Gets the immutable list of sub-bodies.
     *
     * @return The sub-bodies
     */
    public List<MailStructure> getBodies() {
        return bodies;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (contentType != null) {
            sb.append("contentType=").append(contentType).append(", ");
        }
        if (contentDisposition != null) {
            sb.append("contentDisposition=").append(contentDisposition).append(", ");
        }
        sb.append("size=").append(size).append(", ");
        if (contentId != null) {
            sb.append("contentId=").append(contentId).append(", ");
        }
        if (bodies != null) {
            sb.append("bodies=").append(bodies);
        }
        sb.append('}');
        return sb.toString();
    }



}
