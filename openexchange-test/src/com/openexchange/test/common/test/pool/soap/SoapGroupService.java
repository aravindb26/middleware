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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.admin.soap.group.dataobjects.Context;
import com.openexchange.admin.soap.group.dataobjects.Credentials;
import com.openexchange.admin.soap.group.dataobjects.Entry;
import com.openexchange.admin.soap.group.dataobjects.Group;
import com.openexchange.admin.soap.group.dataobjects.SOAPMapEntry;
import com.openexchange.admin.soap.group.dataobjects.SOAPStringMap;
import com.openexchange.admin.soap.group.dataobjects.SOAPStringMapMap;
import com.openexchange.admin.soap.group.soap.Change;
import com.openexchange.admin.soap.group.soap.DatabaseUpdateException_Exception;
import com.openexchange.admin.soap.group.soap.Delete;
import com.openexchange.admin.soap.group.soap.InvalidCredentialsException_Exception;
import com.openexchange.admin.soap.group.soap.InvalidDataException_Exception;
import com.openexchange.admin.soap.group.soap.NoSuchContextException_Exception;
import com.openexchange.admin.soap.group.soap.NoSuchGroupException_Exception;
import com.openexchange.admin.soap.group.soap.NoSuchUserException_Exception;
import com.openexchange.admin.soap.group.soap.OXGroupService;
import com.openexchange.admin.soap.group.soap.OXGroupServicePortType;
import com.openexchange.admin.soap.group.soap.RemoteException_Exception;
import com.openexchange.admin.soap.group.soap.StorageException_Exception;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.test.pool.ProvisioningExceptionCode;
import com.openexchange.test.common.test.pool.ProvisioningUtils;

/**
 * {@link SoapGroupService}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public class SoapGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(SoapGroupService.class);

    private static SoapGroupService INSTANCE;

    private final OXGroupService oxGroupService;
    private final OXGroupServicePortType oxGroupServicePortType;

    private final Credentials soapContextCreds;

    /**
     * Gets the {@link SoapContextService}
     *
     * @return The {@link SoapContextService}
     * @throws MalformedURLException In case service can't be initialized
     */
    public static SoapGroupService getInstance() throws MalformedURLException {
        if (INSTANCE == null) {
            synchronized (SoapGroupService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SoapGroupService();
                }
            }
        }
        return INSTANCE;
    }

    private SoapGroupService() throws MalformedURLException {
        this.oxGroupService = new OXGroupService(new URL(SoapProvisioningService.getSOAPHostUrl(), "/webservices/OXGroupService?wsdl"));
        this.oxGroupServicePortType = oxGroupService.getOXGroupServiceHttpSoap11Endpoint();
        this.soapContextCreds = createCreds(ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_SECRET);
    }

    /**
     * Creates a group in the given context
     *
     * @param cid The context id the group shall be created in
     * @param optUserIds Optional user IDs that shall be added to the group
     * @param createdBy The class name that creates the group
     * @return The group
     * @throws OXException If group can't be created
     */
    public Group create(int cid, Optional<List<Integer>> optUserIds, String createdBy) throws OXException {
        Context ctx = contextForId(cid);
        Group group = createGroup(optUserIds);
        return create(ctx, group, createdBy);
    }


    /**
     * Creates a group in the given context
     *
     * @param ctx The context the group shall be created in
     * @param group The group to be created
     * @param createdBy The class name that creates the group
     * @return The group
     * @throws OXException If group can't be created
     */
    public Group create(Context ctx, Group group, String createdBy) throws OXException {
        SoapProvisioningService.setPodHeader(oxGroupServicePortType, createdBy);
        try {
            return oxGroupServicePortType.create(ctx, group, soapContextCreds);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchContextException_Exception | RemoteException_Exception | NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Could not create group.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_CREATE_GROUP.create(group.getId());
        }
    }

    /**
     * Updates a group in a given context
     *
     * @param ctx The context of the group
     * @param group The group to be changed
     * @param changedBy The class name that changes the group
     * @throws OXException If group can't be updated
     */
    public void change(Context ctx, Group group, String changedBy) throws OXException {
        Change toChange = new Change();
        toChange.setCtx(ctx);
        toChange.setGrp(group);
        toChange.setAuth(soapContextCreds);

        SoapProvisioningService.setPodHeader(oxGroupServicePortType, changedBy);
        try {
            oxGroupServicePortType.change(toChange);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchGroupException_Exception | NoSuchContextException_Exception | RemoteException_Exception |
                NoSuchUserException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to update group.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_GROUP.create(group.getId());
        }
    }

    /**
     * Deletes a group in a given context
     *
     * @param ctx The context of the group
     * @param group The group to be deleted
     * @param deletedBy The class name that deletes the group
     * @throws OXException If group can't be deleted
     */
    public void delete(Context ctx, Group group, String deletedBy) throws OXException {
        Delete toDelete = new Delete();
        toDelete.setCtx(ctx);
        toDelete.setGrp(group);
        toDelete.setAuth(soapContextCreds);

        SoapProvisioningService.setPodHeader(oxGroupServicePortType, deletedBy);
        try {
            oxGroupServicePortType.delete(toDelete);
        } catch (StorageException_Exception | InvalidCredentialsException_Exception | InvalidDataException_Exception |
                NoSuchGroupException_Exception | NoSuchContextException_Exception | RemoteException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to delete group.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_DELETE_GROUP.create(group.getId());
        }
    }


    /**
     * Creates a new group with the provides user id's
     *
     * @param optUserIds Optional user id's
     * @return The group
     */
    protected static Group createGroup(Optional<List<Integer>> optUserIds) {
        Group group = new Group();
        String rand = UUID.randomUUID().toString();
        String name = "name_" + rand;
        group.setDisplayname(name);
        group.setName(name);
        optUserIds.ifPresent(group::setMembers);
        return group;
    }

    /**
     * Creates a new {@link Context} object with the specified id
     * and max quota
     *
     * @param contextId The context identifier
     * @param maxQuota The maximum quota of the context
     * @param optConfig The optional ctx config
     * @return The new {@link Context} object
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


    // -------------  helper methods --------------------
    // Do not summarize. Methods cannot be simplified for the most part
    // (e.g. com.openexchange.admin.soap.context.dataobjects.Context vs. com.openexchange.admin.soap.group.dataobjects.Context)

    /**
     * Converts a map to a {@link SOAPStringMapMap}
     *
     * @param config The config to convert
     * @return The {@link SOAPStringMapMap}
     */
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

    /**
     * Creates a new context for the given id
     *
     * @param cid The context id
     * @return The new context
     */
    Context contextForId(int cid) {
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
    Credentials createCreds(String login, String password) {
        Credentials creds = new Credentials();
        creds.setLogin(login);
        creds.setPassword(password);
        return creds;
    }
}
