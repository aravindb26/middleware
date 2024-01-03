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

package com.openexchange.tools.oxfolder.memory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import com.openexchange.tools.oxfolder.OXFolderExceptionCode;

/**
 * {@link ConditionTreeMapManagement}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConditionTreeMapManagement {

    /** The logger constant */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConditionTreeMapManagement.class);

    private static final AtomicReference<ConditionTreeMapManagement> INSTANCE_REF = new AtomicReference<>();

    /**
     * Starts the {@link ConditionTreeMapManagement management} instance.
     */
    public static void startInstance() {
        stopInstance();
        ConditionTreeMapManagement instance = new ConditionTreeMapManagement();
        if (INSTANCE_REF.compareAndSet(null, instance)) {
            instance.start();
        }
    }

    /**
     * Stops the {@link ConditionTreeMapManagement management} instance.
     */
    public static void stopInstance() {
        ConditionTreeMapManagement mm = INSTANCE_REF.getAndSet(null);
        if (null != mm) {
            mm.stop();
        }
    }

    /**
     * Gets the {@link ConditionTreeMapManagement management} instance.
     *
     * @return The {@link ConditionTreeMapManagement management} instance
     */
    public static ConditionTreeMapManagement getInstance() {
        return INSTANCE_REF.get();
    }

    /**
     * Drops the map for given context identifier
     *
     * @param contextId The context identifier
     */
    public static void dropFor(int contextId) {
        ConditionTreeMapManagement mm = INSTANCE_REF.get();
        if (null != mm) {
            Integer iContextId = Integer.valueOf(contextId);
            CURRENT_LOADERS.remove(iContextId);
            mm.context2maps.invalidate(iContextId);
        }
    }

    /** Tracks currently loading condition tree maps for contexts */
    private static final ConcurrentMap<Integer, CacheLoader<Integer, ConditionTreeMap>> CURRENT_LOADERS = new ConcurrentHashMap<Integer, CacheLoader<Integer,ConditionTreeMap>>(8, 0.9F, 1);

    /*-
     * -------------------- Member stuff -----------------------------
     */

    private static final int TIME2LIVE = 360000; // 6 minutes time-to-live

    private final LoadingCache<Integer, ConditionTreeMap> context2maps;
    private final boolean enabled;
    private ScheduledTimerTask timerTask; // guarded by synchronized

    /**
     * Initializes a new {@link ConditionTreeMapManagement}.
     */
    private ConditionTreeMapManagement() {
        super();

        // Build up cache
        CacheLoader<Integer, ConditionTreeMap> cacheLoader = new CacheLoader<Integer, ConditionTreeMap>() {

            @Override
            public ConditionTreeMap load(Integer contextId) throws Exception {
                ConditionTreeMap newMap = null;
                for (boolean done = false; !done;) {
                    CURRENT_LOADERS.put(contextId, this);
                    try {
                        newMap = new ConditionTreeMap(contextId.intValue(), TIME2LIVE);
                        newMap.init();
                    } finally {
                        done = CURRENT_LOADERS.remove(contextId) == this;
                    }
                }
                return newMap;
            }
        };
        context2maps = CacheBuilder.newBuilder().concurrencyLevel(4).initialCapacity(8192).expireAfterAccess(TIME2LIVE, TimeUnit.MILLISECONDS).build(cacheLoader);

        ConfigurationService service = ServerServiceRegistry.getInstance().getService(ConfigurationService.class);
        enabled = null == service || service.getBoolProperty("com.openexchange.oxfolder.memory.enabled", true);
    }

    /**
     * Starts this instance.
     */
    private synchronized void start() {
        if (timerTask == null) {
            Runnable task = new ShrinkerRunnable();
            int delay = 20000; // Every 20 seconds
            timerTask = ServerServiceRegistry.getInstance().getService(TimerService.class).scheduleWithFixedDelay(task, delay, delay);
        }
    }

    /**
     * Stops this instance.
     */
    private synchronized void stop() {
        ScheduledTimerTask timerTask = this.timerTask;
        if (null != timerTask) {
            this.timerTask = null;
            timerTask.cancel();
        }
        context2maps.invalidateAll();
    }

    /**
     * Gets the tree map for given context identifier.
     *
     * @param contextId The context identifier
     * @return The tree map.
     * @throws OXException If returning tree map fails
     */
    public ConditionTreeMap getMapFor(int contextId) throws OXException {
        if (!enabled) {
            throw OXFolderExceptionCode.RUNTIME_ERROR.create("Memory tree map disabled as per configuration.");
        }
        try {
            return context2maps.get(Integer.valueOf(contextId));
        } catch (ExecutionException e) {
            throw ThreadPools.launderThrowable(e, OXException.class);
        }
    }

    /**
     * Gets the tree map for given context identifier if already initialized.
     *
     * @param contextId The context identifier
     * @return The tree map or <code>null</code>
     */
    public ConditionTreeMap optMapFor(final int contextId) {
        return optMapFor(contextId, true);
    }

    /**
     * Gets the tree map for given context identifier if already initialized.
     *
     * @param contextId The context identifier
     * @param triggerLoad Whether to trigger asynchronous loading of the condition tree map if none is available yet
     * @return The tree map or <code>null</code>
     */
    public ConditionTreeMap optMapFor(final int contextId, final boolean triggerLoad) {
        if (!enabled) {
            return null;
        }

        ConditionTreeMap treeMap = context2maps.getIfPresent(Integer.valueOf(contextId));
        if (null != treeMap) {
            return treeMap;
        }

        if (triggerLoad) {
            /*
             * Submit a task for tree map initialization
             */
            ThreadPools.getThreadPool().submit(ThreadPools.trackableTask(new LoadTreeMapRunnable(contextId, context2maps, LOG)));
        }
        return null;
    }

    /**
     * Drops elapsed maps.
     */
    protected void shrink() {
        context2maps.cleanUp();

        long maxStamp = System.currentTimeMillis() - TIME2LIVE;
        for (Iterator<ConditionTreeMap> it = context2maps.asMap().values().iterator(); it.hasNext();) {
            ConditionTreeMap map = it.next();
            if (null != map) {
                map.trim(maxStamp);
            }
        }
    }

    /*-
     * -------------------------------- Helpers ------------------------------------
     */

    private final class ShrinkerRunnable implements Runnable {

        ShrinkerRunnable() {
            super();
        }

        @Override
        public void run() {
            shrink();
        }
    }

    private static class LoadTreeMapRunnable implements Runnable {

        private final int contextId;
        private final LoadingCache<Integer, ConditionTreeMap> context2maps;
        private final org.slf4j.Logger logger;

        LoadTreeMapRunnable(int contextId, LoadingCache<Integer, ConditionTreeMap> context2maps, org.slf4j.Logger logger) {
            super();
            this.contextId = contextId;
            this.context2maps = context2maps;
            this.logger = logger;
        }

        @Override
        public void run() {
            try {
                context2maps.get(Integer.valueOf(contextId));
            } catch (Exception e) {
                logger.error("", e.getCause());
            }
        }
    } // End of LoadTreeMapRunnable class

}
