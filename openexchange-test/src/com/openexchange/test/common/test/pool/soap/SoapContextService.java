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
import static com.openexchange.java.Autoboxing.i;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.empty;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.mail.internet.AddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.admin.soap.context.dataobjects.Context;
import com.openexchange.admin.soap.context.dataobjects.Credentials;
import com.openexchange.admin.soap.context.dataobjects.Entry;
import com.openexchange.admin.soap.context.dataobjects.SOAPMapEntry;
import com.openexchange.admin.soap.context.dataobjects.SOAPStringMap;
import com.openexchange.admin.soap.context.dataobjects.SOAPStringMapMap;
import com.openexchange.admin.soap.context.dataobjects.SchemaSelectStrategy;
import com.openexchange.admin.soap.context.dataobjects.User;
import com.openexchange.admin.soap.context.dataobjects.UserModuleAccess;
import com.openexchange.admin.soap.context.soap.Change;
import com.openexchange.admin.soap.context.soap.ChangeModuleAccess;
import com.openexchange.admin.soap.context.soap.ContextExistsException_Exception;
import com.openexchange.admin.soap.context.soap.DatabaseUpdateException_Exception;
import com.openexchange.admin.soap.context.soap.Delete;
import com.openexchange.admin.soap.context.soap.InvalidCredentialsException_Exception;
import com.openexchange.admin.soap.context.soap.InvalidDataException_Exception;
import com.openexchange.admin.soap.context.soap.NoSuchContextException_Exception;
import com.openexchange.admin.soap.context.soap.OXContextService;
import com.openexchange.admin.soap.context.soap.OXContextServicePortType;
import com.openexchange.admin.soap.context.soap.RemoteException_Exception;
import com.openexchange.admin.soap.context.soap.StorageException_Exception;
import com.openexchange.exception.OXException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.ProvisioningExceptionCode;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestContextPool;
import com.openexchange.test.common.test.pool.TestUser;

