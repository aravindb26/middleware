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

package com.openexchange.test.common.test.pool.soap;

import static com.openexchange.java.Autoboxing.I;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.mail.internet.AddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.admin.soap.user.dataobjects.Context;
import com.openexchange.admin.soap.user.dataobjects.Credentials;
import com.openexchange.admin.soap.user.dataobjects.Entry;
import com.openexchange.admin.soap.user.dataobjects.SOAPMapEntry;
import com.openexchange.admin.soap.user.dataobjects.SOAPStringMap;
import com.openexchange.admin.soap.user.dataobjects.SOAPStringMapMap;
import com.openexchange.admin.soap.user.dataobjects.User;
import com.openexchange.admin.soap.user.dataobjects.UserModuleAccess;
import com.openexchange.admin.soap.user.soap.Change;
import com.openexchange.admin.soap.user.soap.ChangeByModuleAccess;
import com.openexchange.admin.soap.user.soap.ChangeCapabilities;
import com.openexchange.admin.soap.user.soap.DatabaseUpdateException_Exception;
import com.openexchange.admin.soap.user.soap.Delete;
import com.openexchange.admin.soap.user.soap.InvalidCredentialsException_Exception;
import com.openexchange.admin.soap.user.soap.InvalidDataException_Exception;
import com.openexchange.admin.soap.user.soap.NoSuchContextException_Exception;
import com.openexchange.admin.soap.user.soap.NoSuchUserException_Exception;
import com.openexchange.admin.soap.user.soap.OXUserService;
import com.openexchange.admin.soap.user.soap.OXUserServicePortType;
import com.openexchange.admin.soap.user.soap.RemoteException_Exception;
import com.openexchange.admin.soap.user.soap.StorageException_Exception;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.Mapping;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.test.common.test.TestUserConfig;
import com.openexchange.test.common.test.pool.ProvisioningExceptionCode;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccessFields;
import com.openexchange.test.common.test.pool.UserModuleAccessMapper;

