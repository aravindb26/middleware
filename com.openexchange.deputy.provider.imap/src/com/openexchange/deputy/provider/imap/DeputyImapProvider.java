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

package com.openexchange.deputy.provider.imap;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.mail.Folder;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import com.openexchange.deputy.DefaultModulePermission;
import com.openexchange.deputy.DefaultPermission;
import com.openexchange.deputy.DefaultPermission.Builder;
import com.openexchange.deputy.DeputyExceptionCode;
import com.openexchange.deputy.DeputyInfo;
import com.openexchange.deputy.DeputyModuleProvider;
import com.openexchange.deputy.KnownModuleId;
import com.openexchange.deputy.ModulePermission;
import com.openexchange.deputy.Permission;
import com.openexchange.deputy.Permissions;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.cache.CacheFolderStorage;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapExceptionCode;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.groupware.userconfiguration.UserPermissionBitsStorage;
import com.openexchange.imap.ACLPermission;
import com.openexchange.imap.IMAPAccess;
import com.openexchange.imap.IMAPProvider;
import com.openexchange.imap.cache.ListLsubCache;
import com.openexchange.imap.config.IMAPConfig;
import com.openexchange.imap.converters.IMAPFolderConverter;
import com.openexchange.imap.entity2acl.Entity2ACL;
import com.openexchange.imap.entity2acl.Entity2ACLArgs;
import com.openexchange.imap.entity2acl.Entity2ACLExceptionCode;
import com.openexchange.imap.util.IMAPDeputyMetadataUtility;
import com.openexchange.java.Reference;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailSessionCache;
import com.openexchange.mail.Protocol;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailCapabilities;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.service.MailService;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mailaccount.Account;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

