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

package com.openexchange.deputy.impl;

import static com.openexchange.java.Strings.getEmptyStrings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.openexchange.deputy.DeputyPermission;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.context.ContextService;
import com.openexchange.deputy.DefaultActiveDeputyPermission;
import com.openexchange.deputy.DefaultGrantedDeputyPermissions;
import com.openexchange.deputy.DefaultGrantee;
import com.openexchange.deputy.DeputyExceptionCode;
import com.openexchange.deputy.DeputyInfo;
import com.openexchange.deputy.DeputyModuleProvider;
import com.openexchange.deputy.DeputyService;
import com.openexchange.deputy.GrantedDeputyPermissions;
import com.openexchange.deputy.ActiveDeputyPermission;
import com.openexchange.deputy.Grantee;
import com.openexchange.deputy.ModulePermission;
import com.openexchange.deputy.impl.storage.DeputyStorage;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.UpdateBehavior;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.user.UserService;


/**
 * {@link DeputyServiceImpl} - The implementation of deputy service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyServiceImpl implements DeputyService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DeputyServiceImpl.class);

    private final DeputyModuleProviderRegistry providerRegistry;
    private final DeputyStorage deputyStorage;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link DeputyServiceImpl}.
     *
     * @param providerRegistry The provider registry
     * @param deputyStorage The deputy storage to use
     * @param services The service look-up
     */
    public DeputyServiceImpl(DeputyModuleProviderRegistry providerRegistry, DeputyStorage deputyStorage, ServiceLookup services) {
        super();
        this.providerRegistry = providerRegistry;
        this.deputyStorage = deputyStorage;
        this.services = services;
    }

    @Override
    public boolean isAvailable(Session session) throws OXException {
        ConfigViewFactory viewFactory = services.getServiceSafe(ConfigViewFactory.class);
        ConfigView view = viewFactory.getView(session.getUserId(), session.getContextId());
        return ConfigViews.getDefinedBoolPropertyFrom("com.openexchange.deputy.enabled", false, view);
    }

    @Override
    public List<String> getAvailableModules(Session session) throws OXException {
        List<String> moduleIds = providerRegistry.getAvailableModuleIds();
        for (Iterator<String> it = moduleIds.iterator(); it.hasNext();) {
            String moduleId = it.next();
            DeputyModuleProvider optionalProvider = providerRegistry.getHighestRankedProviderFor(moduleId);
            if (optionalProvider == null || !optionalProvider.isApplicable(Optional.empty(), session)) {
                it.remove();
            }
        }
        return moduleIds;
    }

    private void deleteFromStorageSafe(String deputyId, Session session) {
        try {
            deputyStorage.delete(deputyId, session);
        } catch (Exception e) {
            LOG.error("Failed to delete failed deputy permission with identifier: {}", deputyId, e);
        }
    }

    private DeputyModuleProvider requireProviderFor(String moduleId, boolean checkApplicability, ModulePermission modulePermission, Session session) throws OXException {
        DeputyModuleProvider optionalProvider = providerRegistry.getHighestRankedProviderFor(moduleId);
        if (optionalProvider == null) {
            // No such provider
            throw DeputyExceptionCode.NO_SUCH_PROVIDER.create(moduleId);
        }

        if (checkApplicability && !optionalProvider.isApplicable(Optional.ofNullable(modulePermission), session)) {
            throw DeputyExceptionCode.PROVIDER_DISABLED.create(moduleId);
        }

        return optionalProvider;
    }

    @Override
    public String grantDeputyPermission(DeputyPermission deputyPermission, Session session) throws OXException {
        DeputyInfo deputyInfo = null;
        try {
            if (!deputyPermission.isGroup() && deputyPermission.getEntityId() == session.getUserId()) {
                throw DeputyExceptionCode.NO_SELF_DEPUTY.create();
            }

            List<ModulePermission> modulePermissions = deputyPermission.getModulePermissions();
            // Ensure providers are available
            int numberOfModulePermissions = modulePermissions.size();
            Map<DeputyModuleProvider, ModulePermission> provider2permission = new LinkedHashMap<>(numberOfModulePermissions);
            List<String> moduleIds = new ArrayList<String>(numberOfModulePermissions);
            for (ModulePermission modulePermission : modulePermissions) {
                if (modulePermission.getPermission().isVisible()) {
                    provider2permission.put(requireProviderFor(modulePermission.getModuleId(), true, modulePermission, session), modulePermission);
                    moduleIds.add(modulePermission.getModuleId());
                }
            }

            // Add to storage
            deputyInfo = deputyStorage.store(deputyPermission.getEntityId(), deputyPermission.isGroup(), deputyPermission.isSendOnBehalfOf(), moduleIds, session);

            // Perform grant
            for (Map.Entry<DeputyModuleProvider, ModulePermission> entry : provider2permission.entrySet()) {
                entry.getKey().grantDeputyPermission(deputyInfo, entry.getValue(), session);
            }

            String id2Return = deputyInfo.getDeputyId();
            deputyInfo = null;
            return id2Return;
        } finally {
            if (deputyInfo != null) {
                deleteFromStorageSafe(deputyInfo.getDeputyId(), session);
            }
        }
    }

    @Override
    public void updateDeputyPermission(String deputyId, DeputyPermission deputyPermission, Session session) throws OXException {
        // Test existence & validity
        DeputyInfo deputyInfo = deputyStorage.get(deputyId, session);
        if (deputyInfo.getEntityId() != deputyPermission.getEntityId()) {
            throw DeputyExceptionCode.NO_SUCH_DEPUTY.create(deputyId);
        }
        if (deputyInfo.isGroup() != deputyPermission.isGroup()) {
            throw DeputyExceptionCode.NO_SUCH_DEPUTY.create(deputyId);
        }

        // Ensure providers are available
        List<ModulePermission> modulePermissions = deputyPermission.getModulePermissions();
        Set<String> leftOverModuleIds = new HashSet<>(deputyInfo.getModuleIds());
        int numberOfModulePermissions = modulePermissions.size();
        Map<DeputyModuleProvider, ModulePermission> grantprovider2permission = new HashMap<>(numberOfModulePermissions);
        Map<DeputyModuleProvider, ModulePermission> updateprovider2permission = new HashMap<>(numberOfModulePermissions);
        List<String> moduleIds = new ArrayList<String>(numberOfModulePermissions);
        for (ModulePermission modulePermission : modulePermissions) {
            String moduleId = modulePermission.getModuleId();
            if (leftOverModuleIds.remove(moduleId)) {
                // Contained
                if (modulePermission.getPermission().isVisible()) {
                    updateprovider2permission.put(requireProviderFor(moduleId, true, modulePermission, session), modulePermission);
                    moduleIds.add(moduleId);
                } else {
                    leftOverModuleIds.add(moduleId);
                }
            } else {
                // Not contained. Hence, new provider.
                if (modulePermission.getPermission().isVisible()) {
                    grantprovider2permission.put(requireProviderFor(moduleId, true, modulePermission, session), modulePermission);
                    moduleIds.add(moduleId);
                }
            }
        }

        // Revoke from previous providers
        List<DeputyModuleProvider> providers2Revoke = new ArrayList<DeputyModuleProvider>(leftOverModuleIds.size());
        for (String moduleId : leftOverModuleIds) {
            providers2Revoke.add(requireProviderFor(moduleId, false, null, session));
        }
        for (DeputyModuleProvider provider : providers2Revoke) {
            provider.revokeDeputyPermission(deputyInfo, session);
        }

        // Update deputy information
        DeputyInfo newDeputyInfo = deputyStorage.update(deputyId, deputyPermission.isSendOnBehalfOf(), moduleIds, session);
        if (newDeputyInfo == null) {
            throw DeputyExceptionCode.NO_SUCH_DEPUTY.create(deputyId);
        }

        // Perform grant
        for (Map.Entry<DeputyModuleProvider, ModulePermission> entry : grantprovider2permission.entrySet()) {
            entry.getKey().grantDeputyPermission(newDeputyInfo, entry.getValue(), session);
        }

        // Perform update
        for (Map.Entry<DeputyModuleProvider, ModulePermission> entry : updateprovider2permission.entrySet()) {
            entry.getKey().updateDeputyPermission(newDeputyInfo, entry.getValue(), session);
        }
    }

    @Override
    public void revokeDeputyPermission(String deputyId, Session session) throws OXException {
        // Test existence
        DeputyInfo deputyInfo = deputyStorage.get(deputyId, session);

        // Ensure providers are available
        List<String> moduleIds = deputyInfo.getModuleIds();
        List<DeputyModuleProvider> providers2Revoke = new ArrayList<DeputyModuleProvider>(moduleIds.size());
        for (String moduleId : moduleIds) {
            providers2Revoke.add(requireProviderFor(moduleId, false, null, session));
        }

        // Revoke...
        List<String> removedModuleIds = new ArrayList<String>(providers2Revoke.size());
        boolean allRevoked = true;
        for (DeputyModuleProvider provider : providers2Revoke) {
            String moduleId = provider.getModuleId();
            try {
                provider.revokeDeputyPermission(deputyInfo, session);
                removedModuleIds.add(moduleId);
            } catch (Exception e) {
                LOG.warn("Failed to revoke deputy permission with identifier {} from module {}", deputyId, moduleId, e);
                allRevoked = false;
            }
        }

        if (allRevoked) {
            deputyStorage.delete(deputyId, session);
        } else {
            if (removedModuleIds.isEmpty()) {
                // All failed...
                throw DeputyExceptionCode.REVOKE_FAILED.create(deputyId);
            }
            Set<String> maintainedModuleIds = new LinkedHashSet<String>(moduleIds);
            maintainedModuleIds.removeAll(removedModuleIds);
            if (maintainedModuleIds.isEmpty()) {
                deputyStorage.delete(deputyId, session);
            } else {
                deputyStorage.update(deputyId, deputyInfo.isSendOnBehalfOf(), maintainedModuleIds, session);
            }
        }
    }

    @Override
    public boolean existsDeputyPermission(String deputyId, int contextId) throws OXException {
        return deputyStorage.exists(deputyId, contextId);
    }

    @Override
    public ActiveDeputyPermission getDeputyPermission(String deputyId, Session session) throws OXException {
        // Test existence & validity
        DeputyInfo deputyInfo = deputyStorage.get(deputyId, session);
        ActiveDeputyPermission deputyPermission = loadDeputyPermission(deputyInfo, session);
        if (deputyPermission == null) {
            deleteFromStorageSafe(deputyId, session);
            throw DeputyExceptionCode.NO_SUCH_DEPUTY.create(deputyId);
        }
        return deputyPermission;
    }

    private ActiveDeputyPermission loadDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException {
        List<String> moduleIds = deputyInfo.getModuleIds();
        List<ModulePermission> modulePermissions = null;
        for (String moduleId : moduleIds) {
            Optional<ModulePermission> optionalModulePermission = requireProviderFor(moduleId, true, null, session).getDeputyPermission(deputyInfo, session);
            if (optionalModulePermission.isPresent()) {
                if (modulePermissions == null) {
                    modulePermissions = new ArrayList<ModulePermission>(moduleIds.size());
                }
                modulePermissions.add(optionalModulePermission.get());
            }
        }

        if (modulePermissions == null) {
            return null;
        }

        return DefaultActiveDeputyPermission.builder()
            .withUserId(deputyInfo.getUserId())
            .withDeputyId(deputyInfo.getDeputyId())
            .withEntityId(deputyInfo.getEntityId())
            .withGroup(deputyInfo.isGroup())
            .withSendOnBehalfOf(deputyInfo.isSendOnBehalfOf())
            .withModulePermissions(modulePermissions)
            .build();
    }

    @Override
    public List<ActiveDeputyPermission> listDeputyPermissions(Session session) throws OXException {
        List<DeputyInfo> deputyInfos = deputyStorage.list(session);
        if (deputyInfos.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiveDeputyPermission> deputyPermissions = new ArrayList<ActiveDeputyPermission>(deputyInfos.size());
        for (DeputyInfo deputyInfo : deputyInfos) {
            ActiveDeputyPermission deputyPermission = loadDeputyPermission(deputyInfo, session);
            if (deputyPermission != null) {
                deputyPermissions.add(deputyPermission);
            } else {
                deleteFromStorageSafe(deputyInfo.getDeputyId(), session);
            }
        }
        return deputyPermissions;
    }

    @Override
    public GrantedDeputyPermissions listReverseDeputyPermissions(Session session) throws OXException {
        Map<Integer, List<DeputyInfo>> deputyInfos = deputyStorage.listReverse(session);
        if (deputyInfos.isEmpty()) {
            return DefaultGrantedDeputyPermissions.builder().build();
        }

        DefaultGrantedDeputyPermissions.Builder deputyPermissions = DefaultGrantedDeputyPermissions.builder(deputyInfos.size());
        for (Map.Entry<Integer, List<DeputyInfo>> grantee2deputyInfo : deputyInfos.entrySet()) {
            Integer granteeId = grantee2deputyInfo.getKey();
            List<DeputyInfo> infos = grantee2deputyInfo.getValue();

            List<ActiveDeputyPermission> permissions = new ArrayList<ActiveDeputyPermission>(infos.size());
            for (DeputyInfo deputyInfo : infos) {
                ActiveDeputyPermission deputyPermission = loadDeputyPermission(deputyInfo, session);
                if (deputyPermission != null) {
                    permissions.add(deputyPermission);
                }
            }
            if (!permissions.isEmpty()) {
                Grantee grantee = DefaultGrantee.builder()
                    .withUserId(granteeId.intValue())
                    .withAliases(getAliases(granteeId.intValue(), session.getContextId()))
                    .build();
                deputyPermissions.addEntry(grantee, permissions);
            }
        }
        return deputyPermissions.build();
    }

    @Override
    public List<ActiveDeputyPermission> listReverseDeputyPermissions(int granteeId, Session session) throws OXException {
        List<DeputyInfo> deputyInfos = deputyStorage.listReverse(granteeId, session);
        if (deputyInfos.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActiveDeputyPermission> permissions = new ArrayList<ActiveDeputyPermission>(deputyInfos.size());
        for (DeputyInfo deputyInfo : deputyInfos) {
            ActiveDeputyPermission deputyPermission = loadDeputyPermission(deputyInfo, session);
            if (deputyPermission != null) {
                permissions.add(deputyPermission);
            }
        }
        return permissions;
    }

    private Set<String> getAliases(int userId, int contextId) throws OXException {
        ContextService contextService = services.getServiceSafe(ContextService.class);
        Context context = contextService.getContext(contextId, UpdateBehavior.DENY_UPDATE);

        // Prefer grantee's default sender address
        UserSettingMailStorage settingMailStorage = UserSettingMailStorage.getInstance();
        Optional<String> optSenderAddress = settingMailStorage.getSenderAddress(userId, context, null);
        if (optSenderAddress.isPresent()) {
            return Collections.singleton(optSenderAddress.get());
        }

        // Otherwise fall-back to user's aliases
        UserService userService = services.getServiceSafe(UserService.class);
        MailAccount mailAccount = services.getServiceSafe(MailAccountStorageService.class).getDefaultMailAccount(userId, contextId);

        Set<String> s = new HashSet<>(4);
        s.add(mailAccount.getPrimaryAddress());

        String[] aliases;
        try {
            aliases = userService.getUser(userId, context).getAliases();
        } catch (OXException e) {
            LOG.warn("", e);
            aliases = getEmptyStrings();
        }
        for (final String alias : aliases) {
            s.add(alias);
        }
        return s;
    }

}