/**
 * {@link SoapUserService}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public class SoapUserService {

    private static final Logger LOG = LoggerFactory.getLogger(SoapUserService.class);

    private static SoapUserService INSTANCE;

    private final OXUserService oxUserService;
    private final OXUserServicePortType oxUserServicePortType;

    private final Credentials soapContextCreds;

    /**
     * Gets the {@link SoapContextService}
     *
     * @return The {@link SoapContextService}
     * @throws MalformedURLException In case service can't be initialized
     */
    public static SoapUserService getInstance() throws MalformedURLException {
        if (INSTANCE == null) {
            synchronized (SoapUserService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SoapUserService();
                }
            }
        }
        return INSTANCE;
    }

    private SoapUserService() throws MalformedURLException {
        this.oxUserService = new OXUserService(new URL(SoapProvisioningService.getSOAPHostUrl(), "/webservices/OXUserService?wsdl"));
        this.oxUserServicePortType = oxUserService.getOXUserServiceHttpsEndpoint();
        this.soapContextCreds = createCreds(ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_SECRET);
    }

    /**
     * Creates a user in the given context
     *
     * @param ctx The context the user shall be created in
     * @param userToCreate The user to be created
     * @param createdBy The class name that creates the user
     * @return The created user
     * @throws OXException If user can't be created
     */
    public User create(Context ctx, User userToCreate, String createdBy) throws OXException {
        SoapProvisioningService.setPodHeader(oxUserServicePortType, createdBy);
        try {
            return oxUserServicePortType.create(ctx, userToCreate, soapContextCreds);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchContextException_Exception | RemoteException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to create test user.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_CREATE_USER.create(userToCreate.getId());
        }
    }

    /**
     * Creates a user in the given context and transforms it into a {@link TestUser}
     *
     * @param cid The context identifier of the context the user shall be created in
     * @param config The optional user config
     * @param userLogin The login name of the user.
     * @param createdBy The class name that creates the user
     * @return The created user
     * @throws OXException If user can't be created
     */
    public TestUser createTestUser(int cid, Optional<TestUserConfig> config, String userLogin, String createdBy) throws OXException {
        User userToCreate = createUser(cid, config, userLogin);
        Context ctx = contextForId(cid);

        SoapProvisioningService.setPodHeader(oxUserServicePortType, createdBy);
        User created = null;
        try {
            created = oxUserServicePortType.create(ctx, userToCreate, soapContextCreds);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchContextException_Exception | RemoteException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to create test user.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_CREATE_USER.create(userToCreate.getId());
        }
        assert created != null;

        return new TestUser(created.getName(), ctx.getName(), created.getPassword(), created.getId(), I(cid), createdBy);
    }

    /**
     * Get user module access
     *
     * @param cid The context id
     * @param userID the user id
     * @return The module access
     * @throws OXException If user module access can't be changed
     */
    public com.openexchange.test.common.test.pool.UserModuleAccess getModuleAccess(int cid, int userID) throws OXException {
        User user = new User();
        user.setId(I(userID));

        try {
            /*
             * Get current access
             */
            UserModuleAccess soapModuleAccess = oxUserServicePortType.getModuleAccess(contextForId(cid), user, soapContextCreds);

            com.openexchange.test.common.test.pool.UserModuleAccess access = new com.openexchange.test.common.test.pool.UserModuleAccess();
            for (UserModuleAccessFields setField : SoapUserModuleAccessMapper.getInstance().getAssignedFields(soapModuleAccess)) {
                Mapping<Boolean, com.openexchange.test.common.test.pool.UserModuleAccess> mapping = UserModuleAccessMapper.getInstance().getMappings().get(setField);
                Mapping<Boolean, UserModuleAccess> soapMapping = SoapUserModuleAccessMapper.getInstance().getMappings().get(setField);
                if (null != soapMapping && null != mapping) {
                    mapping.set(access, soapMapping.get(soapModuleAccess));
                }
            }
            return access;
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception | NoSuchContextException_Exception | RemoteException_Exception | NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to get module access.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_USER.create(user.getId());
        }
    }

    /**
     * Updates an existing user
     *
     * @param cid The context ID
     * @param userID The user ID
     * @param config Additional user config
     * @param changedBy The class name that changes the user
     * @throws OXException If user can't be updated
     */
    void changeUser(int cid, int userID, Optional<Map<String, String>> config, String changedBy) throws OXException {
        User user = new User();
        user.setId(I(userID));
        if (config.isEmpty()) {
            LOG.warn("No changes found. Skipping update");
            return;
        }
        user.setUserAttributes(convert(config.get()));
        change(contextForId(cid), user, changedBy);
    }

    /**
     * Updates user capabilities
     *
     * @param cid The context id
     * @param userID The user id
     * @param capsToAdd Capabilities to add
     * @param capsToRemove Capabilities to remove
     * @param capsToDrop Capabilities to drop
     * @param changedBy The class name that changes the capabilities
     * @throws OXException If user can't be updated
     */
    void changeCapabilities(int cid, int userID, Set<String> capsToAdd, Set<String> capsToRemove, Set<String> capsToDrop, String changedBy) throws OXException {
        User user = new User();
        user.setId(I(userID));

        ChangeCapabilities changeCapabilities = new ChangeCapabilities();
        changeCapabilities.setAuth(soapContextCreds);
        changeCapabilities.setUser(user);
        changeCapabilities.setCtx(contextForId(cid));
        changeCapabilities.setCapsToAdd(String.join(",", capsToAdd));
        changeCapabilities.setCapsToRemove(String.join(",", capsToRemove));
        changeCapabilities.setCapsToDrop(String.join(",", capsToDrop));

        SoapProvisioningService.setPodHeader(oxUserServicePortType, changedBy);
        try {
            oxUserServicePortType.changeCapabilities(changeCapabilities);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception | NoSuchContextException_Exception |
                RemoteException_Exception | NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to update user capability.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_USER.create(user.getId());
        }
    }

    /**
     * Updates user module access
     *
     * @param cid The context id
     * @param userID the user id
     * @param moduleAccess The module access to set
     * @param changedBy The class name that changes the module access
     * @throws OXException If user module access can't be changed
     */
    public void changeModuleAccess(int cid, int userID, com.openexchange.test.common.test.pool.UserModuleAccess moduleAccess, String changedBy) throws OXException {
        User user = new User();
        user.setId(I(userID));

        SoapProvisioningService.setPodHeader(oxUserServicePortType, changedBy);
        try {
            /*
             * Get current access
             */
            UserModuleAccess soapModuleAccess = oxUserServicePortType.getModuleAccess(contextForId(cid), user, soapContextCreds);
            /*
             * Updated fields as needed
             */
            for (UserModuleAccessFields setField : UserModuleAccessMapper.getInstance().getAssignedFields(moduleAccess)) {
                Mapping<Boolean, UserModuleAccess> soapMapping = SoapUserModuleAccessMapper.getInstance().getMappings().get(setField);
                Mapping<Boolean, com.openexchange.test.common.test.pool.UserModuleAccess> umaMapping = UserModuleAccessMapper.getInstance().getMappings().get(setField);
                if (null != soapMapping && null != umaMapping) {
                    soapMapping.set(soapModuleAccess, umaMapping.get(moduleAccess));
                }
            }
            /*
             * Finally, perform update
             */
            ChangeByModuleAccess changeByModuleAccess = new ChangeByModuleAccess();
            changeByModuleAccess.setUser(user);
            changeByModuleAccess.setCtx(contextForId(cid));
            changeByModuleAccess.setAuth(soapContextCreds);
            changeByModuleAccess.setModuleAccess(soapModuleAccess);

            oxUserServicePortType.changeByModuleAccess(changeByModuleAccess);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception | NoSuchContextException_Exception | RemoteException_Exception | NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to get module access.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_USER.create(user.getId());
        }
    }

    /**
     * Updates an existing user
     *
     * @param ctx The context the user shall be deleted in
     * @param user The user to be changed
     * @param changedBy The class name that changes the user
     * @throws OXException If user can't be updated
     */
    public void change(Context ctx, User user, String changedBy) throws OXException {
        Change toChange = new Change();
        toChange.setCtx(ctx);
        toChange.setUsrdata(user);
        toChange.setAuth(soapContextCreds);

        SoapProvisioningService.setPodHeader(oxUserServicePortType, changedBy);
        try {
            oxUserServicePortType.change(toChange);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | NoSuchContextException_Exception |
                InvalidDataException_Exception | RemoteException_Exception | NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to update user.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_USER.create(user.getId());
        }
    }

    /**
     * Deletes a user
     *
     * @param ctx The context the user shall be deleted in
     * @param userId The user to be changed
     * @param deletedBy The class name that deletes the user
     * @throws OXException If user can't be deleted
     */
    public void delete(int contextId, int userId, String deletedBy) throws OXException {
        User user = new User();
        user.setId(I(userId));
        delete(contextForId(contextId), user, deletedBy);
    }

    /**
     * Deletes a user
     *
     * @param ctx The context the user shall be deleted in
     * @param user The user to be changed
     * @param deletedBy The class name that deletes the user
     * @throws OXException If user can't be deleted
     */
    public void delete(Context ctx, User user, String deletedBy) throws OXException {
        Delete toDelete = new Delete();
        toDelete.setCtx(ctx);
        toDelete.setUser(user);
        toDelete.setAuth(soapContextCreds);

        SoapProvisioningService.setPodHeader(oxUserServicePortType, deletedBy);
        try {
            oxUserServicePortType.delete(toDelete);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchContextException_Exception | RemoteException_Exception | NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to delete user.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_DELETE_USER.create(user.getId());
        }
    }

    /**
     * Creates a new {@link com.openexchange.admin.soap.context.dataobjects.Context} object with the specified id
     * and max quota
     *
     * @param contextId The context identifier
     * @param maxQuota The maximum quota of the context
     * @param optConfig The optional ctx config
     * @return The new {@link com.openexchange.admin.soap.context.dataobjects.Context} object
     */
    public Context createContext(int contextId, Long maxQuota, Optional<Map<String, String>> optConfig) {
        Context context = new Context();
        context.setId(I(contextId));
        context.setName(ProvisioningUtils.getContextName(contextId));
        context.setMaxQuota(maxQuota);

        optConfig.ifPresent((c) -> {
            SOAPStringMapMap soapStringMapMap = convert(c);
            context.setUserAttributes(soapStringMapMap);
        });
        return context;
    }

    /**
     * Creates a new random {@link com.openexchange.admin.soap.context.dataobjects.User} with only the mandatory fields and a optional config
     *
     * @param cid The context id
     * @param config The optional config
     * @param userLogin The login name of the user
     * @param quota An optional user quota
     * @return The {@link com.openexchange.admin.soap.context.dataobjects.User} object
     * @throws OXException If user can't be created
     */
    private User createUser(int cid, Optional<TestUserConfig> config, String userLogin) throws OXException {
        String login = userLogin != null ? userLogin : "login_" + UUID.randomUUID();
        String pw = ProvisioningUtils.USER_SECRET;
        User user = createUser(login, pw, login, login, login, ProvisioningUtils.getMailAddress(login, cid), config);
        return user;
    }

    /**
     * Creates a {@link com.openexchange.admin.soap.user.dataobjects.User} object from the given data
     *
     * @param name
     * @param passwd
     * @param displayName
     * @param givenName
     * @param surname
     * @param email
     * @param config
     * @param quota
     * @return The {@link com.openexchange.admin.soap.user.dataobjects.User} object
     * @throws OXException If user can't be created
     */
	@SuppressWarnings("unused")
	private User createUser(String name, String passwd, String displayName, String givenName, String surname,
			String email, Optional<TestUserConfig> config) throws OXException {
		Objects.requireNonNull(name);
		Objects.requireNonNull(passwd);
		Objects.requireNonNull(displayName);
		Objects.requireNonNull(givenName);
		Objects.requireNonNull(surname);
		Objects.requireNonNull(email);
		// Check for valid address
		try {
			new QuotedInternetAddress(email);
		} catch (AddressException e) {
			LOG.info("Unable to parse email address.", e);
			throw ProvisioningExceptionCode.UNABLE_TO_PARSE_ADDRESS.create(email);
		}

		User user = new User();
		user.setName(name);
		user.setPassword(passwd);
		user.setDisplayName(displayName);
		user.setGivenName(givenName);
		user.setSurName(surname);
		user.setPrimaryEmail(email);
		user.setEmail1(email);
		user.setImapLogin(email);

		Optional<Long> quota = config.isPresent() ? config.get().optQuota() : Optional.empty();
		Optional<List<String>> aliases = config.isPresent() ? config.get().optAliases() : Optional.empty();
		Optional<List<String>> fakeAliases = config.isPresent() ? config.get().optFakeAliases() : Optional.empty();

		if (!aliases.isPresent() && fakeAliases.isPresent()) {
			List<String> aliasList = new ArrayList<>();
			String domain = email.substring(email.indexOf("@"));
			for (String alias : fakeAliases.get()) {
				aliasList.add(alias.concat(domain));
			}
			user.setAliases(aliasList);
		}

		quota.ifPresent(user::setMaxQuota);
		aliases.ifPresent(user::setAliases);

		return user;
	}


    // -------------  helper methods --------------------
    // Do not summarize. Methods cannot be simplified for the most part
    // (e.g. com.openexchange.admin.soap.context.dataobjects.Context vs. com.openexchange.admin.soap.user.dataobjects.Context)

    private SOAPStringMapMap convert(Map<String, String> config) {
        SOAPStringMapMap soapStringMapMap = new SOAPStringMapMap();
        SOAPStringMap soapStringMap = new SOAPStringMap();

        config.entrySet().forEach(e -> {
            Entry entry = new Entry() {{
                setKey(e.getKey());
                setValue(e.getValue());
            }};
            soapStringMap.getEntries().add(entry);
        });

        SOAPMapEntry soapMapEntry = new SOAPMapEntry(){{
            setKey("config");
            setValue(soapStringMap);
        }};

        soapStringMapMap.getEntries().add(soapMapEntry);
        return soapStringMapMap;
    }

    private Context contextForId(int cid) {
        Context context = new Context();
        context.setId(I(cid));
        context.setName(ProvisioningUtils.getContextName(cid));
        return context;
    }

    /**
     * Creates new credentials based on the given paramters
     *
     * @param login The login name to be used
     * @param password The password to be used
     * @return The credential obj
     */
    private Credentials createCreds(String login, String password) {
        Credentials creds = new Credentials();
        creds.setLogin(login);
        creds.setPassword(password);
        return creds;
    }
}
