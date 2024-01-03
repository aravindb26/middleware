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

package com.openexchange.carddav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.io.BaseEncoding;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.contact.provider.composition.IDMangling;
import com.openexchange.dav.DAVOAuthScope;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.webdav.protocol.WebdavPath;

/**
 * {@link Tools}
 *
 * Provides some utility functions.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Tools {

    private static final Logger LOG = LoggerFactory.getLogger(Tools.class);

    /**
     * The OAuth scope token for CardDAV
     */
    public static final String OAUTH_SCOPE = DAVOAuthScope.CARDDAV.getScope();

    /** The static prefix for the default contacts account */
    public static final String DEFAULT_ACCOUNT_PREFIX = "con://0/";

    public static final String AGGREGATED_COLLECTION_PREFIX = "Contacts.";

    /**
     * Extracts the UID part from the supplied {@link WebdavPath}, i.e. the
     * path's name without the <code>.vcf</code> extension.
     *
     * @param path the path
     * @return the name
     */
    public static String extractUID(WebdavPath path) {
        if (null == path) {
            throw new IllegalArgumentException("path");
        }
        return extractUID(path.name());
    }

    /**
     * Extracts the UID part from the supplied resource name, i.e. the
     * resource name without the <code>.vcf</code> extension.
     *
     * @param name the name
     * @return the UID
     */
    public static String extractUID(String name) {
        if (null != name && 4 < name.length() && name.toLowerCase().endsWith(".vcf")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    /**
     * Calculates a combined hash code for the supplied collection of folders, based on each folder's identifier as well as the user's
     * <i>own</i> permissions on it.
     *
     * @param folders The folders to get the hash code for
     * @return The hash code
     */
    public static String getFoldersHash(List<UserizedFolder> folders) {
        if (null == folders || folders.isEmpty()) {
            return null;
        }
        final int prime = 31;
        int result = 1;
        for (UserizedFolder folder : folders) {
            result = prime * result + ((null == folder.getID()) ? 0 : folder.getID().hashCode());
            result = prime * result + ((null == folder.getOwnPermission()) ? 0 : folder.getOwnPermission().hashCode());
        }

        if(LOG.isDebugEnabled()) {
            StringBuilder b = new StringBuilder("Generated folder hash '");
            b.append(Integer.toHexString(result)).append("' from: ");
            // @formatter:off
            folders.forEach((f) -> {
                b.append("{ name: '")
                 .append(f.getName())
                 .append("', id: ")
                 .append(f.getID())
                 .append(", perms: ")
                 .append(f.getOwnPermission().hashCode())
                 .append(" }, ");
            });
            // @formatter:on
            LOG.debug(b.toString());
        }
        return Integer.toHexString(result);
    }

    public static Date getLatestModified(final Date lastModified1, final Date lastModified2) {
        if (null != lastModified2) {
            return lastModified1.after(lastModified2) ? lastModified1 : lastModified2;
        }
        return lastModified1;
    }

    public static Date getLatestModified(final Date lastModified, final Contact contact) {
        return getLatestModified(lastModified, contact.getLastModified());
    }

    public static Date getLatestModified(Date lastModified, UserizedFolder folder) {
        return getLatestModified(lastModified, folder.getLastModifiedUTC());
    }

    public static boolean isImageProblem(final OXException e) {
        return ContactExceptionCodes.IMAGE_BROKEN.equals(e) ||
            ContactExceptionCodes.IMAGE_DOWNSCALE_FAILED.equals(e) ||
            ContactExceptionCodes.IMAGE_SCALE_PROBLEM.equals(e) ||
            ContactExceptionCodes.IMAGE_TOO_LARGE.equals(e) ||
            ContactExceptionCodes.NOT_VALID_IMAGE.equals(e);
    }

    public static boolean isDataTruncation(final OXException e) {
        return ContactExceptionCodes.DATA_TRUNCATION.equals(e);
    }

    public static boolean isIncorrectString(OXException e) {
        return ContactExceptionCodes.INCORRECT_STRING.equals(e);
    }

    /**
     * Parses a numerical identifier from a string, wrapping a possible
     * NumberFormatException into an OXException.
     *
     * @param id the id string
     * @return the parsed identifier
     * @throws OXException
     */
    public static int parse(final String id) throws OXException {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw ContactExceptionCodes.ID_PARSING_FAILED.create(e, id);
        }
    }

    public static SearchTerm<?> getSearchTerm(String uid, List<String> folderIDs) {
        CompositeSearchTerm uidsTerm = new CompositeSearchTerm(CompositeOperation.OR);
        uidsTerm.addSearchTerm(getSingleSearchTerm(ContactField.UID, uid));
        uidsTerm.addSearchTerm(getSingleSearchTerm(ContactField.USERFIELD19, uid));
        if (null == folderIDs || 0 == folderIDs.size()) {
            return uidsTerm;
        }
        CompositeSearchTerm andTerm = new CompositeSearchTerm(CompositeOperation.AND);
        andTerm.addSearchTerm(uidsTerm);
        if (1 == folderIDs.size()) {
            andTerm.addSearchTerm(getSingleSearchTerm(ContactField.FOLDER_ID, folderIDs.get(0)));
        } else {
            CompositeSearchTerm foldersTerm = new CompositeSearchTerm(CompositeOperation.OR);
            for (String folderID : folderIDs) {
                foldersTerm.addSearchTerm(getSingleSearchTerm(ContactField.FOLDER_ID, folderID));
            }
            andTerm.addSearchTerm(foldersTerm);
        }
        return andTerm;
    }

    private static SingleSearchTerm getSingleSearchTerm(ContactField field, String value) {
        SingleSearchTerm term = new SingleSearchTerm(SingleOperation.EQUALS);
        term.addOperand(new ContactFieldOperand(field));
        term.addOperand(new ConstantOperand<String>(value));
        return term;
    }

    public static String getAggregatedCollectionId(List<UserizedFolder> aggregatedFolders) {
        return AGGREGATED_COLLECTION_PREFIX + getFoldersHash(aggregatedFolders);
    }

    public static String encodeFolderId(String folderId) {
        if (null == folderId) {
            return null;
        }
        if (folderId.startsWith(DEFAULT_ACCOUNT_PREFIX)) {
            /*
             * folders from the default internal groupware account are used with their relative ids as-is (backwards compatibility)
             */
            return folderId.substring(DEFAULT_ACCOUNT_PREFIX.length());
        }
        /*
         * by default, the collection name is the base64 encoded folder id
         */
        return BaseEncoding.base64Url().omitPadding().encode(folderId.getBytes(Charsets.US_ASCII));
    }

    public static String decodeCollectionName(String collectionName) {
        if (null == collectionName) {
            return collectionName;
        }
        if (collectionName.startsWith(AGGREGATED_COLLECTION_PREFIX)) {
            /*
             * return name of aggregated collection as-is
             */
            return collectionName;
        }
        /*
         * try to decode base64 collection names & check if decoded string is a valid composite folder identifier
         */
        try {
            String folderId = new String(BaseEncoding.base64Url().omitPadding().decode(collectionName), Charsets.US_ASCII);
            if (null != IDMangling.getRelativeFolderId(folderId)) {
                return folderId;
            }
            throw new IllegalArgumentException("Cannot extract relative folder id from " + folderId);
        } catch (OXException | IllegalArgumentException e) {
            if (-1 != Strings.parsePositiveInt(collectionName)) {
                /*
                 * folders from the default internal groupware account are used with their relative ids as-is (backwards compatibility)
                 */
                return DEFAULT_ACCOUNT_PREFIX + collectionName;
            }
            /*
             * fall back to collection name as-is, otherwise
             */
            LOG.debug("Collection name {} cannot be decoded, assuming newly created collection.", collectionName, e);
            return collectionName;
        }
    }

    /**
     * Filters a list of folders by a certain contacts access capability.
     * 
     * @param folders The folders to filter
     * @param capability The capability to check
     * @return A new list of folders, containing only these that do have support for the capability
     */
    public static List<UserizedFolder> filterByCapability(List<UserizedFolder> folders, ContactsAccessCapability capability) {
        if (null == folders || folders.isEmpty() || null == capability) {
            return folders;
        }
        List<UserizedFolder> filteredFolders = new ArrayList<>(folders.size());
        for (UserizedFolder folder : folders) {
            if (getSupportedCapabilities(folder).contains(capability)) {
                filteredFolders.add(folder);
            }
        }
        return filteredFolders;
    }

    /**
     * Filters a list of folders that are marked as <i>used for sync</i>.
     * 
     * @param folders The folders to filter
     * @return A new list of folders, containing only these that are used for sync
     */
    public static List<UserizedFolder> filterNotUsedForSync(List<UserizedFolder> folders) {
        if (null == folders || folders.isEmpty()) {
            return folders;
        }
        List<UserizedFolder> filteredFolders = new ArrayList<>(folders.size());
        for (UserizedFolder folder : folders) {
            if (null == folder.getUsedForSync() || folder.getUsedForSync().isUsedForSync())
                filteredFolders.add(folder);
        }
        return filteredFolders;
    }

    /**
     * Gets a set of all supported contacts access capabilities of a certain contact folder.
     * 
     * @param folder The folder to get the supported contacts access capabilities for
     * @return The supported capabilities
     */
    public static EnumSet<ContactsAccessCapability> getSupportedCapabilities(UserizedFolder folder) {
        return getSupportedCapabilities(Collections.singletonList(folder));
    }

    /**
     * Gets a set of all supported contacts access capabilities of certain contact folder(s), which is the least common denominator of
     * the supported capabilities for folders in the supplied collection.
     * 
     * @param folders The folders to get the supported contacts access capabilities for
     * @return The supported capabilities
     */
    public static EnumSet<ContactsAccessCapability> getSupportedCapabilities(List<UserizedFolder> folders) {
        if (null == folders || folders.isEmpty()) {
            return EnumSet.noneOf(ContactsAccessCapability.class);
        }
        if (1 == folders.size()) {
            /*
             * derive capabilities from single folder
             */
            Set<String> supportedCapabilities = folders.get(0).getSupportedCapabilities();
            if (null == supportedCapabilities) {
                return EnumSet.allOf(ContactsAccessCapability.class); // assume all supported
            }
            return ContactsAccessCapability.getCapabilities(supportedCapabilities);
        }
        /*
         * build least common denominator of supported capabilities for all folders
         */
        EnumSet<ContactsAccessCapability> capabilities = EnumSet.allOf(ContactsAccessCapability.class);
        for (UserizedFolder folder : folders) {
            Set<String> supportedCapabilities = folder.getSupportedCapabilities();
            if (null == supportedCapabilities) {
                continue; // assume all supported
            }
            capabilities.retainAll(ContactsAccessCapability.getCapabilities(supportedCapabilities));
        }
        return capabilities;
    }

}

