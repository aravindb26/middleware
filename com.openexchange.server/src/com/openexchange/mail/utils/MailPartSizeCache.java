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

package com.openexchange.mail.utils;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.exception.OXException;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mailaccount.Account;
import com.openexchange.session.Session;

/**
 * {@link MailPartSizeCache} - Volatile cache for the exact size of mail parts.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class MailPartSizeCache {

    private static final MailPartSizeCache INSTANCE = new MailPartSizeCache();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static MailPartSizeCache getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Cache<Key, Long> sizes;

    /**
     * Initializes a new {@link MailPartSizeCache}.
     */
    private MailPartSizeCache() {
        super();
        sizes = CacheBuilder.newBuilder().expireAfterAccess(6, TimeUnit.MINUTES).build();
    }

    /**
     * Gets the size (if available) for denoted mail part.
     *
     * @param session The session providing user information
     * @param folderPath The path of the folder in which the message resides; e.g. <code>"default2/INBOX"</code>
     * @param mailId The identifier of the message
     * @param partId The identifier of the message part; either sequence identifier or image content identifier
     * @param image Whether part is an image or a (file) attachment
     * @return The size or <code>-1</code>
     */
    public long optSizeFor(Session session, String folderPath, String mailId, String partId, boolean image) {
        Optional<FullnameArgument> optFa = MailFolderUtility.optPrepareMailFolderParam(folderPath);
        if (optFa.isPresent()) {
            FullnameArgument fa = optFa.get();
            return optSizeFor(session.getContextId(), session.getUserId(), fa.getAccountId(), fa.getFullName(), mailId, partId, image);
        }

        return optSizeFor(session.getContextId(), session.getUserId(), Account.DEFAULT_ID, folderPath, mailId, partId, image);
    }

    /**
     * Gets the size (if available) for denoted mail part.
     *
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param accountId The identifier of the mail account
     * @param folderId The identifier of the folder in which the message resides
     * @param mailId The identifier of the message
     * @param partId The identifier of the message part; either sequence identifier or image content identifier
     * @param image Whether part is an image or a (file) attachment
     * @return The size or <code>-1</code>
     */
    public long optSizeFor(int contextId, int userId, int accountId, String folderId, String mailId, String partId, boolean image) {
        Long size = sizes.getIfPresent(new Key(contextId, userId, accountId, folderId, mailId, partId, image));
        return size == null ? -1 : size.longValue();
    }

    //                                 ----------------------------------------------------------

    /**
     * Sets the size for denoted mail part.
     *
     * @param session The session providing user information
     * @param folderPath The path of the folder in which the message resides; e.g. <code>"default2/INBOX"</code>
     * @param mailId The identifier of the message
     * @param partId The identifier of the message part; either sequence identifier or image content identifier
     * @param image Whether part is an image or a (file) attachment
     * @param size The size to set
     */
    public void setSizeFor(Session session, String folderPath, String mailId, String partId, boolean image, long size) {
        Optional<FullnameArgument> optFa = MailFolderUtility.optPrepareMailFolderParam(folderPath);
        if (optFa.isPresent()) {
            FullnameArgument fa = optFa.get();
            setSizeFor(session.getContextId(), session.getUserId(), fa.getAccountId(), fa.getFullName(), mailId, partId, image, size);
        } else {
            setSizeFor(session.getContextId(), session.getUserId(), Account.DEFAULT_ID, folderPath, mailId, partId, image, size);
        }
    }

    /**
     * Sets the size for denoted mail part.
     *
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param accountId The identifier of the mail account
     * @param folderId The identifier of the folder in which the message resides
     * @param mailId The identifier of the message
     * @param partId The identifier of the message part; either sequence identifier or image content identifier
     * @param image Whether part is an image or a (file) attachment
     * @param size The size to set
     */
    public void setSizeFor(int contextId, int userId, int accountId, String folderId, String mailId, String partId, boolean image, long size) {
        sizes.put(new Key(contextId, userId, accountId, folderId, mailId, partId, image), L(size));
    }

    /**
     * Sets the size for denoted mail part.
     *
     * @param session The session providing user information
     * @param folderPath The path of the folder in which the message resides; e.g. <code>"default2/INBOX"</code>
     * @param mailId The identifier of the message
     * @param partId The identifier of the message part; either sequence identifier or image content identifier
     * @param image Whether part is an image or a (file) attachment
     * @param loader The loader providing the size to set
     * @return The loaded size for the mail part
     * @throws OXException If loading the size fails
     */
    public long setSizeFor(Session session, String folderPath, String mailId, String partId, boolean image, Callable<Long> loader) throws OXException {
        Optional<FullnameArgument> optFa = MailFolderUtility.optPrepareMailFolderParam(folderPath);
        if (optFa.isPresent()) {
            FullnameArgument fa = optFa.get();
            return setSizeFor(session.getContextId(), session.getUserId(), fa.getAccountId(), fa.getFullName(), mailId, partId, image, loader);
        }
        return setSizeFor(session.getContextId(), session.getUserId(), Account.DEFAULT_ID, folderPath, mailId, partId, image, loader);
    }

    /**
     * Sets the size for denoted mail part.
     *
     * @param contextId The context identifier
     * @param userId The user identifier
     * @param accountId The identifier of the mail account
     * @param folderId The identifier of the folder in which the message resides
     * @param mailId The identifier of the message
     * @param partId The identifier of the message part; either sequence identifier or image content identifier
     * @param image Whether part is an image or a (file) attachment
     * @param loader The loader providing the size to set
     * @return The loaded size for the mail part
     * @throws OXException If loading the size fails
     */
    public long setSizeFor(int contextId, int userId, int accountId, String folderId, String mailId, String partId, boolean image, Callable<Long> loader) throws OXException {
        try {
            return sizes.get(new Key(contextId, userId, accountId, folderId, mailId, partId, image), loader).longValue();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            Throwable t = cause == null ? e : cause;
            throw MailExceptionCode.UNEXPECTED_ERROR.create(t, t.getMessage());
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static class Key {

        private int hash = 0;
        private final int contextId;
        private final int userId;
        private final int accountId;
        private final String folderId;
        private final String mailId;
        private final String partId;
        private final boolean image;

        /**
         * Initializes a new {@link Key}.
         *
         * @param contextId The context identifier
         * @param userId The user identifier
         * @param accountId The identifier of the mail account
         * @param folderId The identifier of the folder in which the message resides
         * @param mailId The identifier of the message
         * @param partId The identifier of the message part; either sequence identifier or image content identifier
         * @param image Whether part is an image or a (file) attachment
         */
        Key(int contextId, int userId, int accountId, String folderId, String mailId, String partId, boolean image) {
            super();
            this.contextId = contextId;
            this.userId = userId;
            this.accountId = accountId;
            this.folderId = folderId;
            this.mailId = mailId;
            this.partId = partId;
            this.image = image;
        }

        @Override
        public int hashCode() {
            // Ignore thread-safety
            int h = hash;
            if (h == 0) {
                h = Objects.hash(I(contextId), I(userId), I(accountId), Boolean.valueOf(image), folderId, mailId, partId);
                hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            return contextId == other.contextId && userId == other.userId && accountId == other.accountId && image == other.image && Objects.equals(folderId, other.folderId) && Objects.equals(mailId, other.mailId) && Objects.equals(partId, other.partId);
        }

    }

}
