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

package com.openexchange.groupware.settings.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.settings.IValueHandler;
import com.openexchange.groupware.settings.PreferencesItemService;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.SettingExceptionCodes;
import com.openexchange.groupware.settings.SharedNode;

/**
 * This class is a container for the settings tree.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class ConfigTree {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConfigTree.class);

    private static final ConfigTree SINGLETON = new ConfigTree();

    /**
     * Gets the singleton instance.
     *
     * @return The instance
     */
    public static ConfigTree getInstance() {
        return SINGLETON;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Reference to the settings tree.
     */
    private final AtomicReference<TreeSetting> treeReference = new AtomicReference<TreeSetting>(null);

    /**
     * Set of all identifiers for the database to check for duplicate ones.
     */
    private final Set<Integer> dbIdentifier = new HashSet<Integer>();

    /**
     * Prevent instantiation
     */
    private ConfigTree() {
        super();
    }

    private static final Pattern SPLIT = Pattern.compile("/");

    /**
     * Resolves a path to the according setting object.
     * @param path Path to resolve.
     * @return A setting object or <code>null</code> if non-existent
     */
    public Setting optSettingByPath(final String path) {
        final String[] pathParts = SPLIT.split(path, 0);
        Setting setting = optSettingByPath(treeReference.get(), pathParts);
        return null == setting ? null : new ValueSetting(setting);
    }

    /**
     * This is the recursive method for resolving the path.
     * @param actual setting object that is already resolved from the path.
     * @param path the path that must be resolved.
     * @return A setting object or <code>null</code> if non-existent
     */
    public static Setting optSettingByPath(final Setting actual, final String[] path) {
        Setting retval = actual;
        if (path.length != 0) {
            String[] remainingPath = new String[path.length - 1];
            System.arraycopy(path, 1, remainingPath, 0, path.length - 1);
            Setting child = isSegmentEmpty(path[0]) ? actual : actual.getElement(path[0]);
            if (null == child) {
                return null;
            }
            retval = optSettingByPath(child, remainingPath);
        }
        return retval;
    }

    /**
     * Resolves a path to the according setting object.
     * @param path Path to resolve.
     * @return a setting object.
     * @throws OXException if the path cannot be resolved to a setting
     * object.
     */
    public Setting getSettingByPath(final String path) throws OXException {
        final String[] pathParts = SPLIT.split(path, 0);
        return new ValueSetting(getSettingByPath(treeReference.get(), pathParts));
    }

    /**
     * This is the recursive method for resolving the path.
     * @param actual setting object that is already resolved from the path.
     * @param path the path that must be resolved.
     * @return a setting object.
     * @throws OXException if the path cannot be resolved to a setting
     * object.
     */
    public static Setting getSettingByPath(final Setting actual, final String[] path) throws OXException {
        Setting retval = actual;
        if (path.length != 0) {
            String[] remainingPath = new String[path.length - 1];
            System.arraycopy(path, 1, remainingPath, 0, path.length - 1);
            Setting child = isSegmentEmpty(path[0]) ? actual : actual.getElement(path[0]);
            if (null == child) {
                StringBuilder sb = new StringBuilder(path[0]);
                Setting parent = actual;
                while (null != parent) {
                    sb.insert(0, '/');
                    sb.insert(0, parent.getName());
                    parent = parent.getParent();
                }
                throw SettingExceptionCodes.UNKNOWN_PATH.create(sb.toString());
            }
            retval = getSettingByPath(child, remainingPath);
        }
        return retval;
    }

    private static <T extends AbstractSetting<? extends T>> T getSettingByPath(final T actual, final String[] path) throws OXException {
        T retval = actual;
        if (path.length != 0) {
            String[] remainingPath = new String[path.length - 1];
            System.arraycopy(path, 1, remainingPath, 0, path.length - 1);
            T child = isSegmentEmpty(path[0]) ? actual : (null == actual ? null : actual.getElement(path[0]));
            if (null == child) {
                StringBuilder sb = new StringBuilder(path[0]);
                Setting parent = actual;
                while (null != parent) {
                    sb.insert(0, '/');
                    sb.insert(0, parent.getName());
                    parent = parent.getParent();
                }
                throw SettingExceptionCodes.UNKNOWN_PATH.create(sb.toString());
            }
            retval = getSettingByPath(child, remainingPath);
        }
        return retval;
    }

    /** TODO: To be removed with v7.8.3 development */
    private static final boolean SUPPORT_EMPTY_PATH_SEGMENTS = true;

    private static boolean isSegmentEmpty(String pathSegment) {
        return SUPPORT_EMPTY_PATH_SEGMENTS && 0 == pathSegment.length();
    }

    /**
     * Gets all available leaf settings.
     *
     * @return All available leaf settings
     */
    public Collection<Setting> getSettings() {
        final List<Setting> settings = new LinkedList<Setting>();
        gatherSettings(treeReference.get(), settings);
        return settings;
    }

    private static <T extends AbstractSetting<? extends T>> void gatherSettings(final T current, final List<Setting> settings) {
        if (current.isLeaf()) {
            settings.add(current);
        } else {
            for (final T child : current.getElements()) {
                gatherSettings(child, settings);
            }
        }
    }

    /**
     * Adds specified <code>PreferencesItemService</code> instance to this configuration tree.
     *
     * @param item The item to add
     * @throws OXException If add operation fails
     */
    public void addPreferencesItem(final PreferencesItemService item) throws OXException {
        addSharedValue(treeReference.get(), item.getPath(), item.getSharedValue());
    }

    private void addSharedValue(final TreeSetting current, final String[] path, final IValueHandler shared) throws OXException {
        if (1 == path.length) {
            if (-1 != shared.getId()) {
                final Integer tmp = Integer.valueOf(shared.getId());
                if (dbIdentifier.contains(tmp)) {
                    throw SettingExceptionCodes.DUPLICATE_ID.create(tmp);
                }
                dbIdentifier.add(tmp);
            }
            addElementWithoutOverwriting(current, new TreeSetting(path[0], shared.getId(), shared));
        } else {
            TreeSetting sub = isSegmentEmpty(path[0]) ? current : current.getElement(path[0]);
            if (null == sub) {
                IValueHandler node = new SharedNode(path[0]);
                TreeSetting toAdd = new TreeSetting(path[0], node.getId(), node);
                addElementWithoutOverwriting(current, toAdd);
                sub = toAdd;
            }
            final String[] subPath = new String[path.length - 1];
            System.arraycopy(path, 1, subPath, 0, subPath.length);
            addSharedValue(sub, subPath, shared);
        }
    }

    private static void addElementWithoutOverwriting(final TreeSetting current, final TreeSetting subSetting) throws OXException {
        if (false == current.checkElement(subSetting)) {
            throw SettingExceptionCodes.DUPLICATE_PATH.create(current.getPath() + "/" + subSetting.getName());
        }

        current.addElement(subSetting);
        subSetting.setParent(current);
    }

    public void removePreferencesItem(final PreferencesItemService item) {
        try {
            removeSharedValue(getSettingByPath(treeReference.get(), item.getPath()));
        } catch (OXException e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("", e);
            } else {
                final String message = e.getMessage();
                if (com.openexchange.java.Strings.toLowerCase(message).indexOf("/io.ox/") < 0) {
                    LOG.warn(message);
                }
            }
        }
    }

    private void removeSharedValue(final TreeSetting setting) throws OXException {
        final TreeSetting parent = setting.getParent();
        parent.removeElement(setting);
        if (-1 != setting.getId()) {
            dbIdentifier.remove(Integer.valueOf(setting.getId()));
        }
        if (parent.isLeaf() && parent != treeReference.get()) {
            removeSharedValue(parent);
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private static Class< ? extends PreferencesItemService>[] getClasses() {
        return new Class[] {
            com.openexchange.groupware.settings.tree.AvailableModules.class,
            com.openexchange.groupware.settings.tree.AvailableTimeZones.class,
            com.openexchange.groupware.settings.tree.BetaFeatures.class,
            com.openexchange.groupware.settings.tree.CalendarNotification.class,
            com.openexchange.groupware.settings.tree.ContactID.class,
            com.openexchange.groupware.settings.tree.ContextID.class,
            com.openexchange.groupware.settings.tree.Login.class,
            com.openexchange.groupware.settings.tree.CookieLifetime.class,
            com.openexchange.groupware.settings.tree.CurrentTime.class,
            com.openexchange.groupware.settings.tree.FastGUI.class,
            com.openexchange.groupware.settings.tree.folder.Calendar.class,
            com.openexchange.groupware.settings.tree.folder.Addressbooks.class,
            com.openexchange.groupware.settings.tree.folder.Contacts.class,
            com.openexchange.groupware.settings.tree.folder.Infostore.class,
            com.openexchange.groupware.settings.tree.folder.Tasks.class,
            com.openexchange.groupware.settings.tree.folder.EASFolder.class,
            com.openexchange.groupware.settings.tree.GUI.class,
            com.openexchange.groupware.settings.tree.Identifier.class,
            com.openexchange.groupware.settings.tree.JsonMaxSize.class,
            com.openexchange.groupware.settings.tree.Language.class,
            com.openexchange.groupware.settings.tree.mail.AppendMailTextSP3.class,
            com.openexchange.groupware.settings.tree.mail.AddressesSP3.class,
            com.openexchange.groupware.settings.tree.mail.ColorquotedSP3.class,
            com.openexchange.groupware.settings.tree.mail.DefaultAddressSP3.class,
            com.openexchange.groupware.settings.tree.mail.DeleteMailSP3.class,
            com.openexchange.groupware.settings.tree.mail.EmoticonsSP3.class,
            com.openexchange.groupware.settings.tree.mail.folder.DraftsSP3.class,
            com.openexchange.groupware.settings.tree.mail.folder.InboxSP3.class,
            com.openexchange.groupware.settings.tree.mail.folder.SentSP3.class,
            com.openexchange.groupware.settings.tree.mail.folder.SpamSP3.class,
            com.openexchange.groupware.settings.tree.mail.folder.TrashSP3.class,
            com.openexchange.groupware.settings.tree.mail.ForwardMessageSP3.class,
            com.openexchange.groupware.settings.tree.mail.InlineAttachmentsSP3.class,
            com.openexchange.groupware.settings.tree.mail.LineWrapSP3.class,
            com.openexchange.groupware.settings.tree.mail.SendAddressSP3.class,
            com.openexchange.groupware.settings.tree.mail.SpamButtonSP3.class,
            com.openexchange.groupware.settings.tree.MaxUploadIdleTimeout.class,
            com.openexchange.groupware.settings.tree.MinimumSearchCharacters.class,
            com.openexchange.groupware.settings.tree.modules.calendar.GUI.class,
            com.openexchange.groupware.settings.tree.modules.calendar.Module.class,
            com.openexchange.groupware.settings.tree.modules.calendar.CalendarConflict.class,
            com.openexchange.groupware.settings.tree.modules.calendar.CalendarFreeBusy.class,
            com.openexchange.groupware.settings.tree.modules.calendar.CalendarTeamView.class,
            com.openexchange.groupware.settings.tree.modules.calendar.NotifyAcceptedDeclinedAsCreator.class,
            com.openexchange.groupware.settings.tree.modules.calendar.NotifyAcceptedDeclinedAsParticipant.class,
            com.openexchange.groupware.settings.tree.modules.calendar.NotifyNewModifiedDeleted.class,
            com.openexchange.groupware.settings.tree.modules.calendar.DefaultStatusPrivate.class,
            com.openexchange.groupware.settings.tree.modules.calendar.DefaultStatusPublic.class,
            com.openexchange.groupware.settings.tree.modules.contacts.AllFoldersForAutoComplete.class,
            com.openexchange.groupware.settings.tree.modules.contacts.GUI.class,
            com.openexchange.groupware.settings.tree.modules.contacts.MailAddressAutoSearch.class,
            com.openexchange.groupware.settings.tree.modules.contacts.Module.class,
            com.openexchange.groupware.settings.tree.modules.contacts.SingleFolderSearch.class,
            com.openexchange.groupware.settings.tree.modules.contacts.CharacterSearch.class,
            com.openexchange.groupware.settings.tree.modules.contacts.MaxImageSize.class,
            com.openexchange.groupware.settings.tree.modules.extras.Module.class,
            com.openexchange.groupware.settings.tree.modules.folder.GUI.class,
            com.openexchange.groupware.settings.tree.modules.folder.PublicFolders.class,
            com.openexchange.groupware.settings.tree.modules.folder.SharedFolders.class,
            com.openexchange.groupware.settings.tree.modules.infostore.GUI.class,
            com.openexchange.groupware.settings.tree.modules.infostore.Module.class,
            com.openexchange.groupware.settings.tree.modules.infostore.MaxUploadSize.class,
            com.openexchange.groupware.settings.tree.modules.infostore.folder.Trash.class,
            com.openexchange.groupware.settings.tree.modules.infostore.folder.Pictures.class,
            com.openexchange.groupware.settings.tree.modules.infostore.folder.Documents.class,
            com.openexchange.groupware.settings.tree.modules.infostore.folder.Templates.class,
            com.openexchange.groupware.settings.tree.modules.infostore.folder.Music.class,
            com.openexchange.groupware.settings.tree.modules.infostore.folder.Videos.class,
            com.openexchange.groupware.settings.tree.modules.infostore.autodelete.AutodeleteEditable.class,
            com.openexchange.groupware.settings.tree.modules.infostore.autodelete.MaxVersionCount.class,
            com.openexchange.groupware.settings.tree.modules.infostore.autodelete.RetentionDays.class,
            com.openexchange.groupware.settings.tree.modules.interfaces.ICal.class,
            com.openexchange.groupware.settings.tree.modules.interfaces.SyncML.class,
            com.openexchange.groupware.settings.tree.modules.interfaces.VCard.class,
            com.openexchange.groupware.settings.tree.modules.mail.Addresses.class,
            com.openexchange.groupware.settings.tree.modules.mail.AllowHTMLImages.class,
            com.openexchange.groupware.settings.tree.modules.mail.AppendMailText.class,
            com.openexchange.groupware.settings.tree.modules.mail.Colorquoted.class,
            com.openexchange.groupware.settings.tree.modules.mail.DefaultAddress.class,
            com.openexchange.groupware.settings.tree.modules.mail.DefaultSeparator.class,
            com.openexchange.groupware.settings.tree.modules.mail.DeleteDraftOnTransport.class,
            com.openexchange.groupware.settings.tree.modules.mail.DeleteMail.class,
            com.openexchange.groupware.settings.tree.modules.mail.MailFetchLimit.class,
            com.openexchange.groupware.settings.tree.modules.mail.SaveNoCopy.class,
            com.openexchange.groupware.settings.tree.modules.mail.DisplayReceiptNotification.class,
            com.openexchange.groupware.settings.tree.modules.mail.UnifiedInboxEnablement.class,
            com.openexchange.groupware.settings.tree.modules.mail.UnifiedInboxIdentifier.class,
            com.openexchange.groupware.settings.tree.modules.mail.Emoticons.class,
            com.openexchange.groupware.settings.tree.modules.mail.folder.Drafts.class,
            com.openexchange.groupware.settings.tree.modules.mail.folder.Inbox.class,
            com.openexchange.groupware.settings.tree.modules.mail.folder.Sent.class,
            com.openexchange.groupware.settings.tree.modules.mail.folder.Spam.class,
            com.openexchange.groupware.settings.tree.modules.mail.folder.Trash.class,
            com.openexchange.groupware.settings.tree.modules.mail.folder.Archive.class,
            com.openexchange.groupware.settings.tree.modules.mail.ForwardMessage.class,
            com.openexchange.groupware.settings.tree.modules.mail.GUI.class,
            com.openexchange.groupware.settings.tree.modules.mail.InlineAttachments.class,
            com.openexchange.groupware.settings.tree.modules.mail.LineWrap.class,
            com.openexchange.groupware.settings.tree.modules.mail.UploadQuotaPerFile.class,
            com.openexchange.groupware.settings.tree.modules.mail.UploadQuotaTotal.class,
            com.openexchange.groupware.settings.tree.modules.mail.MailColorModePreferenceItem.class,
            com.openexchange.groupware.settings.tree.modules.mail.MailFlaggedModePreferenceItem.class,
            com.openexchange.groupware.settings.tree.modules.mail.MaliciousCheck.class,
            com.openexchange.groupware.settings.tree.modules.mail.MaliciousListing.class,
            com.openexchange.groupware.settings.tree.modules.mail.AllMessagesFolder.class,
            com.openexchange.groupware.settings.tree.modules.mail.Whitelist.class,
            com.openexchange.groupware.settings.tree.modules.mail.ForwardUnquoted.class,
            com.openexchange.groupware.settings.tree.modules.mail.IgnoreSubscription.class,
            com.openexchange.groupware.settings.tree.modules.mail.Module.class,
            com.openexchange.groupware.settings.tree.modules.mail.MsgFormat.class,
            com.openexchange.groupware.settings.tree.modules.mail.Namespace.class,
            com.openexchange.groupware.settings.tree.modules.mail.DefaultArchiveDays.class,
            com.openexchange.groupware.settings.tree.modules.mail.PhishingHeaders.class,
            com.openexchange.groupware.settings.tree.modules.mail.MailProtocols.class,
            com.openexchange.groupware.settings.tree.modules.mail.ReplyAllCc.class,
            com.openexchange.groupware.settings.tree.modules.mail.SendAddress.class,
            com.openexchange.groupware.settings.tree.modules.mail.Separators.class,
            com.openexchange.groupware.settings.tree.modules.mail.SpamButton.class,
            com.openexchange.groupware.settings.tree.modules.mail.VCard.class,
            com.openexchange.groupware.settings.tree.modules.mail.MaxMailSize.class,
            com.openexchange.groupware.settings.tree.modules.mailaccount.Module.class,
            com.openexchange.groupware.settings.tree.modules.olox20.Active.class,
            com.openexchange.groupware.settings.tree.modules.olox20.Module.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.Module.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.MaxLength.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.MinLength.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.Regexp.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.ShowStrength.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.Special.class,
            com.openexchange.groupware.settings.tree.modules.passwordchange.EmptyCurrent.class,
            com.openexchange.groupware.settings.tree.modules.personaldata.Module.class,
            com.openexchange.groupware.settings.tree.modules.personaldata.InternalUserEditEnabled.class,
            com.openexchange.groupware.settings.tree.modules.portal.GUI.class,
            com.openexchange.groupware.settings.tree.modules.portal.Module.class,
            com.openexchange.groupware.settings.tree.modules.tasks.GUI.class,
            com.openexchange.groupware.settings.tree.modules.tasks.Module.class,
            com.openexchange.groupware.settings.tree.modules.tasks.DelegateTasks.class,
            com.openexchange.groupware.settings.tree.modules.tasks.NotifyAcceptedDeclinedAsCreator.class,
            com.openexchange.groupware.settings.tree.modules.tasks.NotifyAcceptedDeclinedAsParticipant.class,
            com.openexchange.groupware.settings.tree.modules.tasks.NotifyNewModifiedDeleted.class,
            com.openexchange.groupware.settings.tree.ReloadTimes.class,
            // TODO: Enable per-user spell check switch if needed
            // com.openexchange.groupware.settings.tree.SpellCheck.class,
            com.openexchange.groupware.settings.tree.ServerVersion.class,
            com.openexchange.groupware.settings.tree.UiWebPath.class,
            com.openexchange.groupware.settings.tree.TaskNotification.class,
            com.openexchange.groupware.settings.tree.TimeZone.class,
            com.openexchange.groupware.settings.tree.LocationLogout.class,
            com.openexchange.groupware.settings.tree.LocationError.class,
            com.openexchange.groupware.settings.tree.ValidateContactEmail.class,
        };
    }

    /**
     * Initializes the configuration tree.
     * @throws OXException if initializing doesn't work.
     */
    synchronized void init() throws OXException {
        if (null != treeReference.get()) {
            LOG.error("Duplicate initialization of configuration tree.");
            return;
        }

        TreeSetting tree = new TreeSetting("", -1, new SharedNode(""));
        treeReference.set(tree);
        boolean error = true;
        try {
            final Class< ? extends PreferencesItemService>[] clazzes = getClasses();
            final PreferencesItemService[] items = new PreferencesItemService[clazzes.length];
            for (int i = 0; i < clazzes.length; i++) {
                items[i] = clazzes[i].newInstance();
            }
            for (final PreferencesItemService item : items) {
                addSharedValue(tree, item.getPath(), item.getSharedValue());
            }
            error = false;
        } catch (OXException e) {
            throw e;
        } catch (InstantiationException e) {
            throw SettingExceptionCodes.INIT.create(e);
        } catch (IllegalAccessException e) {
            throw SettingExceptionCodes.INIT.create(e);
        } catch (Exception e) {
            throw SettingExceptionCodes.INIT.create(e);
        } finally {
            if (error) {
                treeReference.set(null);
            }
        }
    }

    synchronized void stop() {
        if (null == treeReference.get()) {
            LOG.error("Duplicate shutdown of configuration tree.");
            return;
        }

        try {
            final Class< ? extends PreferencesItemService>[] clazzes = getClasses();
            final PreferencesItemService[] items = new PreferencesItemService[clazzes.length];
            for (int i = 0; i < clazzes.length; i++) {
                items[i] = clazzes[i].newInstance();
            }
            for (final PreferencesItemService item : items) {
                removePreferencesItem(item);
            }
        } catch (InstantiationException e) {
            final OXException se = SettingExceptionCodes.INIT.create(e);
            LOG.error("", se);
        } catch (IllegalAccessException e) {
            final OXException se = SettingExceptionCodes.INIT.create(e);
            LOG.error("", se);
        } catch (Exception e) {
            final OXException se = SettingExceptionCodes.INIT.create(e);
            LOG.error("", se);
        } finally {
            treeReference.set(null);
        }
    }

}
