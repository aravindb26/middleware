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

package com.openexchange.filestore;

import java.net.URI;

/**
 * {@link FileStorageFilter} - A filter when listing available file storage for a certain base URI.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface FileStorageFilter {

    /**
     * The special filter accepting all file storages.
     */
    public static final FileStorageFilter ACCEPTS_ALL = new FileStorageFilter() {

        @Override
        public boolean considerUserFileStorages() {
            return true;
        }

        @Override
        public boolean considerCustomFileStorages() {
            return true;
        }

        @Override
        public boolean considerContextFileStorages() {
            return true;
        }

        @Override
        public boolean acceptUserFileStorages(URI uri) {
            return true;
        }

        @Override
        public boolean acceptCustomFileStorages(URI uri) {
            return true;
        }

        @Override
        public boolean acceptContextFileStorages(URI uri) {
            return true;
        }
    };

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Whether this filter accepts context-associated file storages at all.
     *
     * @return <code>true</code> if accepted; otherwise <code>false</code> to discard
     */
    boolean considerContextFileStorages();

    /**
     * Whether this filter accepts user-associated file storages at all.
     *
     * @return <code>true</code> if accepted; otherwise <code>false</code> to discard
     */
    boolean considerUserFileStorages();

    /**
     * Whether this filter accepts custom file storages at all.
     *
     * @return <code>true</code> if accepted; otherwise <code>false</code> to discard
     */
    boolean considerCustomFileStorages();

    /**
     * Whether the context-associated file storage belonging to given fully-qualifying URI is accepted by this filter.
     * <p>
     * URI path starts with prefix:
     * <pre>
     * &lt;context-id&gt; + "_ctx_store"
     * </pre>
     *
     * @param uri The URI to examine
     * @return <code>true</code> if accepted; otherwise <code>false</code> to discard
     */
    boolean acceptContextFileStorages(URI uri);

    /**
     * Whether the user-associated file storage belonging to given fully-qualifying URI is accepted by this filter.
     * <p>
     * URI path starts with prefix:
     * <pre>
     * &lt;context-id&gt; + "_ctx_" + &lt;user-id&gt; + "_user_store"
     * </pre>
     *
     * @param uri The URI to examine
     * @return <code>true</code> if accepted; otherwise <code>false</code> to discard
     */
    boolean acceptUserFileStorages(URI uri);

    /**
     * Whether the custom file storage belonging to given fully-qualifying URI is accepted by this filter.
     *
     * @param uri The URI to examine
     * @return <code>true</code> if accepted; otherwise <code>false</code> to discard
     */
    boolean acceptCustomFileStorages(URI uri);

}
