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

package com.openexchange.objectusecount.impl;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.objectusecount.AbstractArguments;
import com.openexchange.objectusecount.FolderId2ObjectIdsMapping;
import com.openexchange.objectusecount.BatchIncrementArguments.ObjectAndFolder;
import com.openexchange.objectusecount.BatchIncrementArguments.ObjectFolderAndModuleForAccount;
import com.openexchange.objectusecount.IncrementArguments;
import com.openexchange.objectusecount.ObjectUseCountExceptionCode;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.objectusecount.SetArguments;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link ObjectUseCountServiceImpl}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class ObjectUseCountServiceImpl implements ObjectUseCountService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ObjectUseCountServiceImpl.class);

    /** The service look-up */
    final ServiceLookup services;

    /** Service handling legacy use counts */
    final LegacyUseCountService legacyService;

    /** Service handling generic use counts */
    final GenericUseCountService genericService;

    /**
     * Initializes a new {@link ObjectUseCountServiceImpl}.
     *
     * @param services The service look-up
     */
    public ObjectUseCountServiceImpl(ServiceLookup services) {
        super();
        this.services = services;
        this.legacyService = new LegacyUseCountService(services);
        this.genericService = new GenericUseCountService(services);
    }

    /**
     * Checks if given module and account identifier refer to the internal contact storage.
     *
     * @param module The module identifier
     * @param account The account identifier
     * @return <code>true</code> if module and account identifier refer to the internal contact storage; otherwise <code>false</code>
     */
    static boolean useLegacyStorage(int module, int account) {
        return CONTACT_MODULE == module && DEFAULT_ACCOUNT == account;
    }

    @Override
    public Map<String, Map<String, Integer>> getUseCounts(Session session, int moduleId, int accountId, FolderId2ObjectIdsMapping folderId2objectIds) throws OXException {
        if (folderId2objectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> f2objs = folderId2objectIds.getFolderId2ObjectIds();
        if (useLegacyStorage(moduleId, accountId)) {
            return getLegacyUseCounts(session, f2objs);
        }
        Map<String, Map<String, Integer>> useCountsPerFolderId = new HashMap<String, Map<String, Integer>>(f2objs.size());
        for (Map.Entry<String, List<String>> entry : f2objs.entrySet()) {
            Map<String, Integer> useCounts = genericService.getUseCount(session, moduleId, accountId, entry.getKey(), entry.getValue());
            if (null != useCounts && 0 < useCounts.size()) {
                useCountsPerFolderId.put(entry.getKey(), useCounts);
            }
        }
        return useCountsPerFolderId;
    }

    @Override
    public void incrementUseCount(final Session session, final IncrementArguments arguments) throws OXException {
        try {
            Task<Void> task = new IncrementObjectUseCountTask(arguments, session, this);
            if (doPerformAsynchronously(arguments)) {
                // Execute asynchronously; as a new connection is supposed to be fetched and no error should be signaled; thus "fire & forget"
                ThreadPools.submitElseExecute(task);
            } else {
                task.call();
            }
        } catch (OXException e) {
            if (arguments.isThrowException()) {
                throw e;
            }

            LOG.debug("Failed to increment object use count", e);
        } catch (RuntimeException e) {
            if (arguments.isThrowException()) {
                throw ObjectUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
            }

            LOG.debug("Failed to increment object use count", e);
        } catch (Exception e) {
            if (arguments.isThrowException()) {
                throw ObjectUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
            }

            LOG.debug("Failed to increment object use count", e);
        }
    }

    @Override
    public void setUseCount(final Session session, final SetArguments arguments) throws OXException {
        try {
            Task<Void> task = new AbstractTask<Void>() {

                @Override
                public Void call() throws Exception {
                    if (useLegacyStorage(arguments.getModule(), arguments.getAccountId())) {
                        legacyService.setUseCount(arguments.getFolderId(), arguments.getObjectId(), arguments.getValue(), session.getUserId(), session.getContextId(), arguments.getCon());
                    } else {
                        genericService.setUseCount(session, arguments.getModule(), arguments.getAccountId(), arguments.getFolder(), arguments.getObject(), arguments.getValue(), arguments.getCon());
                    }

                    return null;
                }
            };

            if (doPerformAsynchronously(arguments)) {
                // Execute asynchronously; as a new connection is supposed to be fetched and no error should be signaled; thus "fire & forget"
                ThreadPools.submitElseExecute(task);
            } else {
                task.call();
            }
        } catch (OXException e) {
            if (arguments.isThrowException()) {
                throw e;
            }
            LOG.debug("Failed to set object use count", e);
        } catch (RuntimeException e) {
            if (arguments.isThrowException()) {
                throw ObjectUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
            }
            LOG.debug("Failed to set object use count", e);
        } catch (Exception e) {
            if (arguments.isThrowException()) {
                throw ObjectUseCountExceptionCode.UNKNOWN.create(e, e.getMessage());
            }
            LOG.debug("Failed to set object use count", e);
        }
    }

    @Override
    public void deleteUseCountsForAccount(Session session, int moduleId, int accountId) throws OXException {
        genericService.deleteUseCountsForAccount(session, moduleId, accountId);
    }

    /**
     * Batch-increments all use counts for specified module, account, folder and object
     *
     * @param counts Counts to increment in legacy use count
     * @param genericCounts Counts to increment in generic use count
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con An existing writeable connection to database or <code>null</code> to fetch a new one
     * @throws OXException If batch incremenent fails
     */
    public void batchIncrementObjectUseCount(Map<ObjectAndFolder, Integer> counts, Map<ObjectFolderAndModuleForAccount, Integer> genericCounts, int userId, int contextId, Connection con) throws OXException {
        if (null != counts && false == counts.isEmpty()) {
            legacyService.batchIncrementUseCount(counts, userId, contextId, con);
        }
        if (null != genericCounts && false == genericCounts.isEmpty()) {
            genericService.batchIncrementUseCount(genericCounts, userId, contextId, con);
        }
    }

    /**
     * Checks if specified arguments allow to modify the use count asynchronously.
     *
     * @param arguments The arguments to check
     * @return <code>true</code> if asynchronous execution is possible; otherwise <code>false</code> for synchronous execution
     */
    private boolean doPerformAsynchronously(AbstractArguments arguments) {
        return null == arguments.getCon() && false == arguments.isThrowException();
    }

    /**
     * Gets the use counts from legacy use count service
     *
     * @param session The associated session
     * @param objectIdsPerFolderId Object identifier to get use count for mapped by folder identifier
     * @return Use count mapped per object mapped per folder
     * @throws OXException On error
     */
    private Map<String, Map<String, Integer>> getLegacyUseCounts(Session session, Map<String, List<String>> objectIdsPerFolderId) throws OXException {
        Map<String, Map<String, Integer>> useCountsPerFolderId = new HashMap<String, Map<String,Integer>>(objectIdsPerFolderId.size());
        for (Map.Entry<String, List<String>> entry : objectIdsPerFolderId.entrySet()) {
            int folderId = Strings.parseUnsignedInt(entry.getKey());
            if (0 >= folderId) {
                LOG.warn("Can't get use counts for non-numerical folder identifier {}", entry.getKey());
                continue;
            }
            Map<String, Integer> useCounts = new HashMap<String, Integer>();
            for (String value : entry.getValue()) {
                int objectId = Strings.parseUnsignedInt(value);
                if (0 >= folderId) {
                    LOG.warn("Can't get use count for non-numerical object identifier {}", value);
                    continue;
                }
                int useCount = legacyService.getUseCount(session, folderId, objectId);
                if (0 < useCount) {
                    useCounts.put(entry.getKey(), I(useCount));
                }
            }
            if (0 < useCounts.size()) {
                useCountsPerFolderId.put(entry.getKey(), useCounts);
            }
        }
        return useCountsPerFolderId;
    }

}
