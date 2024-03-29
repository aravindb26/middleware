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

import static com.openexchange.admin.rmi.exceptions.RemoteExceptionUtils.convertException;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.plugins.OXUserPluginInterface;
import com.openexchange.admin.plugins.OXUserPluginInterfaceExtended;
import com.openexchange.admin.plugins.PluginException;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.properties.PropertyScope;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.dataobjects.UserProperty;
import com.openexchange.admin.rmi.exceptions.AbstractAdminRmiException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.EnforceableDataObjectException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchFilestoreException;
import com.openexchange.admin.rmi.exceptions.NoSuchObjectException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.ProgrammErrorException;
import com.openexchange.admin.rmi.exceptions.RemoteExceptionUtils;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.exceptions.StorageRuntimeException;
import com.openexchange.admin.rmi.impl.util.OXUserPropertySorter;
import com.openexchange.admin.services.AdminServiceRegistry;
import com.openexchange.admin.services.PluginInterfaces;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.storage.interfaces.OXUserStorageInterface;
import com.openexchange.admin.storage.interfaces.OXUtilStorageInterface;
import com.openexchange.admin.storage.utils.Filestore2UserUtil;
import com.openexchange.admin.taskmanagement.TaskManager;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.admin.tools.PropertyHandler;
import com.openexchange.admin.tools.filestore.FilestoreDataMover;
import com.openexchange.admin.tools.filestore.PostProcessTask;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.ConfigurationProperty;
import com.openexchange.config.cascade.ConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViewScope;
import com.openexchange.exception.LogLevel;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorages;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.java.Strings;
import com.openexchange.password.mechanism.PasswordDetails;
import com.openexchange.password.mechanism.PasswordMech;
import com.openexchange.password.mechanism.PasswordMechRegistry;

/**
 * @author d7
 * @author cutmasta
 */
public class OXUser extends OXCommonImpl implements OXUserInterface {

    private static final String EMPTY_STRING = "";

