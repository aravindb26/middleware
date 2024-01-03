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

package com.openexchange.admin.contextrestore.rmi.impl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.admin.contextrestore.dataobjects.PoolIdSchemaAndVersionInfo;
import com.openexchange.admin.contextrestore.dataobjects.UpdateTaskInformation;
import com.openexchange.admin.contextrestore.osgi.Activator;
import com.openexchange.admin.contextrestore.parser.Parser;
import com.openexchange.admin.contextrestore.rmi.OXContextRestoreInterface;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException.Code;
import com.openexchange.admin.contextrestore.storage.interfaces.OXContextRestoreStorageInterface;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.impl.BasicAuthenticator;
import com.openexchange.admin.rmi.impl.OXCommonImpl;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;

/**
 * This class contains the implementation of the API defined in {@link OXContextRestoreInterface}
 *
 * @author <a href="mailto:dennis.sieben@open-xchange.com">Dennis Sieben</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>: Bugfix 20044
 */
public class OXContextRestore extends OXCommonImpl implements OXContextRestoreInterface {

    private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OXContextRestore.class);

    private static class RunParserResult {

        private final PoolIdSchemaAndVersionInfo result;
        private final UpdateTaskInformation updateTaskInfo;

        RunParserResult(PoolIdSchemaAndVersionInfo result, UpdateTaskInformation updateTaskInfo) {
            super();
            this.result = result;
            this.updateTaskInfo = updateTaskInfo;
        }

        PoolIdSchemaAndVersionInfo getResult() {
            return result;
        }

        UpdateTaskInformation getUpdateTaskInfo() {
            return updateTaskInfo;
        }

    }

    /** The reference for ConfigDB name */
    private static final AtomicReference<String> CONFIGDB_NAME = new AtomicReference<String>("configdb");

    /**
     * Sets the name of the ConfigDB.
     *
     * @param configDbName The name
     */
    public static void setConfigDbName(final String configDbName) {
        CONFIGDB_NAME.set(configDbName);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final BasicAuthenticator basicauth;

    public OXContextRestore() throws StorageException {
        super();
        basicauth = BasicAuthenticator.createNonPluginAwareAuthenticator();
    }

    @Override
    public String restore(final Context ctx, final String[] fileNames, final String optConfigDbName, final Credentials auth, final boolean dryrun) throws InvalidDataException, InvalidCredentialsException, StorageException, OXContextRestoreException, DatabaseUpdateException {
        try {
            doNullCheck(ctx, fileNames);
            for (final String filename : fileNames) {
                doNullCheck(filename);
            }
        } catch (InvalidDataException e) {
            LOG.error("One of the arguments for restore is null", e);
            throw e;
        }

        try {
            basicauth.doAuthentication(auth);
        } catch (InvalidCredentialsException e) {
            LOG.error("", e);
            throw e;
        }

        final OXToolStorageInterface storage = OXToolStorageInterface.getInstance();
        boolean contextExists = storage.existsContext(ctx);
        if (contextExists && storage.isLastContextInSchema(ctx)) {
            // The context which is supposed to be restored is the last one kept in associated database schema
            // Deleting it in further processing of context restoration might drop the schema
            // TODO: To be revised for v7.10.0
            throw new OXContextRestoreException(Code.LAST_CONTEXT_IN_SCHEMA, ctx.getIdAsString());
        }

        // Either such a context does not exist or it is not the last one in associated database schema
        LOG.info("Context: {}", ctx);
        LOG.info("Filenames: {}", java.util.Arrays.toString(fileNames));

        try {
            final HashMap<String, File> tempfilemap = new HashMap<String, File>();
            RunParserResult test = runParser(ctx, fileNames, optConfigDbName, null, tempfilemap);
            if (null == test.getResult()) {
                throw new OXContextRestoreException(Code.NO_CONFIGDB_FOUND);
            }
            if (null == test.getUpdateTaskInfo()) {
                // Trigger seconds round because the user database can be located before the configdb entries
                test = runParser(ctx, fileNames, optConfigDbName, test.getResult().getSchema(), tempfilemap);
                if (null == test.getUpdateTaskInfo()) {
                    // Still no user database found. Exiting
                    throw new OXContextRestoreException(Code.NO_USER_DATA_DB_FOUND);
                }
            }
            final PoolIdSchemaAndVersionInfo result = test.getResult();

            final OXContextRestoreStorageInterface instance = OXContextRestoreStorageInterface.getInstance();
            result.setUpdateTaskInformation(test.getUpdateTaskInfo());
            result.setTempfilemap(tempfilemap);

            if (dryrun) {
                return "Done nothing (dry run)";
            }

            final OXContextInterface contextInterface = Activator.getContextInterface();

            // Drop if such a context already exists
            if (contextExists) {
                try {
                    contextInterface.delete(ctx, auth);
                } catch (NoSuchContextException e) {
                    // As we check for the existence beforehand this exception should never occur. Nevertheless we will log this
                    LOG.error("FATAL", e);
                }
            }
            return instance.restorectx(ctx, result, getConfigDbName(optConfigDbName));
        } catch (StorageException e) {
            LOG.error("", e);
            throw e;
        } catch (IOException e) {
            LOG.error("", e);
            throw new OXContextRestoreException(Code.IO_EXCEPTION, e);
        } catch (SQLException e) {
            LOG.error("", e);
            throw new OXContextRestoreException(Code.DATABASE_OPERATION_ERROR, e, e.getMessage());
        } catch (OXContextRestoreException e) {
            LOG.error("", e);
            throw e;
        } catch (DatabaseUpdateException e) {
            LOG.error("", e);
            throw e;
        } catch (Exception e) {
            LOG.error("", e);
            throw new OXContextRestoreException(Code.UNEXPECTED_ERROR, e, e.getMessage());
        }
    }

    private RunParserResult runParser(final Context ctx, final String[] fileNames, final String optConfigDbName, String schema, Map<String, File> filemap) throws IOException, OXContextRestoreException {
        UpdateTaskInformation updateTaskInfo = null;
        PoolIdSchemaAndVersionInfo result = null;
        for (final String fileName : fileNames) {
            final PoolIdSchemaAndVersionInfo infoObject = Parser.start(ctx.getId().intValue(), fileName, optConfigDbName, schema, filemap);
            final UpdateTaskInformation updateTaskInformation = infoObject.getUpdateTaskInformation();
            if (null != updateTaskInformation) {
                updateTaskInfo = updateTaskInformation;
            }
            if (null != infoObject.getSchema() && -1 != infoObject.getPoolId()) {
                result = infoObject;
            }
        }
        return new RunParserResult(result, updateTaskInfo);
    }

    public static String getConfigDbName(final String optConfigDbName) {
        String configDbName = optConfigDbName;
        if (null == configDbName) {
            configDbName = CONFIGDB_NAME.get();
            if (null == configDbName) {
                configDbName = "configdb";
            }
        }
        return configDbName;
    }

}