/**
 * {@link SoapContextService}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public class SoapContextService {

    private static final Logger LOG = LoggerFactory.getLogger(SoapContextService.class);

    private static SoapContextService INSTANCE;

    private final OXContextService oxContextService;
    private final OXContextServicePortType oxContextServicePortType;
    private final SchemaSelectStrategy schemaSelectStrategy = new SchemaSelectStrategy();

    private final Credentials soapMasterCreds;

    private final int MAX_RETRY = 3;

    /**
     * Gets the {@link SoapContextService}
     *
     * @return The {@link SoapContextService}
     * @throws MalformedURLException In case service can't be initialized
     */
    public static SoapContextService getInstance() throws MalformedURLException {
        if (INSTANCE == null) {
            synchronized (SoapContextService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SoapContextService(TestContextPool.getOxAdminMaster().getUser(), TestContextPool.getOxAdminMaster().getPassword());
                }
            }
        }
        return INSTANCE;
    }

    private SoapContextService(String oxAdminMasterUser, String oxAdminMasterPW) throws MalformedURLException {
        this.oxContextService = new OXContextService(new URL(SoapProvisioningService.getSOAPHostUrl(), "/webservices/OXContextService?wsdl"));
        this.oxContextServicePortType = oxContextService.getOXContextServiceHttpsEndpoint();
        this.soapMasterCreds = createCreds(oxAdminMasterUser, oxAdminMasterPW);
        this.schemaSelectStrategy.setStrategy("automatic");
    }

    /**
     * Creates a context and transforms it into a {@link TestContext}
     *
     * @param config The configuration to pass for context
     * @param createdBy The class name that creates the context
     * @return The context as {@link TestContext}
     * @throws OXException If context can't be created
     */
    public TestContext createTestContext(TestContextConfig config, String createdBy) throws OXException {
        Context ctx = createContext(config.optMaxQuota().orElse(ProvisioningUtils.DEFAULT_MAX_QUOTA), config.optConfig(), config.optTaxonomyType());
        User admin_user = createUser(ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_SECRET, ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.generateAdminMailAddress(), empty(), empty());
        UserModuleAccess userModuleAccess = config.optAccess().map(usa -> toSoap(usa)).orElseGet(() -> enableAll(new UserModuleAccess()));

        SoapProvisioningService.setPodHeader(oxContextServicePortType, createdBy);
        Context result;
        int attempts = 0;
        while ( MAX_RETRY > attempts ) {
            try {
                result = callCreateContext(ctx, admin_user, userModuleAccess);

                // Creating and setting the context-name not during context-creation but
                // afterwards, because the server might have ignored the given cid ("autocontextid" is
                // active)and we still want that the auto-cid is part of the context-name
                result.setName(ProvisioningUtils.getContextName(i(result.getId())));
                change(result, createdBy);

                TestUser testUser = new TestUser(admin_user.getName(), result.getName(), admin_user.getPassword(), I(2), result.getId(), createdBy);
                return new TestContext(i(result.getId()), ProvisioningUtils.getContextName(i(result.getId())), testUser, createdBy);
            } catch (Exception e) {
                LOG.info(String.format("Unable to create context for test. Retry %d more times.", I(MAX_RETRY - attempts)), e);
                if (++attempts == MAX_RETRY) {
                    throw ProvisioningExceptionCode.UNABLE_TO_CREATE_CONTEXT.create(e);
                }
            }
        }
        return null;
    }

    private Context callCreateContext(Context ctx, User admin_user, UserModuleAccess userModuleAccess) throws StorageException_Exception, InvalidCredentialsException_Exception, InvalidDataException_Exception, ContextExistsException_Exception, RemoteException_Exception {
        return oxContextServicePortType.createModuleAccess(ctx, admin_user, userModuleAccess, soapMasterCreds, schemaSelectStrategy);
    }

    @SuppressWarnings({ "deprecation" })
    private UserModuleAccess toSoap(com.openexchange.test.common.test.pool.UserModuleAccess access) {

        UserModuleAccess userModuleAccess = new UserModuleAccess();
        userModuleAccess.setCalendar(access.getCalendar());
        userModuleAccess.setContacts(access.getContacts());
        userModuleAccess.setDelegateTask(access.getDelegateTask());
        userModuleAccess.setPublicFolderEditable(access.getEditPublicFolders());
        userModuleAccess.setIcal(access.getIcal());
        userModuleAccess.setInfostore(access.getInfostore());
        userModuleAccess.setReadCreateSharedFolders(access.getReadCreateSharedFolders());
        userModuleAccess.setSyncml(access.getSyncml());
        userModuleAccess.setTasks(access.getTasks());
        userModuleAccess.setVcard(access.getVcard());
        userModuleAccess.setWebdav(access.getWebdav());
        userModuleAccess.setWebdavXml(access.getWebdavXml());
        userModuleAccess.setWebmail(access.getWebmail());
        userModuleAccess.setEditGroup(access.getEditGroup());
        userModuleAccess.setEditResource(access.getEditResource());
        userModuleAccess.setEditPassword(access.getEditPassword());
        userModuleAccess.setCollectEmailAddresses(access.isCollectEmailAddresses());
        userModuleAccess.setMultipleMailAccounts(access.isMultipleMailAccounts());
        userModuleAccess.setSubscription(access.isSubscription());
        userModuleAccess.setPublication(access.isPublication());
        userModuleAccess.setActiveSync(access.isActiveSync());
        userModuleAccess.setUSM(access.isUSM());
        userModuleAccess.setGlobalAddressBookDisabled(access.isGlobalAddressBookDisabled());
        userModuleAccess.setPublicFolderEditable(access.isPublicFolderEditable());
        userModuleAccess.setOLOX20(access.isOLOX20());
        return userModuleAccess;
    }

    /**
     * Updates an existing context
     *
     * @param ctx The context to be changed
     * @param changedBy The class name that changes the context
     * @throws OXException If context can't be changed
     */
    public void change(Context ctx, String changedBy) throws OXException {
        Change toChange = new Change();
        toChange.setCtx(ctx);
        toChange.setAuth(soapMasterCreds);

        SoapProvisioningService.setPodHeader(oxContextServicePortType, changedBy);
        try {
            oxContextServicePortType.change(toChange);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchContextException_Exception | RemoteException_Exception e) {
            LOG.info("Unable to update context for test.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_CONTEXT.create(ctx.getId());
        }
    }

    /**
     * Changes the module access for an existing context
     *
     * @param cid The context identifier
     * @param access The changes {@link com.openexchange.test.common.test.pool.UserModuleAccess}
     * @param changedBy The class name that changes the module access
     * @throws OXException in the user module access couldn't be changed
     */
    public void changeModuleAccess(int cid, com.openexchange.test.common.test.pool.UserModuleAccess access, String changedBy) throws OXException {
        ChangeModuleAccess change = new ChangeModuleAccess();
        change.setAccess(toSoap(access));
        Context context = new Context();
        context.setId(I(cid));
        change.setCtx(context);
        change.setAuth(soapMasterCreds);
        SoapProvisioningService.setPodHeader(oxContextServicePortType, changedBy);
        try {
            oxContextServicePortType.changeModuleAccess(change);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception | NoSuchContextException_Exception | RemoteException_Exception e) {
            LOG.info("Unable to update context for test.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_CONTEXT.create(context.getId());
        }
    }

    /**
     * Deletes a context
     *
     * @param cid The context identifier
     * @param deletedBy The class name that deletes the context
     * @throws OXException If context can't be deleted
     */
    public void delete(int cid, String deletedBy) throws OXException {
        Delete toDelete = new Delete();
        toDelete.setCtx(contextForId(cid));
        toDelete.setAuth(soapMasterCreds);

        SoapProvisioningService.setPodHeader(oxContextServicePortType, deletedBy);
        try {
            oxContextServicePortType.delete(toDelete);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | NoSuchContextException_Exception |
                InvalidDataException_Exception | RemoteException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to delete test context.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_DELETE_CONTEXT.create(e, I(cid), e.getMessage());
        }
    }

    /**
     * Creates a new {@link Context} object with the specified max quota
     *
     * @param maxQuota  The maximum quota of the context
     * @param optConfig The optional ctx config
     * @param optTaxonomyType The optional taxonomy type of the context
     * @return The new {@link Context} object
     */
    public Context createContext(Long maxQuota, Optional<Map<String, String>> optConfig, Optional<String> optTaxonomyType) {
        Context context = new Context();
        context.setMaxQuota(maxQuota);
        Optional<SOAPStringMapMap> attributes = Optional.empty();
        if(optConfig.isPresent()) {
            attributes = Optional.ofNullable(convert(optConfig.get()));
        }

        if(optTaxonomyType.isPresent()) {
            SOAPMapEntry mapEntry = convert(optTaxonomyType.get());
            if (attributes.isPresent() == false) {
                attributes = Optional.of(new SOAPStringMapMap());
            }
            attributes.get().getEntries().add(mapEntry);
        }

        attributes.ifPresent(att -> context.setUserAttributes(att));
        return context;
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
     * @throws OXException
     */
    @SuppressWarnings("unused")
    private User createUser(String name, String passwd, String displayName, String givenName, String surname, String email, Optional<Map<String, String>> config, Optional<Long> quota) throws OXException {
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

        config.ifPresent((c) -> {
            SOAPStringMapMap soapStringMapMap = convert(c);
            user.setUserAttributes(soapStringMapMap);
        });
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

    private SOAPMapEntry convert(String taxonomyType) {
        Entry entry = new Entry();
        entry.setKey("types");
        entry.setValue(taxonomyType);
        SOAPStringMap stringMap = new SOAPStringMap();
        stringMap.setEntries(Collections.singletonList(entry));
        SOAPMapEntry mapEntry = new SOAPMapEntry();
        mapEntry.setKey("taxonomy");
        mapEntry.setValue(stringMap);
        return mapEntry;
    }

    Context contextForId(int cid) {
        Context context = new Context();
        context.setId(I(cid));
        context.setName(ProvisioningUtils.getContextName(cid));
        return context;
    }

    /**
     * Enable all modules on a userModuleAccess
     *
     * @param userModuleAccess
     * @return The given {@link UserModuleAccess}
     */
    @SuppressWarnings("deprecation")
    private UserModuleAccess enableAll(UserModuleAccess userModuleAccess) {
        userModuleAccess.setCalendar(TRUE);
        userModuleAccess.setContacts(TRUE);
        userModuleAccess.setDelegateTask(TRUE);
        userModuleAccess.setPublicFolderEditable(TRUE);
        userModuleAccess.setIcal(TRUE);
        userModuleAccess.setInfostore(TRUE);
        userModuleAccess.setReadCreateSharedFolders(TRUE);
        userModuleAccess.setSyncml(TRUE);
        userModuleAccess.setTasks(TRUE);
        userModuleAccess.setVcard(TRUE);
        userModuleAccess.setWebdav(TRUE);
        userModuleAccess.setWebdavXml(TRUE);
        userModuleAccess.setWebmail(TRUE);
        userModuleAccess.setEditGroup(TRUE);
        userModuleAccess.setEditResource(TRUE);
        userModuleAccess.setEditPassword(TRUE);
        userModuleAccess.setCollectEmailAddresses(TRUE);
        userModuleAccess.setMultipleMailAccounts(TRUE);
        userModuleAccess.setSubscription(TRUE);
        userModuleAccess.setPublication(TRUE);
        userModuleAccess.setActiveSync(TRUE);
        userModuleAccess.setUSM(TRUE);
        userModuleAccess.setGlobalAddressBookDisabled(Boolean.FALSE);
        userModuleAccess.setPublicFolderEditable(TRUE);
        userModuleAccess.setOLOX20(TRUE);
        return userModuleAccess;
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