    /** The logger */
    static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OXUser.class);

    // ------------------------------------------------------------------------------------------------ //

    private final OXUserStorageInterface oxu;
    private final BasicAuthenticator basicauth;
    private final AdminCache cache;
    private final PropertyHandler prop;
    private final boolean allowChangingQuotaIfNoFileStorageSet;

    /**
     * Initializes a new {@link OXUser}.
     *
     * @throws StorageException If initialization fails
     */
    public OXUser() throws StorageException {
        super();
        this.cache = ClientAdminThread.cache;
        this.prop = this.cache.getProperties();
        allowChangingQuotaIfNoFileStorageSet = Boolean.parseBoolean(prop.getUserProp("ALLOW_CHANGING_QUOTA_IF_NO_FILESTORE_SET", "false").trim());
        log(LogLevel.INFO, LOGGER, null, null, "Class loaded: {}", this.getClass().getName());
        basicauth = BasicAuthenticator.createPluginAwareAuthenticator();
        try {
            oxu = OXUserStorageInterface.getInstance();
        } catch (StorageException e) {
            log(LogLevel.ERROR, LOGGER, null, e, EMPTY_STRING);
            throw e;
        }
    }

    private void logAndEnhanceException(Throwable t, final Credentials credentials, final Context ctx, final User usr) {
        logAndEnhanceException(t, credentials, null != ctx ? ctx.getIdAsString() : null, null != usr ? String.valueOf(usr.getId()) : null);
    }

    private void logAndEnhanceException(Throwable t, final Credentials credentials, final String contextId, String userId) {
        if (t instanceof AbstractAdminRmiException x) {
            logAndReturnException(LOGGER, x, credentials, contextId, userId);
        } else if (t instanceof RemoteException remoteException) {
            String exceptionId = AbstractAdminRmiException.generateExceptionId();
            RemoteExceptionUtils.enhanceRemoteException(remoteException, exceptionId);
            logAndReturnException(LOGGER, remoteException, exceptionId, credentials, contextId, userId);
        } else if (t instanceof Exception e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            String exceptionId = AbstractAdminRmiException.generateExceptionId();
            RemoteExceptionUtils.enhanceRemoteException(remoteException, exceptionId);
            logAndReturnException(LOGGER, remoteException, exceptionId, credentials, contextId, userId);
        }
    }

    @Override
    public Set<String> getCapabilities(final Context ctx, final User user, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            if (null == ctx) {
                throw new InvalidDataException("Missing context.");
            }
            if (null == user) {
                throw new InvalidDataException("Missing user.");
            }
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }
            return oxu.getCapabilities(ctx, user);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public void changeMailAddressPersonal(Context ctx, User user, String personal, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            // Change personal
            oxu.changeMailAddressPersonal(ctx, user, personal);

            // Check for context administrator
            final boolean isContextAdmin = tool.isContextAdmin(ctx, user.getId().intValue());

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if (oxuser.canHandleContextAdmin() || (!oxuser.canHandleContextAdmin() && !isContextAdmin)) {
                            try {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null, "Calling changeMailAddressPersonal for plugin: {}", oxuser.getClass().getName());
                                oxuser.changeMailAddressPersonal(ctx, user, personal, auth);
                            } catch (PluginException | RuntimeException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Error while calling changeMailAddressPersonal for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public void changeCapabilities(final Context ctx, final User user, final Set<String> capsToAdd, final Set<String> capsToRemove, final Set<String> capsToDrop, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            if ((null == capsToAdd || capsToAdd.isEmpty()) && (null == capsToRemove || capsToRemove.isEmpty()) && (null == capsToDrop || capsToDrop.isEmpty())) {
                throw new InvalidDataException("No capabilities specified.");
            }
            if (null == ctx) {
                throw new InvalidDataException("Missing context.");
            }
            if (null == user) {
                throw new InvalidDataException("Missing user.");
            }
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null, "{} - {} - {} | {} - {}", ctx, user, (null == capsToAdd ? EMPTY_STRING : capsToAdd.toString()), (null == capsToRemove ? EMPTY_STRING : capsToRemove.toString()), auth);

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            // Change capabilities
            checkCapabilities(Optional.ofNullable(capsToAdd), Optional.ofNullable(capsToRemove));
            oxu.changeCapabilities(ctx, user, capsToAdd, capsToRemove, capsToDrop, auth);

            // Check for context administrator
            final boolean isContextAdmin = tool.isContextAdmin(ctx, user.getId().intValue());

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if (oxuser.canHandleContextAdmin() || (!oxuser.canHandleContextAdmin() && !isContextAdmin)) {
                            try {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "Calling changeCapabilities for plugin: {}", oxuser.getClass().getName());
                                oxuser.changeCapabilities(ctx, user, capsToAdd, capsToRemove, capsToDrop, auth);
                            } catch (PluginException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), e, "Error while calling change for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            } catch (RuntimeException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), e, "Error while calling change for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public int moveUserFilestore(final Context ctx, User user, Filestore dstFilestore, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for moving file storage data is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }

            final int user_id = user.getId().intValue();
            if (!tool.existsContext(ctx)) {
                throw new NoSuchContextException(ctx.getIdAsString());
            } else if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            } else if (!tool.existsStore(dstFilestore.getId().intValue())) {
                throw new NoSuchFilestoreException();
            }

            final OXUserStorageInterface oxuser = this.oxu;
            User storageUser = oxuser.getData(ctx, new User[] { user })[0];

            // Check equality
            int srcStoreId = storageUser.getFilestoreId().intValue();
            if (srcStoreId <= 0) {
                throw new InvalidDataException("Unable to get filestore " + srcStoreId);
            }
            if (srcStoreId == dstFilestore.getId().intValue()) {
                throw new InvalidDataException("The identifiers for the source and destination storage are equal: " + dstFilestore);
            }

            // Check storage name
            String name = storageUser.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for user " + user_id + " in " + ctx.getIdAsString());
            }

            // Check capacity
            OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
            Filestore loadedDstFilestore = oxu.getFilestore(dstFilestore.getId().intValue(), false);
            if (!oxu.hasSpaceForAnotherUser(loadedDstFilestore, true)) {
                throw new StorageException("Destination filestore does not have enough space for another user.");
            }

            // Load it to ensure validity
            String baseUri = loadedDstFilestore.getUrl();
            try {
                URI uri = FileStorages.getFullyQualifyingUriForContext(ctx.getId().intValue(), new java.net.URI(baseUri));
                FileStorages.getFileStorageService().getFileStorage(uri);
            } catch (OXException e) {
                throw StorageException.wrapForRMI(e);
            } catch (URISyntaxException e) {
                throw new StorageException("Invalid file storage URI: " + baseUri, e);
            }

            // Initialize mover instance
            FilestoreDataMover fsdm = FilestoreDataMover.newUserMover(oxu.getFilestore(srcStoreId, false), loadedDstFilestore, storageUser, ctx);

            // Enable user after processing
            fsdm.addPostProcessTask(new PostProcessTask() {

                @Override
                public void perform(ExecutionException executionError) throws StorageException {
                    if (null == executionError) {
                        oxuser.enableUser(user_id, ctx);
                    } else {
                        log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "An execution error occurred during \"moveuserfilestore\" for user {} in context {}. User will stay disabled.", I(user_id), ctx.getId(), executionError.getCause());
                    }
                }
            });

            oxuser.disableUser(user_id, ctx);

            // Schedule task
            return TaskManager.getInstance().addJob(fsdm, "moveuserfilestore", "move user " + user_id + " from context " + ctx.getIdAsString() + " to another filestore " + dstFilestore.getId(), ctx.getId().intValue());
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public int moveFromUserFilestoreToMaster(final Context ctx, User user, User masterUser, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        return moveFromUserFilestoreToMaster(ctx, user, masterUser, credentials, false);
    }

    private int moveFromUserFilestoreToMaster(final Context ctx, User user, User masterUser, Credentials credentials, boolean inline) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for moving file storage data is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            final int userId = user.getId().intValue();
            if (false == inline) {
                basicauth.doAuthentication(auth, ctx);
                checkContextAndSchema(ctx);
                try {
                    setIdOrGetIDFromNameAndIdObject(ctx, user);
                } catch (NoSuchObjectException e) {
                    throw new NoSuchUserException(e);
                }
                try {
                    setIdOrGetIDFromNameAndIdObject(ctx, masterUser);
                } catch (NoSuchObjectException e) {
                    throw new NoSuchUserException(e);
                }

                if (!tool.existsUser(ctx, userId)) {
                    throw new NoSuchUserException("No such user " + userId + " in context " + ctx.getId());
                }
            }

            final OXUserStorageInterface oxuser = this.oxu;
            if (userId == masterUser.getId().intValue()) {
                throw new StorageException("User and master user identifiers are equal.");
            }

            User[] data = oxuser.getData(ctx, new User[] { user, masterUser });
            User storageUser = data[0];
            User storageMasterUser = data[1];

            if (null == storageMasterUser.getFilestoreId() || storageMasterUser.getFilestoreId().intValue() <= 0) {
                throw new StorageException("Master user " + storageMasterUser.getId() + " has no file storage set.");
            }
            if (null == storageUser.getFilestoreId() || storageUser.getFilestoreId().intValue() <= 0) {
                throw new StorageException("User " + storageUser.getId() + " has no file storage set.");
            }
            if (storageMasterUser.getFilestoreId().intValue() == storageUser.getFilestoreId().intValue()) {
                String masterFsName = storageMasterUser.getFilestore_name();
                if (null == masterFsName) {
                    throw new InvalidDataException("Unable to get filestore directory for master user " + masterUser.getId() + " in " + ctx.getIdAsString());
                }
                String userFsName = storageUser.getFilestore_name();
                if (null == userFsName) {
                    throw new InvalidDataException("Unable to get filestore directory for user " + userId + " in " + ctx.getIdAsString());
                }
                if (masterFsName.equals(userFsName)) {
                    throw new StorageException("User " + storageUser.getId() + " already has a master file storage set.");
                }
            }

            if (!tool.existsStore(storageMasterUser.getFilestoreId().intValue())) {
                throw new NoSuchFilestoreException();
            }
            boolean equal = storageMasterUser.getFilestoreId().intValue() == storageUser.getFilestoreId().intValue();

            if (!equal && !tool.existsStore(storageUser.getFilestoreId().intValue())) {
                throw new NoSuchFilestoreException();
            }

            OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
            Filestore destFilestore = oxu.getFilestore(storageMasterUser.getFilestoreId().intValue(), false);
            Filestore srcFilestore = equal ? destFilestore : oxu.getFilestoreBasic(storageUser.getFilestoreId().intValue());

            // Check equality
            int srcStoreId = storageUser.getFilestoreId().intValue();
            if (srcStoreId <= 0) {
                throw new InvalidDataException("Unable to get filestore " + srcStoreId);
            }

            // Check storage name
            String name = storageUser.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for user " + userId + " in " + ctx.getIdAsString());
            }
            name = storageMasterUser.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for user " + storageMasterUser.getId() + " in " + ctx.getIdAsString());
            }

            // Check capacity
            if (!equal && !oxu.hasSpaceForAnotherUser(destFilestore, true)) {
                throw new StorageException("Destination filestore does not have enough space for another user.");
            }

            // Initialize mover instance
            FilestoreDataMover fsdm = FilestoreDataMover.newUser2MasterMover(srcFilestore, destFilestore, storageUser, storageMasterUser, ctx);

            // Enable user after processing
            fsdm.addPostProcessTask(new PostProcessTask() {

                @Override
                public void perform(ExecutionException executionError) throws StorageException {
                    if (null == executionError) {
                        oxuser.enableUser(userId, ctx);
                    } else {
                        log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userId), null, "An execution error occurred during \"movefromuserfilestoretomaster\" for user {} in context {}. User will stay disabled.", I(userId), ctx.getId(), executionError.getCause());
                    }
                }
            });

            oxuser.disableUser(userId, ctx);

            if (false == inline) {
                // Schedule task
                return TaskManager.getInstance().addJob(fsdm, "movefromuserfilestoretomaster", "move user " + userId + " from context " + ctx.getIdAsString() + " from individual to master filestore " + destFilestore.getId(), ctx.getId().intValue());
            }

            // Execute with current thread
            fsdm.call();
            return -1;
        } catch (InterruptedException e) {
            // Keep interrupted status
            Thread.currentThread().interrupt();
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (IOException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (ProgrammErrorException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public int moveFromMasterToUserFilestore(final Context ctx, User user, User masterUser, Filestore dstFilestore, long maxQuota, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for moving file storage data is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, masterUser);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }

            final int userId = user.getId().intValue();
            if (!tool.existsUser(ctx, userId)) {
                throw new NoSuchUserException("No such user " + userId + " in context " + ctx.getId());
            }

            final OXUserStorageInterface oxuser = this.oxu;
            if (userId == masterUser.getId().intValue()) {
                throw new StorageException("User and master user identifiers are equal.");
            }

            User[] data = oxuser.getData(ctx, new User[] { user, masterUser });
            User storageUser = data[0];
            User storageMasterUser = data[1];

            if (null == storageMasterUser.getFilestoreId() || storageMasterUser.getFilestoreId().intValue() <= 0) {
                throw new StorageException("Master user " + storageMasterUser.getId() + " has no file storage set.");
            }
            if (null == storageUser.getFilestoreId() || storageUser.getFilestoreId().intValue() <= 0) {
                throw new StorageException("User " + storageUser.getId() + " has no file storage set.");
            }
            if (storageMasterUser.getFilestoreId().intValue() != storageUser.getFilestoreId().intValue()) {
                throw new StorageException("User " + storageUser.getId() + " has no master file storage set.");
            }
            {
                String masterFsName = storageMasterUser.getFilestore_name();
                if (null == masterFsName) {
                    throw new InvalidDataException("Unable to get filestore directory for master user " + masterUser.getId() + " in " + ctx.getIdAsString());
                }
                String userFsName = storageUser.getFilestore_name();
                if (null == userFsName) {
                    throw new InvalidDataException("Unable to get filestore directory for user " + userId + " in " + ctx.getIdAsString());
                }
                if (!masterFsName.equals(userFsName)) {
                    throw new StorageException("User " + storageUser.getId() + " has no master file storage set.");
                }
            }

            if (null == dstFilestore) {
                throw new InvalidDataException("Missing filestore parameter");
            }

            if (!tool.existsStore(dstFilestore.getId().intValue())) {
                throw new NoSuchFilestoreException();
            }
            boolean equal = dstFilestore.getId().intValue() == storageMasterUser.getFilestoreId().intValue();

            if (!equal && !tool.existsStore(storageMasterUser.getFilestoreId().intValue())) {
                throw new NoSuchFilestoreException();
            }

            OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
            Filestore destFilestore = oxu.getFilestore(dstFilestore.getId().intValue(), false);
            Filestore srcFilestore = equal ? destFilestore : oxu.getFilestoreBasic(storageMasterUser.getFilestoreId().intValue());

            // Check equality
            int srcStoreId = storageMasterUser.getFilestoreId().intValue();
            if (srcStoreId <= 0) {
                throw new InvalidDataException("Unable to get filestore " + srcStoreId);
            }

            // Check storage name
            String name = storageUser.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for user " + userId + " in " + ctx.getIdAsString());
            }
            name = storageMasterUser.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for user " + storageMasterUser.getId() + " in " + ctx.getIdAsString());
            }

            // Check capacity
            if (!equal && !oxu.hasSpaceForAnotherUser(destFilestore, true)) {
                throw new StorageException("Destination filestore does not have enough space for another user.");
            }

            // Load it to ensure validity
            String baseUri = destFilestore.getUrl();
            try {
                URI uri = FileStorages.getFullyQualifyingUriForContext(ctx.getId().intValue(), new java.net.URI(baseUri));
                FileStorages.getFileStorageService().getFileStorage(uri);
            } catch (OXException e) {
                throw StorageException.wrapForRMI(e);
            } catch (URISyntaxException e) {
                throw new StorageException("Invalid file storage URI: " + baseUri, e);
            }

            // Initialize mover instance
            FilestoreDataMover fsdm = FilestoreDataMover.newUserFromMasterMover(srcFilestore, destFilestore, maxQuota, storageUser, storageMasterUser, ctx);

            // Enable user after processing
            fsdm.addPostProcessTask(new PostProcessTask() {

                @Override
                public void perform(ExecutionException executionError) throws StorageException {
                    if (null == executionError) {
                        oxuser.enableUser(userId, ctx);
                    } else {
                        log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userId), null, "An execution error occurred during \"movefrommastertouserfilestore\" for user {} in context {}. User will stay disabled.", I(userId), ctx.getId(), executionError.getCause());
                    }
                }
            });

            // Schedule task
            oxuser.disableUser(userId, ctx);
            return TaskManager.getInstance().addJob(fsdm, "movefrommastertouserfilestore", "move user " + userId + " from context " + ctx.getIdAsString() + " from master to individual filestore " + destFilestore.getId(), ctx.getId().intValue());
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public int moveFromContextToUserFilestore(final Context ctx, User user, Filestore dstFilestore, long maxQuota, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        return moveFromContextToUserFilestore(ctx, user, dstFilestore, maxQuota, credentials, false);
    }

    private int moveFromContextToUserFilestore(final Context ctx, User user, Filestore dstFilestore, long maxQuota, Credentials credentials, boolean inline) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for moving file storage data is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }

            if (false == inline) {
                basicauth.doAuthentication(auth, ctx);
                checkContextAndSchema(ctx);
                try {
                    setIdOrGetIDFromNameAndIdObject(ctx, user);
                } catch (NoSuchObjectException e) {
                    throw new NoSuchUserException(e);
                }

                int user_id = user.getId().intValue();
                if (!tool.existsUser(ctx, user_id)) {
                    throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
                }
            }

            final int user_id = user.getId().intValue();
            final OXUserStorageInterface oxuser = this.oxu;
            final OXContextInterface oxctx = AdminServiceRegistry.getInstance().getService(OXContextInterface.class, true);

            Context storageContext = oxctx.getOwnData(ctx, Credentials.alwaysAcceptFor(auth.getLogin()));
            User storageUser = oxuser.getData(ctx, new User[] { user })[0];

            if (null != storageUser.getFilestoreId() && storageUser.getFilestoreId().intValue() > 0) {
                throw new StorageException("User " + storageUser.getId() + " already has a dedicate file storage set.");
            }

            if (null == dstFilestore) {
                throw new InvalidDataException("Missing filestore parameter");
            }

            if (!tool.existsStore(dstFilestore.getId().intValue())) {
                throw new NoSuchFilestoreException();
            }
            boolean equal = dstFilestore.getId().intValue() == storageContext.getFilestoreId().intValue();

            if (!equal && !tool.existsStore(storageContext.getFilestoreId().intValue())) {
                throw new NoSuchFilestoreException();
            }

            OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
            Filestore destFilestore = oxu.getFilestore(dstFilestore.getId().intValue(), false);
            Filestore srcFilestore = equal ? destFilestore : oxu.getFilestoreBasic(storageContext.getFilestoreId().intValue());

            // Check equality
            int srcStoreId = storageContext.getFilestoreId().intValue();
            if (srcStoreId <= 0) {
                throw new InvalidDataException("Unable to get filestore " + srcStoreId);
            }
            ctx.setFilestoreId(Integer.valueOf(srcStoreId));
            if (srcStoreId == destFilestore.getId().intValue()) {
                // Ok when moving from context to user
                //throw new InvalidDataException("The identifiers for the source and destination storage are equal: " + destFilestore);
            }

            // Check storage name
            String name = storageContext.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for context " + ctx.getIdAsString());
            }

            // Check capacity
            if (!equal && !oxu.hasSpaceForAnotherUser(destFilestore, true)) {
                throw new StorageException("Destination filestore does not have enough space for another user.");
            }

            // Load it to ensure validity
            String baseUri = destFilestore.getUrl();
            try {
                URI uri = FileStorages.getFullyQualifyingUriForContext(ctx.getId().intValue(), new java.net.URI(baseUri));
                FileStorages.getFileStorageService().getFileStorage(uri);
            } catch (OXException e) {
                throw StorageException.wrapForRMI(e);
            } catch (URISyntaxException e) {
                throw new StorageException("Invalid file storage URI: " + baseUri, e);
            }

            // Initialize mover instance
            FilestoreDataMover fsdm = FilestoreDataMover.newContext2UserMover(srcFilestore, destFilestore, maxQuota, storageUser, storageContext);

            // Enable user after processing
            fsdm.addPostProcessTask(new PostProcessTask() {

                @Override
                public void perform(ExecutionException executionError) throws StorageException {
                    if (null == executionError) {
                        oxuser.enableUser(user_id, ctx);
                    } else {
                        log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "An execution error occurred during \"movefromcontexttouserfilestore\" for user {} in context {}. User will stay disabled.", I(user_id), ctx.getId(), executionError.getCause());
                    }
                }
            });

            oxuser.disableUser(user_id, ctx);

            if (false == inline) {
                // Schedule task
                return TaskManager.getInstance().addJob(fsdm, "movefromcontexttouserfilestore", "move user " + user_id + " from context " + ctx.getIdAsString() + " from context to individual filestore " + destFilestore.getId(), ctx.getId().intValue());
            }

            // Execute with current thread
            fsdm.call();
            return -1;
        } catch (InterruptedException e) {
            // Keep interrupted status
            Thread.currentThread().interrupt();
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (IOException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (OXException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (ProgrammErrorException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public int moveFromUserToContextFilestore(final Context ctx, User user, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        return moveFromUserToContextFilestore(ctx, user, credentials, false);
    }

    private int moveFromUserToContextFilestore(final Context ctx, User user, Credentials credentials, boolean inline) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, NoSuchFilestoreException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for moving file storage data is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            if (false == inline) {
                basicauth.doAuthentication(auth, ctx);
                checkContextAndSchema(ctx);
                try {
                    setIdOrGetIDFromNameAndIdObject(ctx, user);
                } catch (NoSuchObjectException e) {
                    throw new NoSuchUserException(e);
                }

                int user_id = user.getId().intValue();
                if (!tool.existsUser(ctx, user_id)) {
                    throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
                }
            }

            final int userId = user.getId().intValue();
            final OXUserStorageInterface oxuser = this.oxu;
            final OXContextInterface oxctx = AdminServiceRegistry.getInstance().getService(OXContextInterface.class, true);

            Context storageContext = oxctx.getOwnData(ctx, Credentials.alwaysAcceptFor(auth.getLogin()));
            User storageUser = oxuser.getData(ctx, new User[] { user })[0];

            if (null == storageUser.getFilestoreId() || storageUser.getFilestoreId().intValue() <= 0) {
                throw new StorageException("User " + storageUser.getId() + " has no file storage set.");
            }
            if (storageUser.getFilestoreOwner() != null) {
                int ownerId = storageUser.getFilestoreOwner().intValue();
                if (ownerId > 0 && ownerId != userId) {
                    throw new StorageException("User " + storageUser.getId() + " does not have his own file storage set, but is currently using the file storage from user " + ownerId);
                }
            }
            if (storageContext.getFilestoreId().intValue() == storageUser.getFilestoreId().intValue()) {
                String contextFsName = storageContext.getFilestore_name();
                if (null == contextFsName) {
                    throw new InvalidDataException("Unable to get filestore directory for context " + ctx.getIdAsString());
                }
                String userFsName = storageUser.getFilestore_name();
                if (null == userFsName) {
                    throw new InvalidDataException("Unable to get filestore directory for user " + userId + " in " + ctx.getIdAsString());
                }
                if (contextFsName.equals(userFsName)) {
                    throw new StorageException("User " + storageUser.getId() + " already has a context file storage set.");
                }
            }

            if (!tool.existsStore(storageContext.getFilestoreId().intValue())) {
                throw new NoSuchFilestoreException();
            }
            boolean equal = storageContext.getFilestoreId().intValue() == storageUser.getFilestoreId().intValue();

            if (!equal && !tool.existsStore(storageUser.getFilestoreId().intValue())) {
                throw new NoSuchFilestoreException();
            }

            OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
            Filestore destFilestore = oxu.getFilestore(storageContext.getFilestoreId().intValue(), false);
            Filestore srcFilestore = equal ? destFilestore : oxu.getFilestoreBasic(storageUser.getFilestoreId().intValue());

            // Check equality
            int srcStore_id = storageUser.getFilestoreId().intValue();
            if (srcStore_id <= 0) {
                throw new InvalidDataException("Unable to get filestore " + srcStore_id);
            }
            ctx.setFilestoreId(destFilestore.getId());
            if (srcStore_id == destFilestore.getId().intValue()) {
                // Ok
                // throw new InvalidDataException("The identifiers for the source and destination storage are equal: " + destFilestore);
            }

            // Check storage name
            String name = storageUser.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for user " + userId + " in " + ctx.getIdAsString());
            }
            name = storageContext.getFilestore_name();
            if (name == null) {
                throw new InvalidDataException("Unable to get filestore directory for context " + storageContext.getId());
            }

            // Check capacity
            if (!equal && !oxu.hasSpaceForAnotherUser(destFilestore, false)) {
                throw new StorageException("Cannot move user files from user-associated filestore " + srcFilestore.getId() + " to context-associated filestore " + destFilestore.getId() + " since context-associated one does not have enough space/capacity.");
            }

            // Initialize mover instance
            FilestoreDataMover fsdm = FilestoreDataMover.newUser2ContextMover(srcFilestore, destFilestore, storageUser, storageContext);

            // Enable user after processing
            fsdm.addPostProcessTask(new PostProcessTask() {

                @Override
                public void perform(ExecutionException executionError) throws StorageException {
                    if (null == executionError) {
                        oxuser.enableUser(userId, ctx);
                    } else {
                        log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userId), null, "An execution error occurred during \"movefromusertocontextfilestore\" for user {} in context {}. User will stay disabled.", I(userId), ctx.getId(), executionError.getCause());
                    }
                }
            });

            oxuser.disableUser(userId, ctx);

            if (false == inline) {
                // Schedule task
                return TaskManager.getInstance().addJob(fsdm, "movefromusertocontextfilestore", "move user " + userId + " from context " + ctx.getIdAsString() + " from individual to context filestore " + destFilestore.getId(), ctx.getId().intValue());
            }

            // Execute with current thread
            fsdm.call();
            return -1;
        } catch (InterruptedException e) {
            // Keep interrupted status
            Thread.currentThread().interrupt();
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (IOException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (OXException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (ProgrammErrorException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public void change(final Context ctx, final User usrdata, Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(usrdata);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for change is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            // SPECIAL USER AUTH CHECK FOR THIS METHOD!
            // check if credentials are from oxadmin or from an user
            Integer userid = null;

            contextcheck(ctx);

            checkContextAndSchema(ctx);

            try {
                setIdOrGetIDFromNameAndIdObject(ctx, usrdata);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            usrdata.testMandatoryCreateFieldsNull();
            userid = usrdata.getId();

            if (!cache.contextAuthenticationDisabled()) {
                if (basicauth.isMasterOfContext(auth, ctx)) {
                    basicauth.doAuthentication(auth, ctx);
                } else {
                    final int auth_user_id = tool.getUserIDByUsername(ctx, auth.getLogin());
                    // check if given user is admin
                    if (tool.isContextAdmin(ctx, auth_user_id)) {
                        basicauth.doAuthentication(auth, ctx);
                    } else {
                        basicauth.doUserAuthentication(auth, ctx);
                        // now check if user which authed has the same id as the user he
                        // wants to change,else fail,
                        // cause then he/she wants to change not his own data!
                        if (userid.intValue() != auth_user_id) {
                            throw new InvalidCredentialsException("Permission denied");
                        }
                    }
                }
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), null, "{} - {} - {}", ctx, usrdata, auth);

            if (!tool.existsUser(ctx, userid.intValue())) {
                final NoSuchUserException noSuchUserException = new NoSuchUserException("No such user " + userid + " in context " + ctx.getId());
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), noSuchUserException, EMPTY_STRING);
                throw noSuchUserException;
            }
            if (tool.getIsGuestByUserID(ctx, userid.intValue())) {
                final InvalidDataException invalidDataException = new InvalidDataException("User to change (user id " + userid + " , context id " + ctx.getId() + ") is a guest user. Guests cannot be changed via provisioning.");
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }

            if (tool.existsDisplayName(ctx, usrdata, i(usrdata.getId()))) {
                try {
                    ConfigViewFactory configViewFactory = AdminServiceRegistry.getInstance().getService(ConfigViewFactory.class, true);
                    ConfigView view = configViewFactory.getView(-1, ctx.getId().intValue());
                    if (null == view || view.opt("com.openexchange.user.enforceUniqueDisplayName", Boolean.class, Boolean.TRUE).booleanValue()) {
                        // Do enforce unique display names
                        throw new InvalidDataException("The displayname is already used");
                    }
                } catch (OXException e) {
                    log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), e, "Unable to get \"com.openexchange.user.enforceUniqueDisplayName\". Fallback to enforce display name uniqueness.");
                    throw new InvalidDataException("The displayname is already used");
                }
            }

            checkUserAttributes(usrdata);
            final User[] dbuser = oxu.getData(ctx, new User[] { usrdata });
            tool.checkChangeUserData(ctx, usrdata, dbuser[0], this.prop);

            // Check if he wants to change the filestore id
            {
                Integer filestoreId = usrdata.getFilestoreId();
                if (filestoreId != null) {
                    if (!tool.existsStore(filestoreId.intValue())) {
                        final InvalidDataException inde = new InvalidDataException("No such filestore with id " + filestoreId.intValue());
                        log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), inde, EMPTY_STRING);
                        throw inde;
                    }

                    Integer fsId = dbuser[0].getFilestoreId();
                    if (fsId == null || fsId.intValue() <= 0) {
                        final InvalidDataException inde = new InvalidDataException("Not allowed to change the filestore for user " + userid + " in context " + ctx.getId() + ". Please use appropriate method instead.");
                        log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), inde, EMPTY_STRING);
                        throw inde;
                    }
                    if (fsId.intValue() != filestoreId.intValue()) {
                        final InvalidDataException inde = new InvalidDataException("Not allowed to change the filestore for user " + userid + " in context " + ctx.getId() + ". Please use appropriate method instead.");
                        log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), inde, EMPTY_STRING);
                        throw inde;
                    }
                }
            }

            final boolean isContextAdmin = tool.isContextAdmin(ctx, userid.intValue());

            // Is a quota specified that implies to assign a file storage?
            Long maxQuota = usrdata.getMaxQuota();
            if (maxQuota != null) {
                long quota_max_temp = maxQuota.longValue();
                if (quota_max_temp != -1) {
                    // A valid quota is specified - ensure an appropriate file storage is set
                    Integer fsId = dbuser[0].getFilestoreId();
                    if (fsId == null || fsId.intValue() <= 0) {
                        if (!allowChangingQuotaIfNoFileStorageSet) {
                            throw new StorageException("Quota cannot be changed for user " + userid + " in context " + ctx.getId() + " since that user has no file storage set. See \"ALLOW_CHANGING_QUOTA_IF_NO_FILESTORE_SET\".");
                        }

                        // Auto-select next suitable file storage
                        OXUtilStorageInterface oxutil = OXUtilStorageInterface.getInstance();
                        int fileStorageToPrefer = oxutil.getFilestoreIdFromContext(ctx.getId().intValue());
                        Filestore filestoreForUser = oxutil.findFilestoreForUser(fileStorageToPrefer);

                        // Load it to ensure validity
                        OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
                        try {
                            URI uri = FileStorages.getFullyQualifyingUriForContext(ctx.getId().intValue(), oxu.getFilestoreURI(i(filestoreForUser.getId())));
                            FileStorages.getFileStorageService().getFileStorage(uri);
                        } catch (OXException e) {
                            throw StorageException.wrapForRMI(e);
                        }

                        // (Synchronous) Move from context to individual user file storage
                        moveFromContextToUserFilestore(ctx, usrdata, filestoreForUser, quota_max_temp, auth, true);
                    } else {
                        if (!OXToolStorageInterface.getInstance().existsStore(i(fsId))) {
                            throw new StorageException("Filestore with identifier " + fsId + " does not exist.");
                        }

                        // Load it to ensure validity
                        OXUtilStorageInterface oxu = OXUtilStorageInterface.getInstance();
                        try {
                            URI uri = FileStorages.getFullyQualifyingUriForContext(ctx.getId().intValue(), oxu.getFilestoreURI(i(fsId)));
                            FileStorages.getFileStorageService().getFileStorage(uri);
                        } catch (OXException e) {
                            throw StorageException.wrapForRMI(e);
                        }
                    }
                }
            }

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if ((oxuser instanceof OXUserPluginInterfaceExtended) && (oxuser.canHandleContextAdmin() || (!oxuser.canHandleContextAdmin() && !isContextAdmin))) {
                            OXUserPluginInterfaceExtended oxuserExtended = (OXUserPluginInterfaceExtended) oxuser;
                            try {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), null, "Calling change for plugin: {}", oxuser.getClass().getName());
                                oxuserExtended.beforeChange(ctx, usrdata, auth);
                            } catch (PluginException | RuntimeException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), e, "Error while calling change for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            }
                        }
                    }
                }
            }

            oxu.change(ctx, usrdata);

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if (oxuser.canHandleContextAdmin() || (!oxuser.canHandleContextAdmin() && !isContextAdmin)) {
                            try {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), null, "Calling change for plugin: {}", oxuser.getClass().getName());
                                oxuser.change(ctx, usrdata, auth);
                            } catch (PluginException | RuntimeException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), e, "Error while calling change for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            }
                        }
                    }
                }
            }

            // change cached admin credentials if necessary
            if (isContextAdmin && usrdata.getPassword() != null) {
                final Credentials cauth = cache.getAdminCredentials(ctx);
                if (cauth == null) {
                    // change via master credentials and no admin credentials in cache
                    return;
                }

                final String mech = cache.getAdminAuthMech(ctx);
                try {
                    PasswordMechRegistry mechFactory = AdminServiceRegistry.getInstance().getService(PasswordMechRegistry.class, true);
                    PasswordMech passwordMech = mechFactory.get(mech);
                    if (null != passwordMech) {
                        PasswordDetails passwordDetails = passwordMech.encode(usrdata.getPassword());
                        cauth.setPassword(passwordDetails.getEncodedPassword());
                        cauth.setSalt(passwordDetails.getSalt());
                        cauth.setPasswordMech(passwordDetails.getPasswordMech());
                    } else {
                        IllegalStateException e = new IllegalStateException("There must be a useable password mechanism.");
                        log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), e, "Error encrypting password for credential cache.");
                        throw new StorageException(e);
                    }
                } catch (OXException e) {
                    log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), e, "Error encrypting password for credential cache.");
                    throw StorageException.wrapForRMI(e);
                }
                cache.setAdminCredentials(ctx, mech, cauth);
            }
        } catch (NoSuchFilestoreException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), credentials, ctx.getIdAsString(), String.valueOf(usrdata.getId()));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, usrdata);
            throw e;
        }
    }

    @Override
    public void changeModuleAccess(final Context ctx, final User user, final UserModuleAccess moduleAccess, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user, moduleAccess);
            } catch (InvalidDataException e1) {
                final InvalidDataException invalidDataException = new InvalidDataException("User or UserModuleAccess is null", e1);
                log(LogLevel.ERROR, LOGGER, credentials, e1, EMPTY_STRING);
                throw invalidDataException;
            }
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null, "{} - {} - {} - {}", ctx, user, moduleAccess, auth);

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            // Change module access
            oxu.changeModuleAccess(ctx, user_id, moduleAccess);

            // Check for context administrator
            final boolean isContextAdmin = tool.isContextAdmin(ctx, user.getId().intValue());

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if (oxuser.canHandleContextAdmin() || (!oxuser.canHandleContextAdmin() && !isContextAdmin)) {
                            try {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "Calling changeModuleAccess for plugin: {}", oxuser.getClass().getName());
                                oxuser.changeModuleAccess(ctx, user, moduleAccess, auth);
                            } catch (PluginException | RuntimeException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), e, "Error while calling change for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }

        //      JCS
        try {
            UserConfigurationStorage.getInstance().invalidateCache(user.getId().intValue(), new ContextImpl(ctx.getId().intValue()));
        } catch (OXException e) {
            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Error removing user {} in context {} from configuration storage", user.getId(), ctx.getId());
        }
        // END OF JCS
    }

    @Override
    public void changeModuleAccess(final Context ctx, final User user, final String access_combination_name, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user, access_combination_name);
                if (access_combination_name.trim().length() == 0) {
                    throw new InvalidDataException("Invalid access combination name");
                }
            } catch (InvalidDataException e1) {
                final InvalidDataException invalidDataException = new InvalidDataException("User or UserModuleAccess is null", e1);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null, "{} - {} - {} - {}", ctx, user, access_combination_name, auth);

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            UserModuleAccess access = cache.getNamedAccessCombination(access_combination_name.trim(), tool.getAdminForContext(ctx) == user_id);
            if (access == null) {
                // no such access combination name defined in configuration
                // throw error!
                throw new InvalidDataException("No such access combination name \"" + access_combination_name.trim() + "\"");
            }
            access = access.clone();

            // Change module access
            oxu.changeModuleAccess(ctx, user_id, access);

            // Check for context administrator
            final boolean isContextAdmin = tool.isContextAdmin(ctx, user.getId().intValue());

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if (oxuser.canHandleContextAdmin() || (!oxuser.canHandleContextAdmin() && !isContextAdmin)) {
                            try {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "Calling changeModuleAccess for plugin: {}", oxuser.getClass().getName());
                                oxuser.changeModuleAccess(ctx, user, access_combination_name, auth);
                            } catch (PluginException | RuntimeException e) {
                                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), e, "Error while calling change for plugin: {}", oxuser.getClass().getName());
                                throw StorageException.wrapForRMI(e);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }

        // JCS
        try {
            UserConfigurationStorage.getInstance().invalidateCache(user.getId().intValue(), new ContextImpl(ctx.getId().intValue()));
        } catch (OXException e) {
            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Error removing user {} in context {} from configuration storage", user.getId(), ctx.getId());
            throw convertException(e);
        } catch (RuntimeException e) {
            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, EMPTY_STRING);
            throw convertException(e);
        }

        final CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
        if (null != cacheService) {
            try {
                final Cache usercCache = cacheService.getCache("User");
                final Cache upCache = cacheService.getCache("UserPermissionBits");
                final Cache ucCache = cacheService.getCache("UserConfiguration");
                final Cache usmCache = cacheService.getCache("UserSettingMail");
                final Cache capabilitiesCache = cacheService.getCache("Capabilities");
                {
                    final CacheKey key = cacheService.newCacheKey(i(ctx.getId()), user.getId().intValue());
                    usercCache.remove(key);
                    usercCache.remove(cacheService.newCacheKey(i(ctx.getId()), user.getName()));
                    upCache.remove(key);
                    ucCache.remove(key);
                    usmCache.remove(key);
                    capabilitiesCache.removeFromGroup(user.getId(), ctx.getId().toString());
                    try {
                        UserConfigurationStorage.getInstance().invalidateCache(user.getId().intValue(), new ContextImpl(ctx.getId().intValue()));
                    } catch (OXException e) {
                        log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Error removing user {} in context {} from configuration storage", user.getId(), ctx.getId());
                    }
                }
            } catch (Exception e) {
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, EMPTY_STRING);
            }
        }
        // END OF JCS
    }

    @Override
    public User create(final Context ctx, final User usr, final UserModuleAccess access, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        // Call common create method directly because we already have out access module
        return createUserCommon(ctx, usr, access, credentials);
    }

    @Override
    public User create(final Context ctx, final User usrdata, final String access_combination_name, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            // Resolve the access rights by the specified combination name. If combination name does not exists, throw error as it is described
            // in the spec!
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(usrdata, access_combination_name);
                if (access_combination_name.trim().length() == 0) {
                    throw new InvalidDataException("Invalid access combination name");
                }
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for create is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(usrdata.getId()), null, "{} - {} - {} - {}", ctx, usrdata, access_combination_name, auth);

            basicauth.doAuthentication(auth, ctx);

            UserModuleAccess access = cache.getNamedAccessCombination(access_combination_name.trim(), false);
            if (access == null) {
                // no such access combination name defined in configuration
                // throw error!
                throw new InvalidDataException("No such access combination name \"" + access_combination_name.trim() + "\"");
            }
            access = access.clone();

            // Call main create user method with resolved access rights
            return createUserCommon(ctx, usrdata, access, auth);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, usrdata);
            throw e;
        }
    }

    @Override
    public User create(final Context ctx, final User usrdata, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            /*
             * Resolve current access rights from the specified context (admin) as
             * it is described in the spec and then call the main create user method
             * with the access rights!
             */
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;

            try {
                doNullCheck(usrdata);
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, credentials, e3, "One of the given arguments for create is null");
                throw e3;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(usrdata.getId()), null, "{} - {} - {}", ctx, usrdata, auth);

            basicauth.doAuthentication(auth, ctx);

            /*
             * Resolve admin user of specified context via tools and then get his current module access rights
             */

            final int admin_id = tool.getAdminForContext(ctx);
            final UserModuleAccess access = oxu.getModuleAccess(ctx, admin_id);

            if (access.isPublicFolderEditable()) {
                // publicFolderEditable can only be applied to the context administrator.
                access.setPublicFolderEditable(false);
            }

            return createUserCommon(ctx, usrdata, access, auth);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, usrdata);
            throw e;
        }
    }

    @Override
    public User getContextAdmin(final Context ctx, final Credentials credentials) throws InvalidCredentialsException, StorageException, InvalidDataException, RemoteException {
        final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;

        try {
            basicauth.doAuthentication(auth, ctx);
            return (oxu.getData(ctx, new User[] { new User(tool.getAdminForContext(ctx)) }))[0];
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, null);
            throw e;
        }
    }

    @Override
    public UserModuleAccess getContextAdminUserModuleAccess(final Context ctx, final Credentials credentials) throws StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, RemoteException {
        final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;

        try {
            basicauth.doAuthentication(auth, ctx);

            /*
             * Resolve admin user of specified context via tools and then get his current module access rights
             */

            final int admin_id = tool.getAdminForContext(ctx);
            final UserModuleAccess access = oxu.getModuleAccess(ctx, admin_id);
            return access;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, null);
            throw e;
        }
    }

    /*
     * Main method to create a user. Which all inner create methods MUST use after resolving the access rights!
     */
    private User createUserCommon(final Context ctx, final User usr, final UserModuleAccess access, final Credentials auth) throws StorageException, InvalidCredentialsException, InvalidDataException, DatabaseUpdateException, RemoteException {
        try {
            try {
                doNullCheck(usr, access);
            } catch (InvalidDataException e3) {
                log(LogLevel.ERROR, LOGGER, auth, e3, "One of the given arguments for create is null");
                throw e3;
            }

            log(LogLevel.DEBUG, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), null, "{} - {} - {} - {}", ctx, usr, access, auth);
            try {
                basicauth.doAuthentication(auth, ctx);

                checkUserAttributes(usr);

                checkContextAndSchema(ctx);

                tool.checkCreateUserData(ctx, usr);

                if (tool.existsUserName(ctx, usr.getName())) {
                    throw new InvalidDataException("User " + usr.getName() + " already exists in this context");
                }

                // validate email adresss
                tool.primaryMailExists(ctx, usr.getPrimaryEmail());
            } catch (InvalidDataException e2) {
                log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e2, EMPTY_STRING);
                throw e2;
            } catch (EnforceableDataObjectException e) {
                log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e, EMPTY_STRING);
                throw new InvalidDataException(e.getMessage());
            }

            final int retval = oxu.create(ctx, usr, access);
            usr.setId(Integer.valueOf(retval));
            final List<OXUserPluginInterface> interfacelist = new ArrayList<>();

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        final String bundlename = oxuser.getClass().getName();
                        try {
                            final boolean canHandleContextAdmin = oxuser.canHandleContextAdmin();
                            if (canHandleContextAdmin || (!canHandleContextAdmin && !tool.isContextAdmin(ctx, usr.getId().intValue()))) {
                                try {
                                    log(LogLevel.DEBUG, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), null, "Calling create for plugin: {}", bundlename);
                                    oxuser.create(ctx, usr, access, auth);
                                    interfacelist.add(oxuser);
                                } catch (PluginException | RuntimeException e) {
                                    log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e, "Error while calling create for plugin: {}", bundlename);
                                    log(LogLevel.INFO, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), null, "Now doing rollback for everything until now...");
                                    for (final OXUserPluginInterface oxuserinterface : interfacelist) {
                                        try {
                                            oxuserinterface.delete(ctx, new User[] { usr }, auth);
                                        } catch (PluginException | RuntimeException e1) {
                                            log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e1, "Error doing rollback for plugin: {}", bundlename);
                                        }
                                    }
                                    try {
                                        oxu.delete(ctx, usr, I(-1));
                                    } catch (StorageException e1) {
                                        log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e1, "Error doing rollback for creating user in database");
                                    }
                                    throw StorageException.wrapForRMI(e);
                                }
                            }
                        } catch (RuntimeException e) {
                            log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e, "Error while calling canHandleContextAdmin for plugin: {}", bundlename);
                            log(LogLevel.INFO, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), null, "Now doing rollback for everything until now...");
                            for (final OXUserPluginInterface oxuserinterface : interfacelist) {
                                try {
                                    oxuserinterface.delete(ctx, new User[] { usr }, auth);
                                } catch (PluginException | RuntimeException e1) {
                                    log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e1, "Error doing rollback for plugin: {}", bundlename);
                                }
                            }
                            try {
                                oxu.delete(ctx, usr, I(-1));
                            } catch (StorageException e1) {
                                log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e1, "Error doing rollback for creating user in database");
                            }
                            throw StorageException.wrapForRMI(e);
                        }
                    }
                }
            }

            // The mail account cache caches resolved imap logins or primary addresses. Creating or changing a user needs the invalidation of
            // that cached data.
            final CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
            if (null != cacheService) {
                try {
                    final Cache mailAccountCache = cacheService.getCache("MailAccount");
                    mailAccountCache.remove(cacheService.newCacheKey(ctx.getId().intValue(), String.valueOf(0), String.valueOf(usr.getId())));
                    mailAccountCache.remove(cacheService.newCacheKey(ctx.getId().intValue(), String.valueOf(usr.getId())));
                    mailAccountCache.invalidateGroup(ctx.getId().toString());

                    final Cache globalFolderCache = cacheService.getCache("GlobalFolderCache");
                    CacheKey cacheKey = cacheService.newCacheKey(1, "0", Integer.toString(FolderObject.SYSTEM_LDAP_FOLDER_ID));
                    globalFolderCache.removeFromGroup(cacheKey, ctx.getId().toString());

                    final Cache folderCache = cacheService.getCache("OXFolderCache");
                    cacheKey = cacheService.newCacheKey(ctx.getId().intValue(), FolderObject.SYSTEM_LDAP_FOLDER_ID);
                    folderCache.removeFromGroup(cacheKey, ctx.getId().toString());
                } catch (OXException e) {
                    log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e, EMPTY_STRING);
                }
            }

            // If com.openexchange.imap.initWithSpecialUse is set to true, add the com.openexchange.mail.specialuse.check to this user
            final ConfigViewFactory viewFactory = AdminServiceRegistry.getInstance().getService(ConfigViewFactory.class);
            if (viewFactory != null) {
                ConfigView view;
                try {
                    view = viewFactory.getView(usr.getId().intValue(), ctx.getId().intValue());
                    Boolean check = view.opt("com.openexchange.imap.initWithSpecialUse", Boolean.class, Boolean.TRUE);
                    if (check != null && check.booleanValue()) {
                        ConfigProperty<Boolean> prop = view.property(ConfigViewScope.USER.getScopeName(), "com.openexchange.mail.specialuse.check", Boolean.class);
                        prop.set(Boolean.TRUE);
                        usr.setUserAttribute("config", "com.openexchange.mail.specialuse.check", Boolean.TRUE.toString());
                    }

                } catch (OXException e) {
                    log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), String.valueOf(usr.getId()), e, "Unable to set special use check property!");
                }
            }

            // Return created user
            return usr;
        } catch (NoSuchContextException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), auth, ctx.getIdAsString(), String.valueOf(usr.getId()));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, auth, ctx, usr);
            throw e;
        }
    }

    @Override
    public void delete(final Context ctx, final User user, final Integer destUser, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        delete(ctx, new User[] { user }, destUser, auth);
    }

    @Override
    public void delete(final Context ctx, final User[] users, Integer destUser, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck((Object[]) users);
            } catch (InvalidDataException e1) {
                log(LogLevel.ERROR, LOGGER, credentials, e1, "One of the given arguments for delete is null");
                throw e1;
            }

            if (users.length == 0) {
                final InvalidDataException e = new InvalidDataException("User array is empty");
                log(LogLevel.ERROR, LOGGER, credentials, e, EMPTY_STRING);
                throw e;
            }
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            basicauth.doAuthentication(auth, ctx);

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(users), null, "{} - {} - {}", ctx, Arrays.toString(users), auth);
            checkContextAndSchema(ctx);

            try {
                setUserIdInArrayOfUsers(ctx, users);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            // FIXME: Change function from int to user object
            if (!tool.existsUser(ctx, users)) {
                final NoSuchUserException noSuchUserException = new NoSuchUserException("No such user(s) " + getUserIdArrayFromUsersAsString(users) + " in context " + ctx.getId());
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(users), noSuchUserException, "No such user(s) {} in context {}", Arrays.toString(users), ctx.getId());
                throw noSuchUserException;
            }

            int contextAdminId = tool.getAdminForContext(ctx);
            List<User> filestoreOwners = new java.util.LinkedList<>();
            {
                Set<Integer> dubCheck = new HashSet<>();
                for (final User user : users) {
                    if (destUser != null && user.getId().intValue() == destUser.intValue()) {
                        throw new InvalidDataException("It is not allowed to reassign the shared data to the user which should be deleted. Please choose a different reassign user.");
                    }
                    if (false == dubCheck.add(user.getId())) {
                        throw new InvalidDataException("User " + user.getId() + " is contained multiple times in delete request.");
                    }

                    if (contextAdminId == user.getId().intValue()) {
                        throw new InvalidDataException("Admin delete not supported");
                    }

                    if (tool.isMasterFilestoreOwner(ctx, user.getId().intValue())) {
                        Map<Integer, List<Integer>> slaveUsers = tool.fetchSlaveUsersOfMasterFilestore(ctx, user.getId().intValue());
                        if (!slaveUsers.isEmpty()) {
                            // slave users found, auto-delete guests in same context if applicable
                            Boolean autoDeleteGuestsUsingFilestore = AdminProperties.optScopedProperty(AdminProperties.User.AUTO_DELETE_GUESTS_USING_FILESTORE, PropertyScope.propertyScopeForUser(i(user.getId()), i(ctx.getId())), Boolean.class);
                            if ((null != autoDeleteGuestsUsingFilestore && !b(autoDeleteGuestsUsingFilestore)) || !deleteContainedGuestUsers(slaveUsers.get(ctx.getId()), ctx, user, credentials)) {
                                String affectedUsers = mapToString(slaveUsers);
                                throw new InvalidDataException("The user " + user.getId() + " is the owner of a master filestore which other users are using. " + "Before deleting this user you must move the filestores of the affected users either to the context filestore, " + "to another master filestore or to a user filestore with the appropriate commandline tools. " + "Affected users are: " + affectedUsers);
                            }

                            // check again and proceed
                            slaveUsers = tool.fetchSlaveUsersOfMasterFilestore(ctx, user.getId().intValue());
                            if (!slaveUsers.isEmpty()) {
                                String affectedUsers = mapToString(slaveUsers);
                                throw new InvalidDataException("The user " + user.getId() + " is the owner of a master filestore which other users are using. " + "Before deleting this user you must move the filestores of the affected users either to the context filestore, " + "to another master filestore or to a user filestore with the appropriate commandline tools. " + "Affected users are: " + affectedUsers);
                            }
                        }

                        filestoreOwners.add(user);
                    }

                }
            }

            ConfigViewFactory configViewFactory = AdminServiceRegistry.getInstance().getService(ConfigViewFactory.class);
            if (destUser == null) {
                // Move to store of context administrator
                User adminUser = oxu.getData(ctx, new User[] { new User(contextAdminId) })[0];
                if (adminUser.getFilestoreId().intValue() > 0) {
                    // Context administrator uses a dedicated store
                    for (User filestoreOwner : filestoreOwners) {
                        // Disable Unified Quota first (if enabled); otherwise 'c.o.filestore.impl.groupware.unified.UnifiedQuotaFilestoreDataMoveListener' kicks-in and will throw an exception
                        disableUnifiedQuotaIfEnabled(filestoreOwner, ctx, configViewFactory);

                        // Move files...
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(filestoreOwner.getId()), null, "User {} has an individual filestore set. Hence, moving user-associated files to filestore of context administrator...", filestoreOwner.getId());
                        moveFromUserFilestoreToMaster(ctx, filestoreOwner, adminUser, credentials, true);
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(filestoreOwner.getId()), null, "Moved all files from user {} to filestore of context administrator.", filestoreOwner.getId());
                    }
                } else {
                    // Context administrator uses general context store
                    for (User filestoreOwner : filestoreOwners) {
                        // Disable Unified Quota first (if enabled); otherwise 'c.o.filestore.impl.groupware.unified.UnifiedQuotaFilestoreDataMoveListener' kicks-in and will throw an exception
                        disableUnifiedQuotaIfEnabled(filestoreOwner, ctx, configViewFactory);

                        // Move files...
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(filestoreOwner.getId()), null, "User {} has an individual filestore set. Hence, moving user-associated files to context filestore...", filestoreOwner.getId());
                        moveFromUserToContextFilestore(ctx, filestoreOwner, credentials, true);
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(filestoreOwner.getId()), null, "Moved all files from user {} to context filestore.", filestoreOwner.getId());
                    }
                }
            } else {
                if (destUser.intValue() > 0) { // Move to master store
                    if (!tool.existsUser(ctx, destUser.intValue())) {
                        throw new InvalidDataException(String.format("The reassign user with id %1$s does not exist in context %2$s. Please choose a different reassign user.", destUser, ctx.getId()));
                    }
                    if (!tool.isMasterFilestoreOwner(ctx, destUser.intValue())) {
                        throw new InvalidDataException(String.format("The reassign user with id %1$s is not an owner of a filestore. Please choose a different reassign user.", destUser));
                    }
                    User masterUser = new User(destUser.intValue());
                    for (User filestoreOwner : filestoreOwners) {
                        // Disable Unified Quota first (if enabled); otherwise 'c.o.filestore.impl.groupware.unified.UnifiedQuotaFilestoreDataMoveListener' kicks-in and will throw an exception
                        disableUnifiedQuotaIfEnabled(filestoreOwner, ctx, configViewFactory);

                        // Move files...
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(filestoreOwner.getId()), null, "User {} has an individual filestore set. Hence, moving user-associated files to filestore of user {}", filestoreOwner.getId(), masterUser.getId());
                        moveFromUserFilestoreToMaster(ctx, filestoreOwner, masterUser, credentials);
                        log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(filestoreOwner.getId()), null, "Moved all files from user {} to filestore of user {}.", filestoreOwner.getId(), masterUser.getId());
                    }
                }
            }

            User[] retusers = oxu.getData(ctx, users);

            final List<OXUserPluginInterface> interfacelist = new ArrayList<>();

            // Here we define a list which takes all exceptions which occur during plugin-processing
            // By this we are able to throw all exceptions to the client while concurrently processing all plugins
            final List<Exception> exceptionlist = new ArrayList<>();

            // Trigger plugin extensions
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuser : pluginInterfaces.getUserPlugins().getServiceList()) {
                        if (!oxuser.canHandleContextAdmin()) {
                            retusers = removeContextAdmin(ctx, retusers);
                            if (retusers.length > 0) {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(retusers), null, "Calling delete for plugin: {}", oxuser.getClass().getName());
                                final Exception exception = callDeleteForPlugin(ctx, auth, retusers, interfacelist, oxuser.getClass().getName(), oxuser);
                                if (null != exception) {
                                    exceptionlist.add(exception);
                                }
                            }
                        } else {
                            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(retusers), null, "Calling delete for plugin: {}", oxuser.getClass().getName());
                            final Exception exception = callDeleteForPlugin(ctx, auth, retusers, interfacelist, oxuser.getClass().getName(), oxuser);
                            if (null != exception) {
                                exceptionlist.add(exception);
                            }
                        }

                    }
                }
            }

            oxu.delete(ctx, users, destUser);
            for (final User user : users) {
                try {
                    Filestore2UserUtil.removeFilestore2UserEntry(ctx.getId().intValue(), user.getId().intValue(), cache);
                } catch (Exception e) {
                    log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Failed to remove filestore2User entry for user {} in context {}", ctx.getId(), user.getId());
                }
            }

            // JCS
            final CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
            if (null != cacheService) {
                try {
                    final Cache usercCache = cacheService.getCache("User");
                    final Cache upCache = cacheService.getCache("UserPermissionBits");
                    final Cache ucCache = cacheService.getCache("UserConfiguration");
                    final Cache usmCache = cacheService.getCache("UserSettingMail");
                    final Cache capabilitiesCache = cacheService.getCache("Capabilities");
                    for (final User user : users) {
                        final CacheKey key = cacheService.newCacheKey(i(ctx.getId()), user.getId().intValue());
                        usercCache.remove(key);
                        usercCache.remove(cacheService.newCacheKey(i(ctx.getId()), user.getName()));
                        upCache.remove(key);
                        ucCache.remove(key);
                        usmCache.remove(key);
                        capabilitiesCache.removeFromGroup(user.getId(), ctx.getId().toString());
                        try {
                            UserConfigurationStorage.getInstance().invalidateCache(user.getId().intValue(), new ContextImpl(ctx.getId().intValue()));
                        } catch (OXException e) {
                            log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Error removing user {} in context {} from configuration storage", user.getId(), ctx.getId());
                        }
                    }
                } catch (OXException e) {
                    log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(users), e, EMPTY_STRING);
                }
            }
            // END OF JCS

            if (!exceptionlist.isEmpty()) {
                final StringBuilder sb = new StringBuilder("The following exceptions occured in the plugins: ");
                for (final Exception e : exceptionlist) {
                    sb.append(e.toString());
                    sb.append('\n');
                }
                throw new StorageException(sb.toString());
            }
        } catch (NoSuchFilestoreException e) {
            RemoteException remoteException = RemoteExceptionUtils.convertException(e);
            logAndReturnException(LOGGER, remoteException, e.getExceptionId(), credentials, ctx.getIdAsString(), getObjectIds(users));
            throw remoteException;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx.getIdAsString(), getObjectIds(users));
            throw e;
        }
    }

    /**
     * Deletes guest users possibly contained in specified list of user identifiers.
     *
     * @param userIds The list of user identifiers to examine
     * @param ctx The context in which users reside
     * @param user The affected user needed for logging
     * @param credentials The credential needed for logging
     * @return <code>true</code> if any guest user has been deleted; otherwise <code>false</code>
     * @throws StorageException If a storage exception occurs
     */
    private boolean deleteContainedGuestUsers(List<Integer> userIds, final Context ctx, final User user, final Credentials credentials) throws StorageException {
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }

        try {
            List<User> slaveGuestUsers = userIds.stream().filter(userId -> isGuestUser(i(userId), ctx)).map(userId -> new User(i(userId))).collect(Collectors.toList());
            if (slaveGuestUsers.isEmpty()) {
                return false;
            }

            log(LogLevel.INFO, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null,
                "User {} is owner of a master filestore which is still in use by guest users. Performing implicit deletion of associated guest accounts.", user.getId());
            try {
                delete(ctx, slaveGuestUsers.toArray(new User[slaveGuestUsers.size()]), user.getId(), credentials);
            } catch (Exception e) {
                log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), e,
                    "Unexpected error purging guest accounts using the filestore of user {}. Manual intervention might be required.", user.getId());
            }
            return true;
        } catch (StorageRuntimeException e) {
            throw e.getStorageException();
        }
    }

    /**
     * Checks if denoted user is a guest user.
     *
     * @param userId The user identifier
     * @param ctx The context associated with the user
     * @return <code>true</code> if user is considered as a guest user; otherwise <code>false</code>
     * @throws StorageRuntimeException If a StorageException occurs
     */
    private boolean isGuestUser(int userId, Context ctx) {
        try {
            return tool.isGuestUser(ctx, userId);
        } catch (StorageException e) {
            throw new StorageRuntimeException(e);
        }
    }

    private void disableUnifiedQuotaIfEnabled(User user, Context ctx, ConfigViewFactory optConfigViewFactory) {
        if (null != optConfigViewFactory) {
            try {
                ConfigView view = optConfigViewFactory.getView(user.getId().intValue(), ctx.getId().intValue());
                String property = "com.openexchange.unifiedquota.enabled";
                Boolean enabled = view.opt(property, Boolean.class, Boolean.FALSE);
                if (enabled != null && enabled.booleanValue()) {
                    ConfigProperty<Boolean> prop = view.property(ConfigViewScope.USER.getScopeName(), property, Boolean.class);
                    prop.set(Boolean.FALSE);
                    user.setUserAttribute("config", property, Boolean.FALSE.toString());
                }
            } catch (Exception e) {
                log(LogLevel.WARNING, LOGGER, null, ctx.getIdAsString(), String.valueOf(user.getId()), e, "Failed to disable Unified Quota for user {} in context {}", user.getId(), ctx.getId());
            }
        }
    }

    private String mapToString(Map<Integer, List<Integer>> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, List<Integer>> cidEntry : map.entrySet()) {
            builder.append("\nCID: ").append(cidEntry.getKey()).append(", User IDs: ");
            List<Integer> ids = cidEntry.getValue();
            for (Integer id : ids) {
                builder.append(id).append(',');
            }
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    @Override
    public User getData(final Context ctx, final User user, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchUserException, DatabaseUpdateException {
        return getData(ctx, new User[] { user }, auth)[0];
    }

    @Override
    public User[] getData(final Context ctx, final User[] users, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchUserException, DatabaseUpdateException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck((Object[]) users);
            } catch (InvalidDataException e1) {
                log(LogLevel.ERROR, LOGGER, credentials, e1, "One of the given arguments for getData is null");
                throw e1;
            }
            try {
                checkContext(ctx);
                if (users.length <= 0) {
                    throw new InvalidDataException();
                }
            } catch (InvalidDataException e) {
                log(LogLevel.ERROR, LOGGER, credentials, e, EMPTY_STRING);
                throw e;
            }
            checkExistence(ctx);
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(users), null, "{} - {} - {}", ctx, Arrays.toString(users), auth);
            // enable check who wants to get data if authentication is enabled
            if (!cache.contextAuthenticationDisabled()) {
                // ok here its possible that a user wants to get his own data
                // SPECIAL USER AUTH CHECK FOR THIS METHOD!
                // check if credentials are from oxadmin or from an user
                // check if given user is not admin, if he is admin, the
                final User authuser = new User();
                authuser.setName(auth.getLogin());
                if (basicauth.isMasterOfContext(auth, ctx)) {
                    basicauth.doAuthentication(auth, ctx);
                } else if (!tool.isContextAdmin(ctx, authuser)) {
                    final InvalidCredentialsException invalidCredentialsException = new InvalidCredentialsException("Permission denied");
                    if (users.length == 1) {
                        final int auth_user_id = authuser.getId().intValue();
                        basicauth.doUserAuthentication(auth, ctx);
                        // its possible that he wants his own data
                        final Integer userid = users[0].getId();
                        if (userid != null) {
                            if (userid.intValue() != auth_user_id) {
                                throw invalidCredentialsException;
                            }
                        } else {
                            // id not set, try to resolv id by username and then check again
                            final String username = users[0].getName();
                            if (username != null) {
                                final int check_user_id = tool.getUserIDByUsername(ctx, username);
                                if (check_user_id != auth_user_id) {
                                    log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), null, "user[0].getId() does not match id from Credentials.getLogin()");
                                    throw invalidCredentialsException;
                                }
                            } else {
                                log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(userid), null, "Cannot resolv user[0]`s internal id because the username is not set!");
                                throw new InvalidDataException("Username and userid missing.");
                            }
                        }
                    } else {
                        log(LogLevel.ERROR, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(users), invalidCredentialsException, "User sent {} users to get data for. Only context admin is allowed to do that", Integer.valueOf(users.length));
                        throw invalidCredentialsException;
                        // one user cannot edit more than his own data
                    }
                } else {
                    basicauth.doAuthentication(auth, ctx);
                }
            } else {
                basicauth.doAuthentication(auth, ctx);
            }

            checkContextAndSchema(ctx);

            for (final User usr : users) {
                final String username = usr.getName();
                final Integer userid = usr.getId();
                if (null != userid && !tool.existsUser(ctx, i(userid))) {
                    if (username != null) {
                        throw new NoSuchUserException("No such user " + username + " in context " + ctx.getId());
                    }
                    throw new NoSuchUserException("No such user " + userid + " in context " + ctx.getId());
                }
                if (null != username && !tool.existsUserName(ctx, username)) {
                    throw new NoSuchUserException("No such user " + username + " in context " + ctx.getId());
                }
                if (username == null && userid == null) {
                    throw new InvalidDataException("Username and userid missing.");
                }
                // ok , try to get the username by id or username
                if (username == null && null != userid) {
                    usr.setName(tool.getUsernameByUserID(ctx, userid.intValue()));
                }
                if (userid == null) {
                    usr.setId(Integer.valueOf(tool.getUserIDByUsername(ctx, username)));
                }
            }

            User[] retusers = oxu.getData(ctx, users);

            // Trigger plugin interfaces
            {
                final PluginInterfaces pluginInterfaces = PluginInterfaces.getInstance();
                if (null != pluginInterfaces) {
                    for (final OXUserPluginInterface oxuserplugin : pluginInterfaces.getUserPlugins().getServiceList()) {
                        log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(retusers), null, "Calling getData for plugin: {}", oxuserplugin.getClass().getName());
                        retusers = oxuserplugin.getData(ctx, retusers, auth);
                    }
                }
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), getObjectIds(retusers), null, Arrays.toString(retusers));
            return retusers;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx.getIdAsString(), getObjectIds(users));
            throw e;
        }
    }

    @Override
    public UserModuleAccess moduleAccessForName(final String accessCombinationName) {
        if (null == accessCombinationName) {
            return null;
        }
        final UserModuleAccess moduleAccess = cache.getAccessCombinationNames().get(accessCombinationName);
        return null == moduleAccess ? null : moduleAccess.clone();
    }

    @Override
    public UserModuleAccess getModuleAccess(final Context ctx, final User user, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e) {
                final InvalidDataException invalidDataException = new InvalidDataException("User object is null", e);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null, "{} - {} - {}", ctx, user, auth);

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }
            return oxu.getModuleAccess(ctx, user_id);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public String getAccessCombinationName(final Context ctx, final User user, final Credentials credentials) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e) {
                final InvalidDataException invalidDataException = new InvalidDataException("User object is null", e);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }
            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user.getId()), null, "{} - {} - {}", ctx, user, auth);

            basicauth.doAuthentication(auth, ctx);
            checkContextAndSchema(ctx);
            try {
                setIdOrGetIDFromNameAndIdObject(ctx, user);
            } catch (NoSuchObjectException e) {
                throw new NoSuchUserException(e);
            }
            final int user_id = user.getId().intValue();

            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            return cache.getNameForAccessCombination(oxu.getModuleAccess(ctx, user_id));
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public User[] list(final Context ctx, final String search_pattern, final Credentials credentials, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        return list(ctx, search_pattern, credentials, false, false, length, offset);
    }

    @Override
    public User[] list(final Context ctx, final String search_pattern, final Credentials credentials, final boolean includeGuests, final boolean excludeUsers, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(ctx, search_pattern);
            } catch (InvalidDataException e1) {
                log(LogLevel.ERROR, LOGGER, credentials, e1, "One of the given arguments for list is null");
                throw e1;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), null, "{} - {}", ctx, auth);

            basicauth.doAuthentication(auth, ctx);

            checkContextAndSchema(ctx);

            final User[] retval = oxu.list(ctx, search_pattern, includeGuests, excludeUsers, length, offset);

            return retval;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, null);
            throw e;
        }
    }

    @Override
    public User[] listCaseInsensitive(final Context ctx, final String search_pattern, final Credentials credentials, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        return listCaseInsensitive(ctx, search_pattern, credentials, false, false, length, offset);
    }

    @Override
    public User[] listCaseInsensitive(final Context ctx, final String search_pattern, final Credentials credentials, final boolean includeGuests, final boolean excludeUsers, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(ctx, search_pattern);
            } catch (InvalidDataException e1) {
                log(LogLevel.ERROR, LOGGER, credentials, e1, "One of the given arguments for list is null");
                throw e1;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            log(LogLevel.DEBUG, LOGGER, credentials, ctx.getIdAsString(), null, "{} - {}", ctx, auth);

            basicauth.doAuthentication(auth, ctx);

            checkContextAndSchema(ctx);

            final User[] retval = oxu.listCaseInsensitive(ctx, search_pattern, includeGuests, excludeUsers, length, offset);

            return retval;
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, null);
            throw e;
        }
    }

    @Override
    public User[] listUsersWithOwnFilestore(final Context context, final Credentials credentials, final Integer filestore_id, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(context);
            } catch (InvalidDataException e1) {
                log(LogLevel.ERROR, LOGGER, credentials, e1, "One of the given arguments for list is null");
                throw e1;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }
            log(LogLevel.DEBUG, LOGGER, credentials, context.getIdAsString(), null, "{} - {}", context, auth);
            basicauth.doAuthentication(auth, context);

            checkContextAndSchema(context);
            if (null == filestore_id || filestore_id.intValue() <= 0) {
                return oxu.listUsersWithOwnFilestore(context, null, length, offset);
            }
            return oxu.listUsersWithOwnFilestore(context, filestore_id, length, offset);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, context, null);
            throw e;
        }
    }

    @Override
    public User[] listAll(final Context ctx, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        return list(ctx, "*", auth, null, null);
    }

    @Override
    public User[] listAll(final Context ctx, final Credentials auth, final boolean includeGuests, final boolean excludeUsers) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        return list(ctx, "*", auth, includeGuests, excludeUsers, null, null);
    }

    private Exception callDeleteForPlugin(final Context ctx, final Credentials auth, final User[] retusers, final List<OXUserPluginInterface> interfacelist, final String bundlename, final OXUserPluginInterface oxuser) {
        try {
            log(LogLevel.DEBUG, LOGGER, auth, ctx.getIdAsString(), getObjectIds(retusers), null, "Calling delete for plugin: {}", bundlename);
            oxuser.delete(ctx, retusers, auth);
            interfacelist.add(oxuser);
            return null;
        } catch (PluginException | RuntimeException e) {
            log(LogLevel.ERROR, LOGGER, auth, ctx.getIdAsString(), getObjectIds(retusers), e, "Error while calling delete for plugin: {}", bundlename);
            return e;
        }
    }

    private static void checkContext(final Context ctx) throws InvalidDataException {
        if (null == ctx || null == ctx.getId()) {
            throw new InvalidDataException("Context invalid");
        }
        // Check a context existence is considered as a security flaw
    }

    private String getUserIdArrayFromUsersAsString(final User[] users) throws InvalidDataException {
        if (null == users) {
            return null;
        } else if (users.length == 0) {
            return EMPTY_STRING;
        }
        final StringBuilder sb = new StringBuilder(users.length * 8);
        {
            final Integer id = users[0].getId();
            if (null == id) {
                throw new InvalidDataException("One user object has no id");
            }
            sb.append(id);
        }
        for (int i = 1; i < users.length; i++) {
            final Integer id = users[i].getId();
            if (null == id) {
                throw new InvalidDataException("One user object has no id");
            }
            sb.append(',');
            sb.append(id);
        }
        return sb.toString();
    }

    private User[] removeContextAdmin(final Context ctx, final User[] retusers) throws StorageException {
        final ArrayList<User> list = new ArrayList<>(retusers.length);
        for (final User user : retusers) {
            if (!tool.isContextAdmin(ctx, user.getId().intValue())) {
                list.add(user);
            }
        }
        return list.toArray(new User[list.size()]);
    }

    @Override
    public void changeModuleAccessGlobal(final String filter, final UserModuleAccess addAccess, final UserModuleAccess removeAccess, final Credentials credentials) throws RemoteException, InvalidCredentialsException, StorageException, InvalidDataException {
        try {
            final Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(addAccess, removeAccess);
            } catch (InvalidDataException e1) {
                final InvalidDataException invalidDataException = new InvalidDataException("Some parameters are null", e1);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            try {
                checkForGABRestriction(addAccess);
                checkForGABRestriction(removeAccess);
            } catch (InvalidDataException e1) {
                final InvalidDataException invalidDataException = new InvalidDataException("\"GlobalAddressBookDisabled\" can not be changed with this method.", e1);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }

            log(LogLevel.DEBUG, LOGGER, credentials, null, "{} - {} - {} - {}", filter, addAccess, removeAccess, auth);

            try {
                basicauth.doAuthentication(auth);
            } catch (InvalidCredentialsException e) {
                log(LogLevel.ERROR, LOGGER, credentials, e, EMPTY_STRING);
                throw e;
            }

            int permissionBits = -1;
            if (filter != null) {
                try {
                    permissionBits = Integer.parseInt(filter);
                } catch (NumberFormatException nfe) {
                    final UserModuleAccess namedAccessCombination = cache.getNamedAccessCombination(filter, true);
                    if (namedAccessCombination == null) {
                        throw new InvalidDataException("No such access combination name \"" + filter.trim() + "\"", nfe);
                    }
                    permissionBits = getPermissionBits(namedAccessCombination);
                }
            }

            final int addBits = getPermissionBits(addAccess);
            final int removeBits = getPermissionBits(removeAccess);
            log(LogLevel.DEBUG, LOGGER, credentials, null, "Adding {} removing {} to filter {}", I(addBits), I(removeBits), filter);

            try {
                tool.changeAccessCombination(permissionBits, addBits, removeBits);
                CacheService cacheService = AdminServiceRegistry.getInstance().getService(CacheService.class);
                cacheService.getCache("User").clear();
                cacheService.getCache("UserPermissionBits").clear();
                cacheService.getCache("UserConfiguration").clear();
                cacheService.getCache("UserSettingMail").clear();
                cacheService.getCache("Capabilities").clear();
            } catch (OXException e) {
                log(LogLevel.ERROR, LOGGER, credentials, null, EMPTY_STRING);
            }

            // TODO: How to notify via EventSystemService ?
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, (String) null, null);
            throw e;
        }
    }

    /**
     * Checks for valid Module Accesses.
     *
     * @param addAccess
     * @throws InvalidDataException
     */
    private void checkForGABRestriction(UserModuleAccess access) throws InvalidDataException {
        if (access.isGlobalAddressBookDisabled()) {
            throw new InvalidDataException("Can not change the value for \"access-global-address-book-disabled\".");
        }
    }

    @Override
    public boolean exists(Context ctx, User user, Credentials credentials) throws RemoteException, InvalidDataException, InvalidCredentialsException, StorageException, DatabaseUpdateException, NoSuchContextException {
        try {
            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            try {
                doNullCheck(user);
            } catch (InvalidDataException e2) {
                final InvalidDataException invalidDataException = new InvalidDataException("One of the given arguments for change is null", e2);
                log(LogLevel.ERROR, LOGGER, credentials, invalidDataException, EMPTY_STRING);
                throw invalidDataException;
            }

            if (prop.getUserProp(AdminProperties.User.AUTO_LOWERCASE, false)) {
                auth.setLogin(auth.getLogin().toLowerCase());
            }

            try {
                basicauth.doAuthentication(auth, ctx);
            } catch (InvalidCredentialsException e) {
                log(LogLevel.ERROR, LOGGER, credentials, e, EMPTY_STRING);
                throw e;
            }

            contextcheck(ctx);

            checkContextAndSchema(ctx);

            if (null != user.getId()) {
                return tool.existsUser(ctx, user);
            } else if (null != user.getName()) {
                return tool.existsUserName(ctx, user.getName());
            } else if (null != user.getDisplay_name()) {
                return tool.existsDisplayName(ctx, user.getDisplay_name());
            } else {
                throw new InvalidDataException("Neither identifier, name nor display name set in given user object");
            }
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    public int getPermissionBits(UserModuleAccess namedAccessCombination) {
        int retval = 0;

        if (namedAccessCombination.isActiveSync()) {
            retval |= UserConfiguration.ACTIVE_SYNC;
        }
        if (namedAccessCombination.getCalendar()) {
            retval |= UserConfiguration.CALENDAR;
        }
        if (namedAccessCombination.isCollectEmailAddresses()) {
            retval |= UserConfiguration.COLLECT_EMAIL_ADDRESSES;
        }
        if (namedAccessCombination.getContacts()) {
            retval |= UserConfiguration.CONTACTS;
        }
        if (namedAccessCombination.getDelegateTask()) {
            retval |= UserConfiguration.DELEGATE_TASKS;
        }
        if (namedAccessCombination.getEditGroup()) {
            retval |= UserConfiguration.EDIT_GROUP;
        }
        if (namedAccessCombination.getEditPassword()) {
            retval |= UserConfiguration.EDIT_PASSWORD;
        }
        if (namedAccessCombination.getEditPublicFolders()) {
            retval |= UserConfiguration.EDIT_PUBLIC_FOLDERS;
        }
        if (namedAccessCombination.getEditResource()) {
            retval |= UserConfiguration.EDIT_RESOURCE;
        }
        if (namedAccessCombination.getIcal()) {
            retval |= UserConfiguration.ICAL;
        }
        if (namedAccessCombination.getInfostore()) {
            retval |= UserConfiguration.INFOSTORE;
        }
        if (namedAccessCombination.getSyncml()) {
            retval |= UserConfiguration.MOBILITY;
        }
        if (namedAccessCombination.isMultipleMailAccounts()) {
            retval |= UserConfiguration.MULTIPLE_MAIL_ACCOUNTS;
        }
        if (namedAccessCombination.isOLOX20()) {
            retval |= UserConfiguration.OLOX20;
        }
        if (namedAccessCombination.isPublication()) {
            retval |= UserConfiguration.PUBLICATION;
        }
        if (namedAccessCombination.getReadCreateSharedFolders()) {
            retval |= UserConfiguration.READ_CREATE_SHARED_FOLDERS;
        }
        if (namedAccessCombination.isSubscription()) {
            retval |= UserConfiguration.SUBSCRIPTION;
        }
        if (namedAccessCombination.getTasks()) {
            retval |= UserConfiguration.TASKS;
        }
        if (namedAccessCombination.isUSM()) {
            retval |= UserConfiguration.USM;
        }
        if (namedAccessCombination.getVcard()) {
            retval |= UserConfiguration.VCARD;
        }
        if (namedAccessCombination.getWebdav()) {
            retval |= UserConfiguration.WEBDAV;
        }
        if (namedAccessCombination.getWebdavXml()) {
            retval |= UserConfiguration.WEBDAV_XML;
        }
        if (namedAccessCombination.getWebmail()) {
            retval |= UserConfiguration.WEBMAIL;
        }

        return retval;
    }

    /**
     * Property name black list REGEX. Taken from the oxsysreport
     */
    private static final Pattern PROPERTY_NAME_BLACK_LIST = Pattern.compile("[pP]assword[[:blank:]]*|[pP]asswd[[:blank:]]*|[sS]ecret[[:blank:]]*|[kK]ey[[:blank:]]*|secretSource[[:blank:]]*|secretRandom[[:blank:]]*|[sS]alt[[:blank:]]*|SSLKey(Pass|Name)[[:blank:]]*|[lL]ogin[[:blank:]]*|[uU]ser[[:blank:]]*");

    private static final List<String> PROPERTY_VALUE_BLACK_LIST_TOKENS = ImmutableList.of("user", "login", "password", "passwd", "secret", "key");

    private static boolean isBlacklisted(ConfigurationProperty property, Optional<Pattern> optionalAdditionalConfigCheckPattern) {
        if (PROPERTY_NAME_BLACK_LIST.matcher(property.getName()).find()) {
            return true;
        }

        // Optional<Boolean> found = optionalAdditionalConfigCheckPattern.map((p) -> Boolean.valueOf(p.matcher(property.getName()).find()));
        if (optionalAdditionalConfigCheckPattern.isPresent() && optionalAdditionalConfigCheckPattern.get().matcher(property.getName()).find()) {
            return true;
        }

        String value = Strings.asciiLowerCase(property.getValue());
        StringBuilder tokenBuilder = new StringBuilder();
        for (String token : PROPERTY_VALUE_BLACK_LIST_TOKENS) {
            tokenBuilder.setLength(0);
            if (value.indexOf(tokenBuilder.append(token).append('=').toString()) >= 0) {
                return true;
            }
            tokenBuilder.setLength(0);
            if (value.indexOf(tokenBuilder.append(token).append(':').toString()) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public List<UserProperty> getUserConfigurationSource(final Context ctx, final User user, final String searchPattern, final Credentials credentials) throws RemoteException, InvalidDataException, StorageException, InvalidCredentialsException, NoSuchUserException {
        if (user == null) {
            throw new InvalidDataException("Invalid user id.");
        }
        if (ctx == null) {
            throw new InvalidDataException("Invalid context id.");
        }

        Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;

        try {
            basicauth.doAuthentication(auth, ctx);
            contextcheck(ctx);
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            final CapabilityService capabilityService = AdminServiceRegistry.getInstance().getService(CapabilityService.class);
            if (capabilityService == null) {
                log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "CapabilityService absent. Unable to retrieve user configuration.");
                return Collections.emptyList();
            }

            Optional<Pattern> optionalAdditionalConfigCheckPattern = Optional.empty();
            try {
                String regex = AdminProperties.optScopedProperty(AdminProperties.User.ADDITIONAL_CONFIG_CHECK_REGEX, PropertyScope.propertyScopeForDefaultSearchPath(user_id, ctx.getId().intValue()), String.class);
                optionalAdditionalConfigCheckPattern = Optional.ofNullable(Strings.isEmpty(regex) ? null : Pattern.compile(regex.trim()));
            } catch (PatternSyntaxException e) {
                log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), e, "Unable to compile the value of the '{}' property to a regular expression.", AdminProperties.User.ADDITIONAL_CONFIG_CHECK_REGEX);
            }

            List<ConfigurationProperty> capabilitiesSource = capabilityService.getConfigurationSource(user_id, ctx.getId().intValue(), searchPattern);
            List<UserProperty> userProperties = new ArrayList<>(capabilitiesSource.size());
            for (ConfigurationProperty property : capabilitiesSource) {
                String value = isBlacklisted(property, optionalAdditionalConfigCheckPattern) ? "<OBFUSCATED>" : property.getValue();
                userProperties.add(new UserProperty(property.getScope(), property.getName(), value, property.getMetadata()));
            }

            Collections.sort(userProperties, OXUserPropertySorter.getInstance());

            return userProperties;
        } catch (OXException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    /**
     *
     * {@inheritDoc}
     *
     * @throws InvalidDataException
     */
    @Override
    public Map<String, Map<String, Set<String>>> getUserCapabilitiesSource(Context ctx, User user, Credentials credentials) throws RemoteException, InvalidDataException, StorageException, InvalidCredentialsException, NoSuchUserException {
        if (user == null) {
            throw new InvalidDataException("Invalid user id.");
        }
        if (ctx == null) {
            throw new InvalidDataException("Invalid context id.");
        }

        Map<String, Map<String, Set<String>>> capabilitiesSource = new HashMap<>();

        Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;

        try {
            basicauth.doAuthentication(auth, ctx);
            contextcheck(ctx);
            final int user_id = user.getId().intValue();
            if (!tool.existsUser(ctx, user_id)) {
                throw new NoSuchUserException("No such user " + user_id + " in context " + ctx.getId());
            }

            final CapabilityService capabilityService = AdminServiceRegistry.getInstance().getService(CapabilityService.class);
            if (capabilityService == null) {
                log(LogLevel.WARNING, LOGGER, credentials, ctx.getIdAsString(), String.valueOf(user_id), null, "CapabilityService absent. Unable to retrieve user configuration.");
                return capabilitiesSource;
            }
            capabilitiesSource = capabilityService.getCapabilitiesSource(user_id, ctx.getId().intValue());
            return capabilitiesSource;
        } catch (OXException e) {
            throw logAndReturnException(LOGGER, StorageException.wrapForRMI(e), credentials, ctx.getIdAsString(), String.valueOf(user.getId()));
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, ctx, user);
            throw e;
        }
    }

    @Override
    public User[] listByAliasDomain(Context context, String aliasDomain, Credentials credentials, final Integer length, final Integer offset) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException {
        try {
            if (aliasDomain == null) {
                throw new InvalidDataException("Invalid alias domain");
            }
            if (context == null) {
                throw new InvalidDataException("Invalid context id.");
            }

            Credentials auth = credentials == null ? new Credentials(EMPTY_STRING, EMPTY_STRING) : credentials;
            basicauth.doAuthentication(auth, context);
            contextcheck(context);
            return oxu.listUsersByAliasDomain(context, aliasDomain, length, offset);
        } catch (Throwable e) {
            logAndEnhanceException(e, credentials, context, null);
            throw e;
        }
    }

    @Override
    public void delete(Context ctx, User[] users, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        delete(ctx, users, null, auth);
    }

    @Override
    public void delete(Context ctx, User user, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchUserException {
        delete(ctx, user, null, auth);
    }
}
