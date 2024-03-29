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

package com.openexchange.imap;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.List;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.imap.services.Services;
import com.openexchange.mail.utils.StorageUtility;
import com.openexchange.session.Session;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import gnu.trove.map.TIntObjectMap;

/**
 * {@link SpecialUseDefaultFolderChecker} - The IMAP default folder checker with respect to .
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SpecialUseDefaultFolderChecker extends IMAPDefaultFolderChecker {

    /** The logger constant */
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SpecialUseDefaultFolderChecker.class);
    private static final String SET_SPECIAL_USE_PROPERTY = "com.openexchange.imap.setSpecialUseFlags";

    // -------------------------------------------------------------------------------------------------------------------------------- //

    private final boolean hasCreateSpecialUse;

    /**
     * Initializes a new {@link SpecialUseDefaultFolderChecker}.
     *
     * @param accountId The account ID
     * @param session The session
     * @param ctx The context
     * @param imapStore The (connected) IMAP store
     * @param imapAccess The IMAP access
     * @param hasCreateSpecialUse Whether the IMAP server advertises "CREATE-SPECIAL-USE" capability string
     * @param hasMetadata Whether the IMAP server advertises "METADATA" capability string
     */
    public SpecialUseDefaultFolderChecker(int accountId, Session session, Context ctx, IMAPStore imapStore, IMAPAccess imapAccess, boolean hasCreateSpecialUse, boolean hasMetadata) {
        super(accountId, session, ctx, imapStore, imapAccess, hasMetadata);
        this.hasCreateSpecialUse = hasCreateSpecialUse;
    }

    private boolean isAllowedToSetSpecialUse() {
        boolean setSpecialUseFlags = false;
        ConfigViewFactory configViewFactory = Services.getService(ConfigViewFactory.class);
        if (null != configViewFactory) {
            try {
                ConfigView view = configViewFactory.getView(session.getUserId(), session.getContextId());
                ComposedConfigProperty<Boolean> property = view.property(SET_SPECIAL_USE_PROPERTY, Boolean.class);
                Boolean b = property.get();
                if (null != b) {
                    setSpecialUseFlags = b.booleanValue();
                }
            } catch (OXException e) {
                LOG.error("", e);
                // continue with default value
            }
        }
        return setSpecialUseFlags;
    }

    @Override
    protected boolean setSpecialUseForExisting() {
        return hasCreateSpecialUse && isAllowedToSetSpecialUse();
    }

    @Override
    protected void createIfNonExisting(IMAPFolder f, int type, char sep, String namespace, int index, TIntObjectMap<String> specialUseInfo) throws MessagingException {
        if (!f.exists()) {
            try {
                if (hasCreateSpecialUse && isAllowedToSetSpecialUse() && specialUseInfo.get(index) == null) {
                    // E.g. CREATE MyDrafts (USE (\Drafts))
                    List<String> specialUses = index <= StorageUtility.INDEX_TRASH ? Collections.singletonList(SPECIAL_USES[index]) : null;
                    createFolder(f, sep, type, specialUses);
                } else {
                    createFolder(f, sep, type, null);
                    if (hasMetadata && isAllowedToSetSpecialUse() && index <= StorageUtility.INDEX_TRASH && specialUseInfo.get(index) == null) {
                        // E.g. SETMETADATA "SavedDrafts" (/private/specialuse "\\Drafts")
                        String flag = SPECIAL_USES[index];
                        try {
                            IMAPCommandsCollection.setSpecialUses(f, Collections.singletonList(flag));
                        } catch (Exception e) {
                            LOG.debug("Failed to set {} flag for new standard {} folder (full-name=\"{}\", namespace=\"{}\") for login {} (account={}) on IMAP server {} (user={}, context={})", flag, getFallbackName(index), f.getFullName(), namespace, imapConfig.getLogin(), I(accountId), imapConfig.getServer(), I(session.getUserId()), I(session.getContextId()), e);
                        }
                    }
                }
                LOG.info("Created new standard {} folder (full-name={}, namespace={}) for login {} (account={}) on IMAP server {} (user={}, context={})", getFallbackName(index), f.getFullName(), namespace, imapConfig.getLogin(), I(accountId), imapConfig.getServer(), I(session.getUserId()), I(session.getContextId())); // NOSONARLINT
            } catch (MessagingException e) { // NOSONARLINT
                LOG.warn("Failed to create new standard {} folder (full-name={}, namespace={}) for login {} (account={}) on IMAP server {} (user={}, context={})", getFallbackName(index), f.getFullName(), namespace, imapConfig.getLogin(), I(accountId), imapConfig.getServer(), I(session.getUserId()), I(session.getContextId()), e);
                throw e;
            }
        } else {
            if (hasMetadata && isAllowedToSetSpecialUse() && index <= StorageUtility.INDEX_TRASH && specialUseInfo.get(index) == null) {
                // E.g. SETMETADATA "SavedDrafts" (/private/specialuse "\\Drafts")
                String flag = SPECIAL_USES[index];
                try {
                    IMAPCommandsCollection.setSpecialUses(f, Collections.singletonList(flag));
                } catch (Exception e) {
                    LOG.debug("Failed to set {} flag for existing standard {} folder (full-name=\"{}\", namespace=\"{}\") for login {} (account={}) on IMAP server {} (user={}, context={})", flag, getFallbackName(index), f.getFullName(), namespace, imapConfig.getLogin(), I(accountId), imapConfig.getServer(), I(session.getUserId()), I(session.getContextId()), e);
                }
            }
        }
    }

}