/**
 * {@link DeputyImapProvider} - The IMAP provider for deputy permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyImapProvider implements DeputyModuleProvider {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DeputyImapProvider.class);

    /** The implementation version of the IMAP provider for deputy permission */
    public static final int VERSION = 1;

    private final ServiceLookup services;

    /**
     * Initializes a new {@link DeputyImapProvider}.
     *
     * @param services The service look-up
     */
    public DeputyImapProvider(ServiceLookup services) {
        super();
        this.services = services;
    }

    private UserPermissionBits getPermissionBits(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getUserPermissionBits();
        }
        return UserPermissionBitsStorage.getInstance().getUserPermissionBits(session.getUserId(), session.getContextId());
    }

    private Context getContext(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        return ContextStorage.getInstance().getContext(session.getContextId());
    }

    private boolean existsEntity(DeputyInfo deputyInfo, Context context) throws OXException {
        int entityId = deputyInfo.getEntityId();
        boolean group = deputyInfo.isGroup();

        if (group) {
            return services.getServiceSafe(GroupService.class).exists(context, entityId);
        }
        return services.getServiceSafe(UserService.class).exists(entityId, context.getContextId());
    }

    @Override
    public String getModuleId() {
        return KnownModuleId.MAIL.getId();
    }

    @Override
    public boolean isApplicable(Optional<ModulePermission> optionalModulePermission, Session session) throws OXException {
        if (getPermissionBits(session).hasWebMail() == false) {
            // No mail permission at all
            return false;
        }

        if (optionalModulePermission.isPresent()) {
            int[] accountCandidates = getAccountCandidates(session, true);

            Set<Integer> accountIds = new LinkedHashSet<Integer>(accountCandidates.length);
            for (String folderId : determineFolderIds(optionalModulePermission.get())) {
                Optional<FullnameArgument> optArg = MailFolderUtility.optPrepareMailFolderParam(folderId);
                int accountId = optArg.isPresent() ? optArg.get().getAccountId() : Account.DEFAULT_ID;
                if (Arrays.binarySearch(accountCandidates, accountId) < 0) {
                    // Not contained in account candidates
                    LOG.debug("Folder {} is not contained in account candidates. Deputy permission cannot be applied for user {} in context {}", folderId, I(session.getUserId()), I(session.getContextId()));
                    return false;
                }
                accountIds.add(I(accountId));
            }

            for (Integer accountId : accountIds) {
                if (checkAccountForApplicability(accountId.intValue(), session) == false) {
                    return false;
                }
            }
        } else {
            for (int accountId : getAccountCandidates(session, true)) {
                if (checkAccountForApplicability(accountId, session) == false) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkAccountForApplicability(int accountId, Session session) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            MailService mailService = services.getOptionalService(MailService.class);
            if (null == mailService) {
                throw ServiceExceptionCode.absentService(MailService.class);
            }
            mailAccess = mailService.getMailAccess(session, accountId);

            // Check protocol
            Protocol protocol = mailAccess.getProvider().getProtocol();
            if (null == protocol || (!Protocol.ALL.equals(protocol.getName()) && !IMAPProvider.PROTOCOL_IMAP.equals(protocol))) {
                // Account is not IMAP
                LOG.debug("Account {} is not an IMAP account. Deputy permission cannot be applied for user {} in context {}", I(accountId), I(session.getUserId()), I(session.getContextId()));
                return false;
            }

            // Check capabilities
            mailAccess.connect();
            MailCapabilities capabilities = mailAccess.getMailConfig().getCapabilities();
            if (!capabilities.hasPermissions()) {
                // Insufficient capabilities
                LOG.debug("Account {} does not have \"ACL\" capability. Deputy permission cannot be applied for user {} in context {}", I(accountId), I(session.getUserId()), I(session.getContextId()));
                return false;
            }
            if (!capabilities.hasMetadata()) {
                // Insufficient capabilities
                LOG.debug("Account {} does not have \"METADATA\" capability. Deputy permission cannot be applied for user {} in context {}", I(accountId), I(session.getUserId()), I(session.getContextId()));
                return false;
            }
            if (!((IMAPConfig) mailAccess.getMailConfig()).isSupportsACLs()) {
                // Insufficient capabilities
                LOG.debug("Account {} does not support ACLs (check property \"com.openexchange.imap.imapSupportsACL\"). Deputy permission cannot be applied for user {} in context {}", I(accountId), I(session.getUserId()), I(session.getContextId()));
                return false;
            }

            return true;
        } finally {
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public void grantDeputyPermission(DeputyInfo deputyInfo, ModulePermission modulePermission, Session session) throws OXException {
        int[] accountCandidates = getAccountCandidates(session, true);
        for (Map.Entry<Integer, List<String>> entry : getGroupedFolderIds(modulePermission).entrySet()) {
            int accountId = entry.getKey().intValue();
            if (Arrays.binarySearch(accountCandidates, accountId) < 0) {
                // Not contained in account candidates
                throw DeputyImapProviderExceptionCode.MAIL_ACCOUNT_NOT_SUPPORTED.create(I(accountId));
            }
            grantDeputyPermissionByFolders(accountId, entry.getValue(), deputyInfo, modulePermission.getPermission(), session);
        }
    }

    private void grantDeputyPermissionByFolders(int accountId, Collection<? extends String> folderIds, DeputyInfo deputyInfo, Permission permission, Session session) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            MailService mailService = services.getOptionalService(MailService.class);
            if (null == mailService) {
                throw ServiceExceptionCode.absentService(MailService.class);
            }

            mailAccess = mailService.getMailAccess(session, accountId);
            mailAccess.connect(false);

            Context context = getContext(session);
            IMAPConfig imapConfig = (IMAPConfig) mailAccess.getMailConfig();
            IMAPStore imapStore = IMAPAccess.getIMAPFolderStorageFrom(mailAccess).getImapStore();

            List<String> foldersToClear = new ArrayList<String>(folderIds.size());
            try {
                for (String folderId : folderIds) {
                    String fullName = MailFolderUtility.prepareMailFolderParamOrElseReturn(folderId);
                    IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullName);

                    imapFolder.open(Folder.READ_WRITE);
                    try {
                        boolean selectable = (imapFolder.getType() & IMAPFolder.HOLDS_MESSAGES) > 0;
                        if (selectable == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_SELECT_FOLDER.create(folderId);
                        }

                        // Check rights
                        Rights ownRights = IMAPFolderConverter.getOwnRights(imapFolder, session, imapConfig);
                        if (ownRights == null || imapConfig.getACLExtension().canGetACL(ownRights) == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_GET_ACL.create(folderId);
                        }
                        if (imapConfig.getACLExtension().canSetACL(ownRights) == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_SET_ACL.create(folderId);
                        }

                        // Determine deputy's ACL name
                        Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session.getUserId(), session, imapFolder, imapConfig);
                        String aclName = Entity2ACL.getInstance(imapStore, imapConfig).getACLName(deputyInfo.getEntityId(), context, args);

                        // Check possibly existent deputy meta-data in JSON format
                        Optional<JSONObject> optionalMetadata = IMAPDeputyMetadataUtility.getDeputyMetadata(imapFolder);
                        if (optionalMetadata.isPresent()) {
                            // Check existence of deputy meta-data
                            JSONObject jMetadata = optionalMetadata.get();
                            String removeKey = null;
                            for (Map.Entry<String, Object> e : jMetadata.entrySet()) {
                                JSONObject jDeputyMetadata = (JSONObject) e.getValue();
                                if (aclName.equals(jDeputyMetadata.optString("name", null))) {
                                    if (VERSION == jDeputyMetadata.optInt("version", 0)) {
                                        // Cannot grant deputy permission since already present
                                        throw DeputyImapProviderExceptionCode.DUPLICATE_DEPUTY_PERMISSION.create(folderId);
                                    }
                                    // Version mismatch
                                    removeKey = e.getKey();
                                    break;
                                }
                            }
                            if (removeKey != null) {
                                jMetadata.remove(removeKey);
                            }
                        }

                        // Check existent ACLs
                        ACL[] acls = imapFolder.getACL();
                        ACLPermission existentPermission = null;
                        for (int i = acls.length; existentPermission == null && i-- > 0;) {
                            try {
                                ACLPermission aclPerm = new ACLPermission();
                                aclPerm.parseACL(acls[i], args, imapStore, imapConfig, context);
                                if (aclPerm.getEntity() == deputyInfo.getEntityId()) {
                                    existentPermission = aclPerm;
                                }
                            } catch (OXException e) {
                                if (!isUnknownEntityError(e)) {
                                    throw e;
                                }

                                LOG.debug("Cannot map ACL entity named \"{}\" to a system user", acls[i].getName());
                            }
                        }

                        // Apply permission & store deputy meta-data
                        ACLPermission newPermission = new ACLPermission();
                        newPermission.setEntity(deputyInfo.getEntityId());
                        newPermission.setDeleteObjectPermission(permission.getDeletePermission());
                        newPermission.setFolderAdmin(permission.isAdmin());
                        newPermission.setFolderPermission(permission.getFolderPermission());
                        newPermission.setGroupPermission(permission.isGroup());
                        newPermission.setReadObjectPermission(permission.getReadPermission());
                        newPermission.setSystem(0);
                        newPermission.setWriteObjectPermission(permission.getWritePermission());

                        JSONObject jDeputyMetadata;
                        if (existentPermission == null) {
                            // No existent permission
                            imapFolder.addACL(newPermission.getPermissionACL(args, imapConfig, imapStore, context));
                            foldersToClear.add(folderId);
                            jDeputyMetadata = IMAPDeputyMetadataUtility.createDeputyMetadata(deputyInfo.getDeputyId(), deputyInfo.getEntityId(), Permissions.createPermissionBits(permission), aclName, VERSION, Optional.empty(), optionalMetadata);
                        } else {
                            // Deputy has already a permission on that IMAP folder
                            imapFolder.removeACL(aclName);
                            foldersToClear.add(folderId);
                            imapFolder.addACL(newPermission.getPermissionACL(args, imapConfig, imapStore, context));
                            jDeputyMetadata = IMAPDeputyMetadataUtility.createDeputyMetadata(deputyInfo.getDeputyId(), deputyInfo.getEntityId(), Permissions.createPermissionBits(permission), aclName, VERSION, Optional.of(existentPermission.getPermissionACL(args, imapConfig, imapStore, context)), optionalMetadata);
                        }
                        IMAPDeputyMetadataUtility.setDeputyMetadata(jDeputyMetadata, imapFolder);
                    } finally {
                        closeIMAPFolderSafe(imapFolder);
                    }
                }
            } catch (javax.mail.MessagingException e) {
                throw MimeMailException.handleMessagingException(e, imapConfig, session);
            } finally {
                if (foldersToClear.isEmpty() == false) {
                    clearCaches(foldersToClear, accountId, deputyInfo, session);
                }
            }
        } finally {
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public void updateDeputyPermission(DeputyInfo deputyInfo, ModulePermission modulePermission, Session session) throws OXException {
        int[] accountCandidates = getAccountCandidates(session, true);
        for (Map.Entry<Integer, List<String>> entry : getGroupedFolderIds(modulePermission).entrySet()) {
            int accountId = entry.getKey().intValue();
            if (Arrays.binarySearch(accountCandidates, accountId) < 0) {
                // Not contained in account candidates
                throw DeputyImapProviderExceptionCode.MAIL_ACCOUNT_NOT_SUPPORTED.create(I(accountId));
            }
            updateDeputyPermissionForAccount(accountId, entry.getValue(), deputyInfo, modulePermission.getPermission(), session);
        }
    }

    private void updateDeputyPermissionForAccount(int accountId, List<String> folderIds, DeputyInfo deputyInfo, Permission permission, Session session) throws OXException {
        Set<String> removeFolderIds;
        Set<String> newFolderIds;
        Set<String> updateFolderIds;
        {
            Optional<ModulePermission> existentModulePermission = getDeputyPermission(deputyInfo, session);
            if (existentModulePermission.isPresent()) {
                List<String> existentFolderIds = existentModulePermission.get().getOptionalFolderIds().get();
                removeFolderIds = new HashSet<String>(existentFolderIds);
                removeFolderIds.removeAll(folderIds);

                newFolderIds = new HashSet<String>(folderIds);
                newFolderIds.removeAll(existentFolderIds);

                updateFolderIds = new HashSet<String>(folderIds);
                updateFolderIds.retainAll(existentFolderIds);
            } else {
                removeFolderIds = Collections.emptySet();
                newFolderIds = Collections.emptySet();
                updateFolderIds = new HashSet<String>(folderIds);
            }
        }

        if (updateFolderIds.isEmpty() == false) {
            updateDeputyPermissionByFolders(accountId, updateFolderIds, deputyInfo, permission, session);
        }

        if (newFolderIds.isEmpty() == false) {
            grantDeputyPermissionByFolders(accountId, newFolderIds, deputyInfo, permission, session);
        }

        if (removeFolderIds.isEmpty() == false) {
            revokeDeputyPermissionByFolders(accountId, removeFolderIds, deputyInfo, session);
        }
    }

    private void updateDeputyPermissionByFolders(int accountId, Collection<? extends String> folderIds, DeputyInfo deputyInfo, Permission permission, Session session) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            MailService mailService = services.getOptionalService(MailService.class);
            if (null == mailService) {
                throw ServiceExceptionCode.absentService(MailService.class);
            }

            mailAccess = mailService.getMailAccess(session, accountId);
            mailAccess.connect(false);

            Context context = getContext(session);
            IMAPConfig imapConfig = (IMAPConfig) mailAccess.getMailConfig();
            IMAPStore imapStore = IMAPAccess.getIMAPFolderStorageFrom(mailAccess).getImapStore();

            List<String> foldersToClear = new ArrayList<String>(folderIds.size());
            try {
                Boolean entityExists = null;
                for (String folderId : folderIds) {
                    String fullName = MailFolderUtility.prepareMailFolderParamOrElseReturn(folderId);
                    IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullName);

                    imapFolder.open(Folder.READ_WRITE);
                    try {
                        boolean selectable = (imapFolder.getType() & IMAPFolder.HOLDS_MESSAGES) > 0;
                        if (selectable == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_SELECT_FOLDER.create(folderId);
                        }

                        // Check rights
                        Rights ownRights = IMAPFolderConverter.getOwnRights(imapFolder, session, imapConfig);
                        if (ownRights == null || imapConfig.getACLExtension().canGetACL(ownRights) == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_GET_ACL.create(folderId);
                        }

                        // Check existence of meta-data
                        Optional<JSONObject> optionalMetadata = IMAPDeputyMetadataUtility.getDeputyMetadata(imapFolder);
                        if (!optionalMetadata.isPresent()) {
                            // Cannot update deputy permission since not present
                            throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                        }

                        // Check existence of deputy meta-data
                        JSONObject jMetadata = optionalMetadata.get();
                        JSONObject jDeputyMetadata = jMetadata.optJSONObject(deputyInfo.getDeputyId());
                        if (jDeputyMetadata == null || VERSION != jDeputyMetadata.optInt("version", 0)) {
                            // Version mismatch
                            throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                        }

                        // Check entity existence
                        if (entityExists == null) {
                            entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
                        }
                        if (entityExists.booleanValue() == false) {
                            //  Entity does no more exist
                            jMetadata.remove(deputyInfo.getDeputyId());
                            if (jMetadata.length() <= 0) {
                                IMAPDeputyMetadataUtility.unsetDeputyMetadata(imapFolder);
                            } else {
                                IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                            }
                        } else {
                            // Determine & validate deputy's ACL name
                            Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session.getUserId(), session, imapFolder, imapConfig);
                            String aclName = Entity2ACL.getInstance(imapStore, imapConfig).getACLName(deputyInfo.getEntityId(), context, args);

                            if (!aclName.equals(jDeputyMetadata.optString("name", null))) {
                                // Version mismatch
                                throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                            }

                            // Check existent ACLs
                            ACL[] acls = imapFolder.getACL();
                            ACLPermission existentPermission = null;
                            for (int i = acls.length; existentPermission == null && i-- > 0;) {
                                try {
                                    ACLPermission aclPerm = new ACLPermission();
                                    aclPerm.parseACL(acls[i], args, imapStore, imapConfig, context);
                                    if (aclPerm.getEntity() == deputyInfo.getEntityId()) {
                                        existentPermission = aclPerm;
                                    }
                                } catch (OXException e) {
                                    if (!isUnknownEntityError(e)) {
                                        throw e;
                                    }

                                    LOG.debug("Cannot map ACL entity named \"{}\" to a system user", acls[i].getName());
                                }
                            }

                            // Apply permission & store deputy meta-data
                            if (existentPermission == null) {
                                throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                            }

                            // Deputy has a permission on that IMAP folder
                            if (imapConfig.getACLExtension().canSetACL(ownRights) == false) {
                                throw DeputyImapProviderExceptionCode.CANNOT_SET_ACL.create(folderId);
                            }

                            ACLPermission newPermission = new ACLPermission();
                            newPermission.setEntity(deputyInfo.getEntityId());
                            newPermission.setDeleteObjectPermission(permission.getDeletePermission());
                            newPermission.setFolderAdmin(permission.isAdmin());
                            newPermission.setFolderPermission(permission.getFolderPermission());
                            newPermission.setGroupPermission(permission.isGroup());
                            newPermission.setReadObjectPermission(permission.getReadPermission());
                            newPermission.setSystem(0);
                            newPermission.setWriteObjectPermission(permission.getWritePermission());

                            imapFolder.removeACL(aclName);
                            foldersToClear.add(folderId);
                            imapFolder.addACL(newPermission.getPermissionACL(args, imapConfig, imapStore, context));

                            jDeputyMetadata.put("permission", Permissions.createPermissionBits(permission));
                            IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                        }
                    } catch (JSONException e) {
                        throw DeputyExceptionCode.JSON_ERROR.create(e, e.getMessage());
                    } finally {
                        closeIMAPFolderSafe(imapFolder);
                    }
                }

            } catch (javax.mail.MessagingException e) {
                throw MimeMailException.handleMessagingException(e, imapConfig, session);
            } finally {
                if (foldersToClear.isEmpty() == false) {
                    clearCaches(folderIds, accountId, deputyInfo, session);
                }
            }
        } finally {
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    private void revokeDeputyPermissionByFolders(int accountId, Collection<? extends String> folderIds, DeputyInfo deputyInfo, Session session) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            MailService mailService = services.getOptionalService(MailService.class);
            if (null == mailService) {
                throw ServiceExceptionCode.absentService(MailService.class);
            }

            mailAccess = mailService.getMailAccess(session, accountId);
            mailAccess.connect(false);

            Context context = getContext(session);
            IMAPConfig imapConfig = (IMAPConfig) mailAccess.getMailConfig();
            IMAPStore imapStore = IMAPAccess.getIMAPFolderStorageFrom(mailAccess).getImapStore();

            List<String> foldersToClear = new ArrayList<String>(folderIds.size());
            try {
                Boolean entityExists = null;
                for (String folderId : folderIds) {
                    String fullName = MailFolderUtility.prepareMailFolderParamOrElseReturn(folderId);
                    IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullName);

                    imapFolder.open(Folder.READ_WRITE);
                    try {
                        boolean selectable = (imapFolder.getType() & IMAPFolder.HOLDS_MESSAGES) > 0;
                        if (selectable == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_SELECT_FOLDER.create(folderId);
                        }

                        // Check rights
                        Rights ownRights = IMAPFolderConverter.getOwnRights(imapFolder, session, imapConfig);
                        if (ownRights == null || imapConfig.getACLExtension().canGetACL(ownRights) == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_GET_ACL.create(folderId);
                        }
                        if (imapConfig.getACLExtension().canSetACL(ownRights) == false) {
                            throw DeputyImapProviderExceptionCode.CANNOT_SET_ACL.create(folderId);
                        }

                        // Check existence of meta-data
                        Optional<JSONObject> optionalMetadata = IMAPDeputyMetadataUtility.getDeputyMetadata(imapFolder);
                        if (!optionalMetadata.isPresent()) {
                            // Cannot update deputy permission since not present
                            throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                        }

                        // Check existence of deputy meta-data
                        JSONObject jMetadata = optionalMetadata.get();
                        JSONObject jDeputyMetadata = jMetadata.optJSONObject(deputyInfo.getDeputyId());
                        if (jDeputyMetadata == null || VERSION != jDeputyMetadata.optInt("version", 0)) {
                            // Version mismatch
                            throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                        }

                        // Check entity existence
                        if (entityExists == null) {
                            entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
                        }
                        if (entityExists.booleanValue() == false) {
                            //  Entity does no more exist
                            jMetadata.remove(deputyInfo.getDeputyId());
                            if (jMetadata.length() <= 0) {
                                IMAPDeputyMetadataUtility.unsetDeputyMetadata(imapFolder);
                            } else {
                                IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                            }
                        } else {
                            // Determine deputy's ACL name
                            Entity2ACLArgs args = IMAPFolderConverter.getEntity2AclArgs(session.getUserId(), session, imapFolder, imapConfig);
                            String aclName = Entity2ACL.getInstance(imapStore, imapConfig).getACLName(deputyInfo.getEntityId(), context, args);

                            if (!aclName.equals(jDeputyMetadata.optString("name", null))) {
                                // Version mismatch
                                throw DeputyImapProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                            }

                            imapFolder.removeACL(aclName);
                            foldersToClear.add(folderId);

                            String previousRights = jDeputyMetadata.optString("previousRights", null);
                            if (previousRights != null) {
                                imapFolder.addACL(new ACL(aclName, new Rights(previousRights)));
                            }

                            jMetadata.remove(deputyInfo.getDeputyId());
                            if (jMetadata.isEmpty()) {
                                IMAPDeputyMetadataUtility.unsetDeputyMetadata(imapFolder);
                            } else {
                                IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                            }
                        }
                    } finally {
                        closeIMAPFolderSafe(imapFolder);
                    }
                }

                clearCaches(folderIds, accountId, deputyInfo, session);
            } catch (javax.mail.MessagingException e) {
                throw MimeMailException.handleMessagingException(e, imapConfig, session);
            } finally {
                if (foldersToClear.isEmpty() == false) {
                    clearCaches(folderIds, accountId, deputyInfo, session);
                }
            }
        } finally {
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public void revokeDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException {
        for (int accountId : getAccountCandidates(session, false)) {
            revokeDeputyPermission(accountId, deputyInfo, session);
        }
    }

    private void revokeDeputyPermission(int accountId, DeputyInfo deputyInfo, Session session) throws OXException {
        Context context = getContext(session);

        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            MailService mailService = services.getOptionalService(MailService.class);
            if (null == mailService) {
                throw ServiceExceptionCode.absentService(MailService.class);
            }

            mailAccess = mailService.getMailAccess(session, accountId);
            mailAccess.connect(false);

            IMAPConfig imapConfig = (IMAPConfig) mailAccess.getMailConfig();
            IMAPStore imapStore = IMAPAccess.getIMAPFolderStorageFrom(mailAccess).getImapStore();

            List<String> foldersToClear = null;
            try {
                Map<String, JSONObject> deputyMetadatas = IMAPDeputyMetadataUtility.getAllFoldersHavingDeputy(deputyInfo.getDeputyId(), imapStore);
                foldersToClear = new ArrayList<String>(deputyMetadatas.size());
                Boolean entityExists = null;
                for (Map.Entry<String, JSONObject> deputyMetadataEntry : deputyMetadatas.entrySet()) {
                    String fullName = deputyMetadataEntry.getKey();
                    JSONObject jMetadata = deputyMetadataEntry.getValue();

                    // Look-up deputy metadata for given deputy identifier
                    JSONObject jDeputyMetadata = jMetadata.optJSONObject(deputyInfo.getDeputyId());
                    if (jDeputyMetadata != null) {
                        String aclName = jDeputyMetadata.optString("name", null);
                        IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullName);

                        imapFolder.removeACL(aclName);
                        foldersToClear.add(MailFolderUtility.prepareFullname(accountId, fullName));

                        // Check entity existence
                        if (entityExists == null) {
                            entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
                        }
                        if (entityExists.booleanValue() == false) {
                            //  Entity does no more exist
                            jMetadata.remove(deputyInfo.getDeputyId());
                            if (jMetadata.length() <= 0) {
                                IMAPDeputyMetadataUtility.unsetDeputyMetadata(imapFolder);
                            } else {
                                IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                            }
                        } else {
                            String previousRights = jDeputyMetadata.optString("previousRights", null);
                            if (previousRights != null) {
                                imapFolder.addACL(new ACL(aclName, new Rights(previousRights)));
                            }

                            jMetadata.remove(deputyInfo.getDeputyId());
                            if (jMetadata.isEmpty()) {
                                IMAPDeputyMetadataUtility.unsetDeputyMetadata(imapFolder);
                            } else {
                                IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                            }
                        }
                    }
                }
            } catch (javax.mail.MessagingException e) {
                throw MimeMailException.handleMessagingException(e, imapConfig, session);
            } finally {
                if (foldersToClear != null && foldersToClear.isEmpty() == false) {
                    clearCaches(foldersToClear, accountId, deputyInfo, session);
                }
            }
        } finally {
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public Optional<ModulePermission> getDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException {
        Reference<Permission> permissionReference = new Reference<Permission>();
        List<String> folderIds = null;
        for (int accountId : getAccountCandidates(session, false)) {
            List<String> accountFolderIds = getDeputyPermissionFrom(accountId, permissionReference, deputyInfo, session);
            if (accountFolderIds != null && !accountFolderIds.isEmpty()) {
                if (folderIds == null) {
                    folderIds = accountFolderIds;
                } else {
                    folderIds.addAll(accountFolderIds);
                }
            }
        }

        if (folderIds == null) {
            return Optional.empty();
        }

        return Optional.of(DefaultModulePermission.builder()
            .withModuleId(KnownModuleId.MAIL.getId())
            .withFolderIds(folderIds)
            .withPermission(permissionReference.getValue())
            .build());
    }

    private List<String> getDeputyPermissionFrom(int accountId, Reference<Permission> permissionReference, DeputyInfo deputyInfo, Session session) throws OXException {
        Context context = getContext(session);

        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            MailService mailService = services.getOptionalService(MailService.class);
            if (null == mailService) {
                throw ServiceExceptionCode.absentService(MailService.class);
            }

            mailAccess = mailService.getMailAccess(session, accountId);
            mailAccess.connect(false);

            IMAPConfig imapConfig = (IMAPConfig) mailAccess.getMailConfig();
            IMAPStore imapStore = IMAPAccess.getIMAPFolderStorageFrom(mailAccess).getImapStore();

            try {
                Map<String, JSONObject> fullName2Metadata = IMAPDeputyMetadataUtility.getAllFoldersHavingDeputy(deputyInfo.getDeputyId(), imapStore);
                int numberOfFolders = fullName2Metadata.size();
                if (numberOfFolders <= 0) {
                    return Collections.emptyList();
                }

                Boolean entityExists = null;
                List<String> folderIds = new ArrayList<String>(numberOfFolders);
                for (Map.Entry<String, JSONObject> deputyMetadataEntry : fullName2Metadata.entrySet()) {
                    String fullName = deputyMetadataEntry.getKey();
                    JSONObject jMetadata = deputyMetadataEntry.getValue();

                    // Look-up deputy metadata for given deputy identifier
                    JSONObject jDeputyMetadata = jMetadata.optJSONObject(deputyInfo.getDeputyId());
                    if (jDeputyMetadata != null) {
                        // Check entity existence
                        if (entityExists == null) {
                            entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
                        }
                        if (entityExists.booleanValue() == false) {
                            //  Entity does no more exist
                            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullName);
                            jMetadata.remove(deputyInfo.getDeputyId());
                            if (jMetadata.length() <= 0) {
                                IMAPDeputyMetadataUtility.unsetDeputyMetadata(imapFolder);
                            } else {
                                IMAPDeputyMetadataUtility.setDeputyMetadata(jMetadata, imapFolder);
                            }
                        } else {
                            folderIds.add(MailFolderUtility.prepareFullname(accountId, fullName));
                            if (permissionReference.hasNoValue()) {
                                int[] bits = Permissions.parsePermissionBits(jDeputyMetadata.optInt("permission", 0));
                                Builder tmp = DefaultPermission.builder()
                                    .withFolderPermission(bits[0])
                                    .withReadPermission(bits[1])
                                    .withWritePermission(bits[2])
                                    .withDeletePermission(bits[3])
                                    .withAdmin(bits[4] > 0)
                                    .withEntity(deputyInfo.getEntityId())
                                    .withGroup(deputyInfo.isGroup());
                                permissionReference.setValue(tmp.build());
                            }
                        }
                    }
                }

                return folderIds;
            } catch (javax.mail.MessagingException e) {
                throw MimeMailException.handleMessagingException(e, imapConfig, session);
            }
        } finally {
            if (mailAccess != null) {
                mailAccess.close(true);
            }
        }
    }

    // ------------------------------------------------- Utility methods -------------------------------------------------------------------

    private static boolean isUnknownEntityError(OXException e) {
        final int code = e.getCode();
        return (e.isPrefix("ACL") && (Entity2ACLExceptionCode.RESOLVE_USER_FAILED.getNumber() == code)) || (e.isPrefix("USR") && (LdapExceptionCode.USER_NOT_FOUND.getNumber() == code));
    }

    /**
     * Artificial flag since first implementation of deputy permission only covers default mail account's INBOX folder.
     * <p>
     * This flag can be removed later on to let other code become effective.
     */
    private static final boolean ALLOW_PRIMARY_ONLY = true;

    private int[] getAccountCandidates(Session session, boolean sort) throws OXException {
        if (ALLOW_PRIMARY_ONLY) {
            return new int[] { Account.DEFAULT_ID };
        }

        TIntList accountIds = null;
        MailAccountStorageService mass = services.getServiceSafe(MailAccountStorageService.class);
        for (MailAccount mailAccount : mass.getUserMailAccounts(session.getUserId(), session.getContextId())) {
            if (mailAccount.isDefaultOrSecondaryAccount() && !mailAccount.isDeactivated() && "imap".equals(mailAccount.getMailProtocol())) {
                if (accountIds == null) {
                    accountIds = new TIntArrayList(4);
                }
                accountIds.add(mailAccount.getId());
            }
        }
        if (accountIds == null) {
            return new int[0];
        }
        if (sort) {
            accountIds.sort();
        }
        return accountIds.toArray();
    }

    private void clearCaches(Collection<? extends String> folderIds, int accountId, DeputyInfo deputyInfo, Session session) {
        ListLsubCache.clearCache(accountId, session);
        ListLsubCache.dropFor(deputyInfo.getEntityId(), session.getContextId());
        MailSessionCache.clearFor(deputyInfo.getEntityId(), session.getContextId());

        CacheFolderStorage.getInstance().removeSingleFromCache(folderIds, CacheFolderStorage.REAL_TREE_ID, session.getUserId(), session, false);
        CacheFolderStorage.dropUserEntries(deputyInfo.getEntityId(), session.getContextId(), true);
    }

    private Map<Integer, List<String>> getGroupedFolderIds(ModulePermission modulePermission) {
        Map<Integer, List<String>> groupedFolderIds = new LinkedHashMap<Integer, List<String>>(4);
        for (String folderId : determineFolderIds(modulePermission)) {
            Optional<FullnameArgument> optArg = MailFolderUtility.optPrepareMailFolderParam(folderId);
            if (optArg.isPresent()) {
                // E.g. "default2/INBOX"
                Integer accountId =  I(optArg.get().getAccountId());
                List<String> folderIds = groupedFolderIds.get(accountId);
                if (folderIds == null) {
                    folderIds = new ArrayList<String>(4);
                    groupedFolderIds.put(accountId, folderIds);
                }
                folderIds.add(folderId);
            } else {
                // E.g. "INBOX"
                Integer accountId =  I(Account.DEFAULT_ID);
                List<String> folderIds = groupedFolderIds.get(accountId);
                if (folderIds == null) {
                    folderIds = new ArrayList<String>(4);
                    groupedFolderIds.put(accountId, folderIds);
                }
                folderIds.add(MailFolderUtility.prepareFullname(accountId.intValue(), folderId));
            }
        }
        return groupedFolderIds;
    }

    private List<String> determineFolderIds(ModulePermission modulePermission) {
        Optional<List<String>> optionalFolderIds = modulePermission.getOptionalFolderIds();
        return optionalFolderIds.isPresent() ? optionalFolderIds.get() : Collections.singletonList(MailFolderUtility.prepareFullname(Account.DEFAULT_ID, "INBOX"));
    }

    private static void closeIMAPFolderSafe(IMAPFolder imapFolder) {
        try {
            imapFolder.close(false);
        } catch (Exception e) {
            // Ignore
        }
    }

}
