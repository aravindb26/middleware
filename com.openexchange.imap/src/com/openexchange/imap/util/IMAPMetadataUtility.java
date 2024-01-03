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

package com.openexchange.imap.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.sun.mail.imap.protocol.BASE64MailboxDecoder;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * {@link IMAPMetadataUtility} - Utility for parsing IMAP METADATA response.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class IMAPMetadataUtility {

    /**
     * Initializes a new {@link IMAPMetadataUtility}.
     */
    private IMAPMetadataUtility() {
        super();
    }

    /**
     * {@link Metadata} - Represents a parsed METADATA response
     * <p>
     * Example:
     * <pre>
     * * METADATA INBOX (/private/vendor/vendor.open-xchange/mycomment {17} This is a comment)
     * </pre>
     */
    public static class Metadata {

        private final String fullName;
        private final Map<String, String> keyValuePairs;

        /**
         * Initializes a new {@link MetadataResponse}.
         *
         * @param fullName The mailbox full name
         * @param keyValuePairs The key-value-pairs of the METADATA response
         */
        Metadata(String fullName, Map<String, String> keyValuePairs) {
            super();
            this.fullName = fullName;
            this.keyValuePairs = keyValuePairs;
        }

        /**
         * Gets the mailbox full name.
         *
         * @return The mailbox full name
         */
        public String getFullName() {
            return fullName;
        }

        /**
         * Gets the key-value-pairs of the METADATA response.
         *
         * @return The key-value-pairs of the METADATA response or <code>null</code>
         */
        public Map<String, String> getKeyValuePairs() {
            return keyValuePairs;
        }

    }

    /**
     * Parses given METADATA response.
     *
     * @param metadataResponse The IMAP METADATA response
     * @return The parsed METADATA response
     */
    public static Metadata parseMetadata(IMAPResponse metadataResponse) {
        String fullName = metadataResponse.readAtomString();
        if (!metadataResponse.supportsUtf8()) {
            fullName = BASE64MailboxDecoder.decode(fullName);
        }

        Map<String, String> keyValuePairs = readMetadataList(metadataResponse);
        return new Metadata(fullName, keyValuePairs);
    }

    /**
     * Parses given METADATA response containing only matching keys.
     *
     * @param metadataResponse The IMAP METADATA response
     * @return The parsed METADATA response containing only matching keys
     */
    public static Metadata parseMatchingMetadata(IMAPResponse metadataResponse, String... metadataKeys) {
        String fullName = metadataResponse.readAtomString();
        if (!metadataResponse.supportsUtf8()) {
            fullName = BASE64MailboxDecoder.decode(fullName);
        }

        Map<String, String> keyValuePairs = readMatchingMetadataList(metadataResponse, metadataKeys);
        return new Metadata(fullName, keyValuePairs);
    }

    /**
     * Parses the key-value-pairs portion of the given IMAP METADATA response.
     *
     * @param metadataResponse The IMAP METADATA response
     * @param metadataKeys The METADATA keys to filter by
     * @return The parsed mapping of key-value-pairs or <code>null</code>
     */
    public static Map<String, String> readMatchingMetadataList(IMAPResponse metadataResponse, String... metadataKeys) {
        if (metadataKeys == null || metadataKeys.length <= 0) {
            return Collections.emptyMap();
        }

        Set<String> keys = Arrays.stream(metadataKeys).filter(Objects::nonNull).collect(Collectors.toSet());
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        return doReadMetadataList(metadataResponse, new SetBackedKeyMatcher(keys));
    }

    /**
     * Parses the key-value-pairs portion of the given IMAP METADATA response.
     *
     * @param metadataResponse The IMAP METADATA response
     * @return The parsed mapping of key-value-pairs or <code>null</code>
     */
    public static Map<String, String> readMetadataList(IMAPResponse metadataResponse) {
        return doReadMetadataList(metadataResponse, ACCEPT_ALL_MATCHER);
    }

    private static Map<String, String> doReadMetadataList(IMAPResponse metadataResponse, KeyMatcher keyMatcher) {
        // Assume mailbox name already parsed
        // * METADATA INBOX (/private/vendor/vendor.open-xchange/deputy {149} {"c0722dee6cd04eb48f4c0b2af95cd0a2":{"version": 1,"name":"jane.doe", ... }})
        if (metadataResponse.isNextNonSpace('(') == false) {
            // Not what we expected
            return null;
        }

        Map<String, String> kvps = null;
        String key = null;
        while (metadataResponse.isNextNonSpace(')') == false) {
            if (key == null) {
                key = metadataResponse.readAtomString();
                if (keyMatcher.isNotAccepted(key)) {
                    // Non-accepted key. Discard value and continue.
                    metadataResponse.readString();
                    key = null;
                }
            } else {
                String value = metadataResponse.readString();
                if (value != null) {
                    if (kvps == null) {
                        kvps = new LinkedHashMap<String, String>(8);
                    }
                    kvps.put(key, value);
                }
                key = null;
            }
        }
        return kvps;
    }

    // -------------------------------------------------------- KeyMatcher -----------------------------------------------------------------

    private static interface KeyMatcher {

        default boolean isNotAccepted(String metadataKey) {
            return !isAccepted(metadataKey);
        }

        boolean isAccepted(String metadataKey);
    }

    private static final KeyMatcher ACCEPT_ALL_MATCHER = metadataKey -> true;

    private static class SetBackedKeyMatcher implements KeyMatcher {

        private final Set<String> acceptedKeys;

        SetBackedKeyMatcher(Set<String> acceptedKeys) {
            super();
            this.acceptedKeys = acceptedKeys;
        }

        @Override
        public boolean isAccepted(String metadataKey) {
            return acceptedKeys.contains(metadataKey);
        }
    }

}
