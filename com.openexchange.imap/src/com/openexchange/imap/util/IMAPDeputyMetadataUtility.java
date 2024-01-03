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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.Store;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.imap.util.IMAPMetadataUtility.Metadata;
import com.openexchange.mail.MailExceptionCode;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.iap.ResponseInterceptor;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.BASE64MailboxDecoder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link IMAPDeputyMetadataUtility} - Utility class for deputy metadata.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class IMAPDeputyMetadataUtility {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(IMAPDeputyMetadataUtility.class);
    }

    /**
     * Initializes a new {@link IMAPDeputyMetadataUtility}.
     */
    private IMAPDeputyMetadataUtility() {
        super();
    }

    private static final String METADATA_KEY = "/shared/vendor/vendor.open-xchange/deputy";

    /**
     * Sets the deputy meta-data in JSON format for given IMAP folder.
     *
     * @param jDeputyMetadata The deputy meta-data in JSON format to set
     * @param imapFolder The IMAP folder
     * @throws MessagingException If deputy meta-data in JSON format cannot be set
     */
    public static void setDeputyMetadata(JSONObject jDeputyMetadata, IMAPFolder imapFolder) throws MessagingException {
        final Store store = imapFolder.getStore();
        imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                // Check for "METADATA" capability
                if (!protocol.hasCapability("METADATA")) {
                    LoggerHolder.LOG.warn("Cannot set deputy metadata since \"METADATA\" extension is not supported by IMAP server: {}", store.toString());
                    return Boolean.FALSE;
                }

                Argument args = new Argument();
                if (protocol.supportsUtf8()) {
                    args.writeString(imapFolder.getFullName(), StandardCharsets.UTF_8);
                } else {
                    // encode the mbox as per RFC2060
                    args.writeString(IMAPCommandsCollection.prepareStringArgument(imapFolder.getFullName()));
                }
                Argument tokens = new Argument();
                tokens.writeAtom(METADATA_KEY);
                tokens.writeString(jDeputyMetadata.toString());
                args.writeArgument(tokens);

                Response[] r = IMAPCommandsCollection.performCommand(protocol, "SETMETADATA", args);
                Response response = r[r.length - 1];
                if (response.isOK()) {
                    protocol.notifyResponseHandlers(r);
                    return Boolean.TRUE;
                }

                protocol.notifyResponseHandlers(r);
                protocol.handleResult(response, "METADATA");
                return Boolean.FALSE;
            }
        });
    }

    /**
     * Unsets the deputy meta-data in JSON format from given IMAP folder.
     *
     * @param imapFolder The IMAP folder
     * @throws MessagingException If deputy meta-data in JSON format cannot be unset
     */
    public static void unsetDeputyMetadata(IMAPFolder imapFolder) throws MessagingException {
        final Store store = imapFolder.getStore();
        imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                // Check for "METADATA" capability
                if (!protocol.hasCapability("METADATA")) {
                    LoggerHolder.LOG.warn("Cannot unset deputy metadata since \"METADATA\" extension is not supported by IMAP server: {}", store.toString());
                    return Boolean.FALSE;
                }

                Argument args = new Argument();
                if (protocol.supportsUtf8()) {
                    args.writeString(imapFolder.getFullName(), StandardCharsets.UTF_8);
                } else {
                    // encode the mbox as per RFC2060
                    args.writeString(IMAPCommandsCollection.prepareStringArgument(imapFolder.getFullName()));
                }
                Argument tokens = new Argument();
                tokens.writeAtom(METADATA_KEY);
                tokens.writeAtom("NIL");
                args.writeArgument(tokens);

                Response[] r = IMAPCommandsCollection.performCommand(protocol, "SETMETADATA", args);
                Response response = r[r.length - 1];
                if (response.isOK()) {
                    protocol.notifyResponseHandlers(r);
                    return Boolean.TRUE;
                }

                protocol.notifyResponseHandlers(r);
                protocol.handleResult(response, "METADATA");
                return Boolean.FALSE;
            }
        });
    }

    /**
     * Retrieves the optional deputy meta-data in JSON format from given IMAP folder.
     *
     * @param imapFolder The IMAP folder
     * @return The optional deputy meta-data in JSON format
     * @throws MessagingException If deputy meta-data in JSON format cannot be returned
     */
    public static Optional<JSONObject> getDeputyMetadata(IMAPFolder imapFolder) throws MessagingException {
        final Store store = imapFolder.getStore();
        return Optional.ofNullable((JSONObject) imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                // Check for "METADATA" capability
                if (!protocol.hasCapability("METADATA")) {
                    LoggerHolder.LOG.warn("Cannot retrieve deputy metadata since \"METADATA\" extension is not supported by IMAP server: {}", store.toString());
                    return null;
                }

                // Encode the mbox as per RFC2060
                String mbox = protocol.supportsUtf8() ? imapFolder.getFullName() : IMAPCommandsCollection.prepareStringArgument(imapFolder.getFullName());

                Response[] r = IMAPCommandsCollection.performCommand(protocol, new StringBuilder(32).append("GETMETADATA ").append(mbox).append(" (").append(METADATA_KEY).append(')').toString());
                Response response = r[r.length - 1];
                JSONObject jDeputyMetadata = null;
                if (response.isOK()) {
                    for (int i = 0, len = r.length - 1; i < len; i++) {
                        if (!(r[i] instanceof IMAPResponse)) {
                            continue;
                        }
                        IMAPResponse ir = (IMAPResponse) r[i];
                        if (ir.keyEquals("METADATA")) {
                            r[i] = null;
                        }
                        if (jDeputyMetadata == null) {
                            String fullName = ir.readAtomString();
                            if (!ir.supportsUtf8()) {
                                fullName = BASE64MailboxDecoder.decode(fullName);
                            }
                            if (fullName.equals(imapFolder.getFullName())) {
                                // * METADATA INBOX (/private/deputy NIL)
                                // * METADATA INBOX (/private/deputy {76} {...})
                                Map<String, String> keyValuePairs = IMAPMetadataUtility.readMatchingMetadataList(ir, METADATA_KEY);
                                if (keyValuePairs != null) {
                                    String value = keyValuePairs.get(METADATA_KEY);
                                    if (value != null) {
                                        try {
                                            jDeputyMetadata = JSONServices.parseObject(value);
                                        } catch (Exception e) {
                                            throw new ProtocolException("Invalid JSON deputy metadata: " + value, e);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dispatch remaining untagged responses
                    protocol.notifyResponseHandlers(r);
                } else {
                    protocol.notifyResponseHandlers(r);
                    protocol.handleResult(response, "METADATA");
                }
                return jDeputyMetadata;
            }
        }));
    }

    /**
     * Retrieves the entity identifiers from deputy meta-data from given IMAP folder.
     *
     * @param imapFolder The IMAP folder
     * @param optAcls The optional ACL list for given IMAP folder to optionally validate deputy entries in METADATA against actually
     *                existent ACL entries; leave to <code>null</code> to bypass that validation
     * @return The entity identifiers
     * @throws MessagingException If entity identifiers cannot be returned
     */
    public static TIntSet getDeputyEntities(IMAPFolder imapFolder, Optional<ACL[]> optionalAcls) throws MessagingException {
        Optional<JSONObject> optMetadata = getDeputyMetadata(imapFolder);
        if (optMetadata.isPresent() == false) {
            // No such METADATA at all
            return new TIntHashSet(0);
        }

        JSONObject jMetadata = optMetadata.get();
        int numberOfDeputyEntries = jMetadata.length();
        if (numberOfDeputyEntries <= 0) {
            // METADATA is empty
            try {
                unsetDeputyMetadata(imapFolder);
            } catch (Exception e) {
                LoggerHolder.LOG.warn("Failed to unset deputy metadata for IMAP folder {}", imapFolder.getFullName(), e);
            }
            return new TIntHashSet(0);
        }

        Set<String> aclNames = optionalAcls != null && optionalAcls.isPresent() ? Arrays.stream(optionalAcls.get()).map((a) -> a.getName()).collect(Collectors.toSet()) : null;
        Set<String> deputyMetadatasToDrop = null;
        TIntSet entityIds = new TIntHashSet(numberOfDeputyEntries);
        for (Map.Entry<String, Object> deputyEntry : jMetadata.entrySet()) {
            JSONObject jDeputyMetadata = (JSONObject) deputyEntry.getValue();
            int entityId = jDeputyMetadata.optInt("entity", 0);
            if (entityId > 0) {
                boolean aclExists = true;
                String aclName = jDeputyMetadata.optString("name", null);
                if (aclName != null && aclNames != null) {
                    aclExists = aclNames.contains(aclName);
                }
                if (aclExists) {
                    entityIds.add(entityId);
                } else {
                    if (deputyMetadatasToDrop == null) {
                        deputyMetadatasToDrop = new HashSet<String>();
                    }
                    deputyMetadatasToDrop.add(deputyEntry.getKey());
                }
            }
        }

        if (deputyMetadatasToDrop != null) {
            try {
                for (String deputyId : deputyMetadatasToDrop) {
                    jMetadata.remove(deputyId);
                }
                setDeputyMetadata(jMetadata, imapFolder);
            } catch (Exception e) {
                LoggerHolder.LOG.warn("Failed to remove orphaned deputy metadata entries from IMAP folder {}", imapFolder.getFullName(), e);
            }
        }

        return entityIds;
    }

    private static class MetadataResponseInterceptor implements ResponseInterceptor {

        private final String deputyId;
        private Map<String, JSONObject> fullName2Metadata = null;
        private boolean metadataResponseDetected = false;

        /**
         * Initializes a new {@link MetadataResponseInterceptor}.
         *
         * @param deputyId The deputy identifier
         */
        MetadataResponseInterceptor(String deputyId) {
            super();
            this.deputyId = deputyId;
        }

        Map<String, JSONObject> getFullName2Metadata() {
            return fullName2Metadata == null ? Collections.emptyMap() : fullName2Metadata;
        }

        boolean wasMetadataResponseDetected() {
            return metadataResponseDetected;
        }

        @Override
        public boolean intercept(Response response) {
            if (!(response instanceof IMAPResponse)) {
                return false;
            }

            IMAPResponse ir = (IMAPResponse) response;
            if (!ir.keyEquals("METADATA")) {
                return false;
            }

            metadataResponseDetected = true;
            Metadata metadata = IMAPMetadataUtility.parseMatchingMetadata(ir, METADATA_KEY);
            Map<String, String> keyValuePairs = metadata.getKeyValuePairs();
            if (keyValuePairs != null) {
                String value = keyValuePairs.get(METADATA_KEY);
                if (value != null) {
                    try {
                        JSONObject jDeputyMetadata = JSONServices.parseObject(value);
                        if (jDeputyMetadata.hasAndNotNull(deputyId)) {
                            if (fullName2Metadata == null) {
                                fullName2Metadata = new LinkedHashMap<String, JSONObject>();
                            }
                            fullName2Metadata.put(metadata.getFullName(), jDeputyMetadata);
                        }
                    } catch (JSONException e) {
                        // Invalid JSON content
                    }
                }
            }
            return true;
        }
    }

    /**
     * Determines all IMAP folders having a deputy permission for specified deputy.
     *
     * @param deputyId The deputy identifier
     * @param imapStore The IMAP store
     * @return A mapping of IMAP folder full name to meta-data
     * @throws MessagingException If operation fails
     */
    @SuppressWarnings("unchecked")
    public static Map<String, JSONObject> getAllFoldersHavingDeputy(String deputyId, IMAPStore imapStore) throws MessagingException {
        return (Map<String, JSONObject>) ((IMAPFolder) imapStore.getFolder("INBOX")).doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                MetadataResponseInterceptor interceptor = new MetadataResponseInterceptor(deputyId);
                String command = new StringBuilder(32).append("GETMETADATA * (").append(METADATA_KEY).append(')').toString();
                Response[] r = IMAPCommandsCollection.performCommand(protocol, command, null, Optional.of(interceptor), false);
                protocol.notifyResponseHandlers(r);

                // Examine result (OK, NO, BYE, ...) with special handling for "NO [NOPERM] ..." response
                try {
                    protocol.handleResult(r[r.length - 1], command);
                    // Apparently "OK"
                    return interceptor.getFullName2Metadata();
                } catch (com.sun.mail.iap.CommandFailedException e) {
                    // CommandFailedException --> A "NO" response. Check its response code...
                    if (e.getKnownResponseCode() == com.sun.mail.iap.ResponseCode.NOPERM && interceptor.wasMetadataResponseDetected()) {
                        // Some folders not accessible
                        return interceptor.getFullName2Metadata();
                    }
                    throw e;
                }
            }
        });
    }

    /**
     * Creates the deputy meta-data in JSON format for given arguments.
     * <p>
     * <blockquote>
     * <pre>
     * {
     *   "a234aef435345": {
     *     "version": 1,
     *     "entity": 3,
     *     "name": "jane.doe",
     *     "permission": 272662785,
     *     "previousRights": "lrswdex"
     *   },
     *
     *   "7856aef435345": {
     *     "version": 1,
     *     "entity": 4,
     *     "name": "bob.santos",
     *     "permission": 4227329,
     *   }
     * }
     * </pre>
     * </blockquote>
     *
     * @param deputyId The deputy identifier
     * @param entityId The entity identifier
     * @param permissionBits The granted permission
     * @param aclName The ACL name
     * @param version The implementation version of the IMAP provider for deputy permission
     * @param optionalExistentAcl The optional existent ACL
     * @param jMetadata The optional existent meta-data in JSON format
     * @return The deputy meta-data in JSON format
     * @throws OXException If deputy meta-data in JSON format cannot be created
     */
    public static JSONObject createDeputyMetadata(String deputyId, int entityId, int permissionBits, String aclName, int version, Optional<ACL> optionalExistentAcl, Optional<JSONObject> optMetadata) throws OXException {
        try {
            JSONObject jDeputyMetadata = new JSONObject(6);
            jDeputyMetadata.put("version", version);
            jDeputyMetadata.put("entity", entityId);
            jDeputyMetadata.put("name", aclName);
            jDeputyMetadata.put("permission", permissionBits);
            if (optionalExistentAcl.isPresent()) {
                jDeputyMetadata.put("previousRights", optionalExistentAcl.get().getRights().toString());
            }

            JSONObject jMetadata = optMetadata.isPresent() ? optMetadata.get() : new JSONObject(2);
            jMetadata.put(deputyId, jDeputyMetadata);
            return jMetadata;
        } catch (JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

}
