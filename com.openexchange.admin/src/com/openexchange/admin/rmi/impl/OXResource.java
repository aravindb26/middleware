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

package com.openexchange.admin.rmi.impl;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.plugins.OXResourcePluginInterface;
import com.openexchange.admin.plugins.PluginException;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.properties.PropertyScope;
import com.openexchange.admin.rmi.OXResourceInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.dataobjects.ResourcePermission;
import com.openexchange.admin.rmi.exceptions.AbstractAdminRmiException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.EnforceableDataObjectException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchObjectException;
import com.openexchange.admin.rmi.exceptions.NoSuchResourceException;
import com.openexchange.admin.rmi.exceptions.RemoteExceptionUtils;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.services.AdminServiceRegistry;
import com.openexchange.admin.services.PluginInterfaces;
import com.openexchange.admin.storage.interfaces.OXResourceStorageInterface;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.admin.tools.GenericChecks;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheService;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.LogLevel;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Pair;
import com.openexchange.resource.SchedulingPrivilege;
import com.openexchange.resource.storage.ResourceStorage;

public class OXResource extends OXCommonImpl implements OXResourceInterface {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OXResource.class);

    /** The identifier of the virtual group 'all users' - com.openexchange.group.GroupStorage.GROUP_ZERO_IDENTIFIER */
    private static final int GROUP_ALL_USERS = 0;
    private static final ResourcePermission DEFAULT_PERMISSION = new ResourcePermission(I(0), B(true), SchedulingPrivilege.BOOK_DIRECTLY.name());

    // ---------------------------------------------------------------------------------------------------------- //

    private final BasicAuthenticator basicauth;
    private final OXResourceStorageInterface oxRes;
    private final AdminCache cache;
    private final PropertyHandler prop;

    public OXResource() throws RemoteException, StorageException {
        super();
        try {
            oxRes = OXResourceStorageInterface.getInstance();
        } catch (StorageException e) {
            log(LogLevel.ERROR, LOGGER, null, e, null);
            throw new RemoteException(e.getMessage());
        }
        cache = ClientAdminThread.cache;
        prop = cache.getProperties();
        log(LogLevel.INFO, LOGGER, null, null, "Class loaded: {}", this.getClass().getName());
        basicauth = BasicAuthenticator.createPluginAwareAuthenticator();
    }

    private void logAndEnhanceException(Throwable t, final Credentials credentials, final Context ctx, final Resource res) {
        logAndEnhanceException(t, credentials, null != ctx ? ctx.getIdAsString() : null, null != res ? String.valueOf(res.getId()) : null);
    }

    private void logAndEnhanceException(Throwable t, final Credentials credentials, final String contextId, String resourceId) {
        if (t instanceof AbstractAdminRmiException) {
            logAndReturnException(LOGGER, ((AbstractAdminRmiException) t), credentials, contextId, resourceId);
        } else if (t instanceof RemoteException remoteException) {
            String exceptionId = AbstractAdminRmiException.generateExceptionId();
            RemoteExceptionUtils.enhanceRemoteException(remoteException, exceptionId);
            logAndReturnException(LOGGER, remoteException, exceptionId, credentials, contextId, resourceId);
        } else if (t instanceof Exception) {
            RemoteException remoteException = RemoteExceptionUtils.convertException((Exception) t);
            String exceptionId = AbstractAdminRmiException.generateExceptionId();
            RemoteExceptionUtils.enhanceRemoteException(remoteException, exceptionId);
            logAndReturnException(LOGGER, remoteException, exceptionId, credentials, contextId, resourceId);
        }
    }

    @Override
    public void change(final Context ctx, final Resource res, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException {
        try {
            Credentials auth = credentials == null ? new Credentials("", "") : credentials;
            try {
                doNullCheck(res);
            } catch (InvalidDataException e3) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for change is null");
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, "");
                throw invalidDataException;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "{} - {} - {}", ctx, res, auth);

            try {
                setIdOrGetIDFromNameAndIdObject(ctx, res);
            } catch (NoSuchObjectException e) {
                throw new NoSuchResourceException(e);
            }

            res.testMandatoryCreateFieldsNull();

            final int resource_ID = res.getId().intValue();

            checkContextAndSchema(ctx);

            if (!tool.existsResource(ctx, resource_ID)) {
                throw new NoSuchResourceException("Resource with this id does not exists");
            }

            if (res.getName() != null && tool.existsResourceName(ctx, res)) {
                throw new InvalidDataException("Resource " + res.getName() + " already exists in this context");
            }

            if (res.getEmail() != null && tool.existsResourceAddress(ctx, res.getEmail(), res.getId())) {
                throw new InvalidDataException("Resource with this email address already exists");
            }

            tool.primaryMailExists(ctx, res.getEmail());

            if ((null != res.getName()) && prop.getResourceProp(AdminProperties.Resource.AUTO_LOWERCASE, true)) {
                final String rid = res.getName().toLowerCase();
                res.setName(rid);
            }

            if ((null != res.getName()) && prop.getResourceProp(AdminProperties.Resource.CHECK_NOT_ALLOWED_CHARS, true)) {
                validateResourceName(res.getName());
            }

            final String resmail = res.getEmail();
            if (resmail != null && resmail.trim().length() > 0 && (!GenericChecks.isValidMailAddress(resmail) || !GenericChecks.isValidMailAddress(resmail, PropertyScope.propertyScopeForContext(ctx.getId().intValue())))) {
                throw new InvalidDataException("Invalid email address");
            }

            if (res.isPermissionsSet()) {
                validatePermissionPrivileges(ctx, res);
            }

            oxRes.change(ctx, res);

            // Invalidate jcs cache
            invalidateCache(ctx, res, auth, false);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXResourcePluginInterface oxresource : pluginInterfaces.getResourcePlugins().getServiceList()) {
                        final String bundlename = oxresource.getClass().getName();
                        try {
                            log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(resource_ID), null, "Calling change for plugin: {}", bundlename);
                            oxresource.change(ctx, res, auth);
                        } catch (PluginException e) {
                            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(resource_ID), e, "Error while calling change for plugin: {}", bundlename);
                            throw StorageException.wrapForRMI(e);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, res);
            throw e;
        }
    }

    @Override
    public Resource create(final Context ctx, final Resource res, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            Credentials auth = credentials == null ? new Credentials("", "") : credentials;
            try {
                doNullCheck(res);
                doNullCheck(res.getName());
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for create is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "{} - {} - {}", ctx, res, auth);

            checkContextAndSchema(ctx);

            if (tool.existsResourceName(ctx, res.getName())) {
                throw new InvalidDataException("Resource " + res.getName() + " already exists in this context");
            }

            if (res.getEmail() != null && tool.existsResourceAddress(ctx, res.getEmail())) {
                throw new InvalidDataException("Resource with this email address already exists");
            }

            tool.primaryMailExists(ctx, res.getEmail());

            if (!res.mandatoryCreateMembersSet()) {
                throw new InvalidDataException("Mandatory fields not set: " + res.getUnsetMembers());
                // TODO: cutmasta look here
            }

            if (prop.getResourceProp(AdminProperties.Resource.AUTO_LOWERCASE, true)) {
                final String uid = res.getName().toLowerCase();
                res.setName(uid);
            }

            if (prop.getResourceProp(AdminProperties.Resource.CHECK_NOT_ALLOWED_CHARS, true)) {
                validateResourceName(res.getName());
            }

            final String resmail = res.getEmail();
            if (resmail != null && (!GenericChecks.isValidMailAddress(resmail)) || !GenericChecks.isValidMailAddress(resmail, PropertyScope.propertyScopeForContext(ctx.getId().intValue()))) {
                throw new InvalidDataException("Invalid email address");
            }

            if (res.isPermissionsSet()) {
                validatePermissionPrivileges(ctx, res);
            }

            final int retval = oxRes.create(ctx, res);
            res.setId(I(retval));

            invalidateCache(ctx, res, credentials, true);

            final ArrayList<OXResourcePluginInterface> interfacelist = new ArrayList<>();

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXResourcePluginInterface oxresource : pluginInterfaces.getResourcePlugins().getServiceList()) {
                        final String bundlename = oxresource.getClass().getName();
                        try {
                            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "Calling create for plugin: {}", bundlename);
                            oxresource.create(ctx, res, auth);
                            interfacelist.add(oxresource);
                        } catch (PluginException e) {
                            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), e, "Error while calling create for plugin: {}", bundlename);
                            log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "Now doing rollback for everything until now...");
                            for (final OXResourcePluginInterface oxresourceinterface : interfacelist) {
                                try {
                                    oxresourceinterface.delete(ctx, res, auth);
                                } catch (PluginException e1) {
                                    log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), e1, "Error doing rollback for plugin: {}", bundlename);
                                }
                            }
                            try {
                                oxRes.delete(ctx, res);
                            } catch (StorageException e1) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), e1, "Error doing rollback for creating resource in database");
                            }
                            throw StorageException.wrapForRMI(e);
                        }
                    }
                }
            }

            return res;
        } catch (EnforceableDataObjectException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), credentials, ctx.getIdAsString(), String.valueOf(res.getId()));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, res);
            throw e;
        }
    }

    @Override
    public void delete(final Context ctx, final Resource res, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException {
        try {
            Credentials auth = credentials == null ? new Credentials("", "") : credentials;
            try {
                doNullCheck(res);
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for delete is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);

            try {
                setIdOrGetIDFromNameAndIdObject(ctx, res);
            } catch (NoSuchObjectException e) {
                throw new NoSuchResourceException(e);
            }
            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "{} - {} - {}", ctx, res, auth);
            checkContextAndSchema(ctx);
            if (!tool.existsResource(ctx, res.getId().intValue())) {
                throw new NoSuchResourceException("Resource with this id does not exist");
            }
            final ArrayList<OXResourcePluginInterface> interfacelist = new ArrayList<>();

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXResourcePluginInterface oxresource : pluginInterfaces.getResourcePlugins().getServiceList()) {
                        final String bundlename = oxresource.getClass().getName();
                        try {
                            log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "Calling delete for plugin: {}", bundlename);
                            oxresource.delete(ctx, res, auth);
                            interfacelist.add(oxresource);
                        } catch (PluginException e) {
                            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), e, "Error while calling delete for plugin: {}", bundlename);
                            throw StorageException.wrapForRMI(e);
                        }
                    }
                }
            }

            oxRes.delete(ctx, res);
            invalidateCache(ctx, res, credentials, true);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, res);
            throw e;
        }
    }

    @Override
    public Resource getData(final Context ctx, final Resource res, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException {
        try {
            Credentials auth = credentials == null ? new Credentials("", "") : credentials;
            try {
                doNullCheck(res);
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for get is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);

            try {
                setIdOrGetIDFromNameAndIdObject(ctx, res);
            } catch (NoSuchObjectException e) {
                throw new NoSuchResourceException(e);
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "{} - {} - {}", ctx, res.getId(), auth);

            checkContextAndSchema(ctx);

            final int resource_id = res.getId().intValue();
            if (!tool.existsResource(ctx, resource_id)) {
                throw new NoSuchResourceException("resource with with this id does not exist");
            }

            Resource retres;
            retres = oxRes.getData(ctx, res);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXResourcePluginInterface oxresource : pluginInterfaces.getResourcePlugins().getServiceList()) {
                        final String bundlename = oxresource.getClass().getName();
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), null, "Calling getData for plugin: {}", bundlename);
                        retres = oxresource.get(ctx, retres, auth);
                    }
                }
            }

            return retres;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, res);
            throw e;
        }
    }

    @Override
    public Resource[] getData(final Context ctx, final Resource[] resources, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchResourceException, DatabaseUpdateException {
        try {
            Credentials auth = credentials == null ? new Credentials("", "") : credentials;
            try {
                doNullCheck((Object[]) resources);
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for getData is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(resources), null, "{} - {} - {}", ctx, Arrays.toString(resources), auth);

            checkContextAndSchema(ctx);

            // check if all resources exists
            for (final Resource resource : resources) {
                if (resource.getId() != null && !tool.existsResource(ctx, resource.getId().intValue())) {
                    throw new NoSuchResourceException("No such resource " + resource.getId().intValue());
                }
                if (resource.getName() != null && !tool.existsResourceName(ctx, resource.getName())) {
                    throw new NoSuchResourceException("No such resource " + resource.getName());
                }
                if (resource.getName() == null && resource.getId() == null) {
                    throw new InvalidDataException("Resourcename and resourceid missing!Cannot resolve resource data");
                }

                if (resource.getName() == null) {
                    // resolv name by id
                    resource.setName(tool.getResourcenameByResourceID(ctx, resource.getId().intValue()));
                }
                if (resource.getId() == null) {
                    resource.setId(I(tool.getResourceIDByResourcename(ctx, resource.getName())));
                }
            }
            final ArrayList<Resource> retval = new ArrayList<>();
            for (final Resource resource : resources) {
                // not nice, but works ;)
                final Resource tmp = oxRes.getData(ctx, resource);
                retval.add(tmp);
            }

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXResourcePluginInterface oxresource : pluginInterfaces.getResourcePlugins().getServiceList()) {
                        final String bundlename = oxresource.getClass().getName();
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(resources), null, "Calling get for plugin: {}", bundlename);
                        for (Resource resource : retval) {
                            resource = oxresource.get(ctx, resource, auth);
                        }
                    }
                }
            }
            return retval.toArray(new Resource[retval.size()]);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx.getIdAsString(), getObjectIds(resources));
            throw e;
        }
    }

    @Override
    public Resource[] list(final Context ctx, final String pattern, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            Credentials auth = credentials == null ? new Credentials("", "") : credentials;
            try {
                doNullCheck(pattern);
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for list is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            try {
                if (pattern.length() == 0) {
                    throw new InvalidDataException("Invalid pattern!");
                }

                basicauth.doAuthentication(auth, ctx);
            } catch (InvalidDataException e) {
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), e, "");
                throw e;
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), null, "{} - {} - {}", ctx, pattern, auth);

            checkContextAndSchema(ctx);

            return oxRes.list(ctx, pattern);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, null);
            throw e;
        }
    }

    @Override
    public Resource[] listAll(final Context ctx, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        return list(ctx, "*", auth);
    }

    private void validateResourceName(final String resName) throws InvalidDataException {
        if (resName == null || resName.trim().length() == 0) {
            throw new InvalidDataException("Invalid resource name");
        }
        // Check for allowed chars:
        // abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 _-+.%$@
        final String resource_check_regexp = prop.getResourceProp("CHECK_RES_UID_REGEXP", "[ $@%\\.+a-zA-Z0-9_-]");
        final String illegal = resName.replaceAll(resource_check_regexp, "");
        if (illegal.length() > 0) {
            throw new InvalidDataException("Illegal chars: \"" + illegal + "\"");
        }
    }

    /**
     * Checks permission privilege strings in a resource to be valid. In case only a single {@link #DEFAULT_PERMISSION} is left, the
     * permissions are removed (via {@link Resource#DELETE_PERMISSIONS}) implicitly, so that the resource is effectively transformed back
     * to a legacy/unmanaged resource.
     *
     * @param ctx The context
     * @param resource The resource to validate
     * @throws InvalidDataException On invalid permission privilege string
     */
    private void validatePermissionPrivileges(Context ctx, Resource resource) throws InvalidDataException, StorageException {
        List<ResourcePermission> permissions = resource.getPermissions();

        // No permissions
        if (null == permissions || permissions.isEmpty()) {
            return;
        }

        if (1 == permissions.size()) {
            if (permissions.get(0).equals(DEFAULT_PERMISSION)) {
                // Only default permission, transform to legacy/unmangaed resource
                resource.setPermissions(Collections.singletonList(Resource.DELETE_PERMISSIONS));
                return;
            }
            if (permissions.get(0).equals(Resource.DELETE_PERMISSIONS)) {
                return; // Indicator to delete existing permissions
            }
        }

        Map<SchedulingPrivilege, List<ResourcePermission>> permissionsByPrivilege = new HashMap<>();
        Set<Pair<Integer, Boolean>> usedEntities = new HashSet<>();
        for (ResourcePermission perm : permissions) {
            Integer entity = perm.entity();
            String privilege = perm.privilege();
            Boolean group = perm.group();

            if (null == entity) {
                throw new InvalidDataException("Permission without entity found.");
            }
            if (null == group) {
                throw new InvalidDataException("Permission without group flag found.");
            }
            if (Strings.isEmpty(privilege)) {
                throw new InvalidDataException(String.format("Missing permission privilege for entity %d.", entity));
            }

            if (false == usedEntities.add(new Pair<>(entity, group))) {
                throw new InvalidDataException(String.format("Duplicate permission entity %d.", entity));
            }

            if (b(group)) {
                if (GROUP_ALL_USERS != i(entity) && false == tool.existsGroup(ctx, i(entity))) {
                    throw new InvalidDataException(String.format("Group with id %d not found.", entity));
                }
            } else {
                if (false == tool.existsUser(ctx, i(entity))) {
                    throw new InvalidDataException(String.format("User with id %d not found.", entity));
                }
                if (tool.isGuestUser(ctx, i(entity))) {
                    throw new InvalidDataException(String.format("User %d is a guest user.", entity));
                }
            }

            try {
                SchedulingPrivilege schedulingPrivilege = SchedulingPrivilege.valueOf(privilege.toUpperCase());
                com.openexchange.tools.arrays.Collections.put(permissionsByPrivilege, schedulingPrivilege, perm);
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException(String.format("Invalid permission privilege %s for entity %d.", privilege, entity), e);
            }
        }
        /*
         * if there is an 'ask_to_book' privilege, ensure that there's at least one 'delegate' defined as well
         */
        if (permissionsByPrivilege.containsKey(SchedulingPrivilege.ASK_TO_BOOK) && false == permissionsByPrivilege.containsKey(SchedulingPrivilege.DELEGATE)) {
            throw new InvalidDataException(String.format("Permission %s without %s found.", SchedulingPrivilege.ASK_TO_BOOK.name(), SchedulingPrivilege.DELEGATE.name()));
        }

        if (isSimplePermissionMode(ctx)) {
            List<ResourcePermission> delegatePermissions = permissionsByPrivilege.get(SchedulingPrivilege.DELEGATE);
            if (null == delegatePermissions || 0 == delegatePermissions.size()) {
                /*
                 * no 'delegate' defined, so only 'book_directly' for group 0 is possible
                 */
                if (1 == permissions.size() && false == DEFAULT_PERMISSION.equals(permissions.get(0))) {
                    throw new InvalidDataException("Only 'book_directly' for group 0 allowed without 'delegate' in simple permission mode");
                }
            } else {
                /*
                 * at least one 'delegate' defined, so only a remaining 'ask_to_book' for group 0 is possible
                 */
                if (permissionsByPrivilege.containsKey(SchedulingPrivilege.BOOK_DIRECTLY) || permissionsByPrivilege.containsKey(SchedulingPrivilege.NONE)) {
                    throw new InvalidDataException("Only 'ask_to_book' for group 0 allowed with 'delegate' in simple permission mode");
                }
                List<ResourcePermission> askToBookPermissions = permissionsByPrivilege.get(SchedulingPrivilege.ASK_TO_BOOK);
                if (null == askToBookPermissions || 1 != askToBookPermissions.size() ||
                    false == new ResourcePermission(I(GROUP_ALL_USERS), Boolean.TRUE, SchedulingPrivilege.ASK_TO_BOOK.name()).equals(askToBookPermissions.get(0))) {
                    throw new InvalidDataException("Only 'ask_to_book' for group 0 allowed with 'delegate' in simple permission mode");
                }
            }
        }
    }

    /**
     * Checks if simple managed resource mode is set for context
     *
     * @param ctx The context
     * @return <code>true</code> if simple mode is set, <code>false</code> otherwise
     * @throws StorageException If a problem occurs on the storage layer
     */
    private boolean isSimplePermissionMode(Context ctx) throws StorageException {
        ConfigViewFactory configViewFactory = AdminServiceRegistry.getInstance().getService(ConfigViewFactory.class);
        if (null != configViewFactory) {
            try {
                ConfigView view = configViewFactory.getView(-1, i(ctx.getId()));
                return b(view.property("com.openexchange.resource.simplePermissionMode", Boolean.class).get());
            } catch (OXException e) {
                throw new StorageException(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Invalidates the jcs resource cache for the given resource
     *
     * @param ctx The context
     * @param res The resource
     * @param credentials The crendentials
     * @param invalidateAllEntry Whether to invalidate the all entry or not
     */
    private void invalidateCache(Context ctx, Resource res, Credentials credentials, boolean invalidateAllEntry) {
        final CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
        if (cacheService != null) {
            try {
                Cache cache = cacheService.getCache(ResourceStorage.CACHE_REGION_NAME);
                cache.remove(cacheService.newCacheKey(ctx.getId().intValue(), res.getId().intValue()));
                if (invalidateAllEntry) {
                    cache.remove(cacheService.newCacheKey(ctx.getId().intValue(), ResourceStorage.SEARCH_PATTERN_ALL));
                }
            } catch (OXException e) {
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(res.getId()), e, "");
            }
        }
    }

}
