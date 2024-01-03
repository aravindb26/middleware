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
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.admin.soap.resource.dataobjects.Context;
import com.openexchange.admin.soap.resource.dataobjects.Credentials;
import com.openexchange.admin.soap.resource.dataobjects.Resource;
import com.openexchange.admin.soap.resource.soap.Change;
import com.openexchange.admin.soap.resource.soap.DatabaseUpdateException_Exception;
import com.openexchange.admin.soap.resource.soap.Delete;
import com.openexchange.admin.soap.resource.soap.InvalidCredentialsException_Exception;
import com.openexchange.admin.soap.resource.soap.InvalidDataException_Exception;
import com.openexchange.admin.soap.resource.soap.NoSuchContextException_Exception;
import com.openexchange.admin.soap.resource.soap.NoSuchResourceException_Exception;
import com.openexchange.admin.soap.resource.soap.OXResourceService;
import com.openexchange.admin.soap.resource.soap.OXResourceServicePortType;
import com.openexchange.admin.soap.resource.soap.RemoteException_Exception;
import com.openexchange.admin.soap.resource.soap.StorageException_Exception;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.test.pool.ProvisioningExceptionCode;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.test.common.test.pool.TestUser;



/**
 * {@link SoapResourceService}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v8.0.0
 */
public class SoapResourceService {

    private static final Logger LOG = LoggerFactory.getLogger(SoapResourceService.class);

    private static SoapResourceService INSTANCE;

    private final OXResourceService oxResourceService;
    private final OXResourceServicePortType oxResourceServicePortType;

    private final Credentials soapContextCreds;

    /**
     * Gets the {@link SoapContextService}
     *
     * @return The {@link SoapContextService}
     * @throws MalformedURLException In case service can't be initialized
     */
    public static SoapResourceService getInstance() throws MalformedURLException {
        if (INSTANCE == null) {
            synchronized (SoapResourceService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SoapResourceService();
                }
            }
        }
        return INSTANCE;
    }

    private SoapResourceService() throws MalformedURLException {
        this.oxResourceService = new OXResourceService(new URL(SoapProvisioningService.getSOAPHostUrl(), "/webservices/OXResourceService?wsdl"));
        this.oxResourceServicePortType = oxResourceService.getOXResourceServiceHttpSoap12Endpoint();
        this.soapContextCreds = createCreds(ProvisioningUtils.CTX_ADMIN, ProvisioningUtils.CTX_SECRET);
    }

    /**
     * Creates a resource in the given context
     *
     * @param cid The context ID the resource shall be created in
     * @param createdBy The class name that creates the resource
     * @return The created resource
     * @throws OXException If resource can't be created
     */
    public Resource create(int cid, String createdBy) throws OXException {
        Context ctx = contextForId(cid);
        Resource resource = createResourceObject(cid);
        return create(ctx, resource, createdBy);
    }

    /**
     * Creates a resource in the given context
     *
     * @param ctx The context the resource shall be created in
     * @param resource The resource to be created
     * @param createdBy The class name that creates the resource
     * @return The created resource
     * @throws OXException If resource can't be created
     */
    public Resource create(Context ctx, Resource resource, String createdBy) throws OXException {
        SoapProvisioningService.setPodHeader(oxResourceServicePortType, createdBy);
        try {
            return oxResourceServicePortType.create(ctx, resource, soapContextCreds);
        } catch (StorageException_Exception | InvalidDataException_Exception | DatabaseUpdateException_Exception |
                RemoteException_Exception | NoSuchContextException_Exception | InvalidCredentialsException_Exception e) {
            LOG.info("Unable to create resource.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_CREATE_USER.create(resource.getId());
        }
    }

    /**
     * Update a resource in a given context
     *
     * @param ctx The context the resource shall be updated in
     * @param resource The resource to be updated
     * @param changedBy The class name that changes the resource
     * @throws OXException If resource can't be updated
     */
    public void change(Context ctx, Resource resource, String changedBy) throws OXException {
        Change toChange = new Change();
        toChange.setCtx(ctx);
        toChange.setRes(resource);
        toChange.setAuth(soapContextCreds);

        SoapProvisioningService.setPodHeader(oxResourceServicePortType, changedBy);
        try {
            oxResourceServicePortType.change(toChange);
        } catch (StorageException_Exception | InvalidDataException_Exception | NoSuchContextException_Exception |
                InvalidCredentialsException_Exception | RemoteException_Exception | NoSuchResourceException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to update resource.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_UPDATE_USER.create(resource.getId());
        }
    }

    /**
     * Deletes a resource in a given context
     *
     * @param ctx The context the resource shall be deleted in
     * @param resource The resource to be deleted
     * @param deletedBy The class name that deletes the resource
     * @throws OXException If resource can't be deleted
     */
    public void delete(Context ctx, Resource resource, String deletedBy) throws OXException {
        Delete toDelete = new Delete();
        toDelete.setCtx(ctx);
        toDelete.setRes(resource);
        toDelete.setAuth(soapContextCreds);

        SoapProvisioningService.setPodHeader(oxResourceServicePortType, deletedBy);
        try {
            oxResourceServicePortType.delete(toDelete);
        } catch (StorageException_Exception | InvalidDataException_Exception | NoSuchContextException_Exception |
                RemoteException_Exception | InvalidCredentialsException_Exception | NoSuchResourceException_Exception | DatabaseUpdateException_Exception e) {
            LOG.info("Unable to delete resource.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_DELETE_USER.create(resource.getId());
        }
    }

    /**
     * Get resource data from the server
     *
     * @param cid The context ID
     * @param resourceId The resource ID
     * @return The resource
     * @throws OXException In case of error
     */
    public TestUser get(int cid, int resourceId) throws OXException {
        Context ctx = contextForId(cid);
        Resource resource = createResourceObject(cid);
        resource.setId(I(resourceId));
        try {
            Resource loaded = oxResourceServicePortType.getData(ctx, resource, soapContextCreds);
            return new TestUser(loaded.getName(), ctx.getName(), null, I(resourceId), I(cid), Optional.empty(), null);
        } catch (StorageException_Exception | InvalidDataException_Exception | DatabaseUpdateException_Exception | RemoteException_Exception | NoSuchContextException_Exception | InvalidCredentialsException_Exception | NoSuchResourceException_Exception e) {
            LOG.info("Unable to create resource.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_GET_USER.create(resource.getId());
        }
    }

    protected static Resource createResourceObject(int cid) {
        Resource resource = new Resource();
        String rand = UUID.randomUUID().toString();
        String name = "name_" + rand;
        resource.setDisplayname(name);
        resource.setName(name);
        resource.setEmail(ProvisioningUtils.getMailAddress(name, cid));
        return resource;
    }

    // -------------  helper methods --------------------
    // Do not summarize. Methods cannot be simplified for the most part
    // (e.g. com.openexchange.admin.soap.context.dataobjects.Context vs. com.openexchange.admin.soap.resource.dataobjects.Context)

    protected Context contextForId(int cid) {
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
