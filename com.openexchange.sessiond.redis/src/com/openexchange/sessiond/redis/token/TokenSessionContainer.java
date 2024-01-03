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

package com.openexchange.sessiond.redis.token;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONObject;
import org.slf4j.Logger;
import com.openexchange.annotation.NonNull;
import com.openexchange.cluster.map.AbstractJSONMapCodec;
import com.openexchange.cluster.map.BasicCoreClusterMapProvider;
import com.openexchange.cluster.map.ClusterMap;
import com.openexchange.cluster.map.ClusterMapService;
import com.openexchange.cluster.map.CoreMap;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.cluster.serialization.session.ClusterSession;
import com.openexchange.cluster.serialization.session.SessionCodec;
import com.openexchange.exception.OXException;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.sessiond.redis.RedisSessionVersionService;
import com.openexchange.sessiond.redis.SessionImpl;
import com.openexchange.sessiond.redis.osgi.Services;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * This container stores the sessions created by the token login. These sessions either die after 60 seconds or they are moved over to the
 * normal session container if the session becomes active. The session will not become active if the browser still has the cookies for an
 * already existing session.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> Added support for distributed Hazelcast map
 */
public final class TokenSessionContainer extends BasicCoreClusterMapProvider<TokenSessionControl> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TokenSessionContainer.class);

    private static final TokenSessionContainer INSTANCE = new TokenSessionContainer();

    /**
     * Gets the {@link TokenSessionContainer} instance.
     *
     * @return The {@link TokenSessionContainer} instance
     */
    public static TokenSessionContainer getInstance() {
        return INSTANCE;
    }

    private static final MapCodec<TokenSessionControl> initCodec() {
        return new AbstractJSONMapCodec<TokenSessionControl>() {

            @Override
            protected @NonNull JSONObject writeJson(TokenSessionControl value) throws Exception {
                JSONObject jToken = new JSONObject(4);

                jToken.put("creationStamp", value.getCreationStamp());
                jToken.putOpt("clientToken", value.getClientToken());
                jToken.putOpt("serverToken", value.getServerToken());

                SessionImpl session = value.getSession();
                if (session != null) {
                    ObfuscatorService obfuscator = Services.getServiceLookup().getServiceSafe(ObfuscatorService.class);
                    jToken.put("session", SessionCodec.session2Json(session, obfuscator, RedisSessionVersionService.getInstance()));
                }

                return jToken;
            }

            @Override
            protected @NonNull TokenSessionControl parseJson(JSONObject jToken) throws Exception {
                long creationStamp = jToken.getLong("creationStamp");
                String clientToken = jToken.optString("clientToken", null);
                String serverToken = jToken.optString("serverToken", null);

                SessionImpl sessionImpl = null;
                JSONObject jSession = jToken.optJSONObject("session");
                if (jSession != null) {
                    ObfuscatorService obfuscator = Services.getServiceLookup().getServiceSafe(ObfuscatorService.class);
                    ClusterSession session = SessionCodec.json2Session(jSession, obfuscator, RedisSessionVersionService.getInstance());
                    sessionImpl = new SessionImpl(session.getUserId(), session.getLoginName(), session.getPassword(), session.getContextId(), session.getSessionID(), session.getSecret(), session.getRandomToken(), session.getLocalIp(), session.getLogin(), session.getAuthId(), session.getHash(), session.getClient(), session.isStaySignedIn(), session.getOrigin());
                    for (String name : session.getParameterNames()) {
                        Object value = session.getParameter(name);
                        sessionImpl.setParameter(name, value);
                        LOG.debug("Restored remote parameter '{}' with value '{}' for session {} ({}@{})", name, value, session.getSessionID(), Integer.valueOf(session.getUserId()), Integer.valueOf(session.getContextId()));
                    }
                }

                return new TokenSessionControl(sessionImpl, clientToken, serverToken, creationStamp);
            }
        };
    }

    // ------------------------------------------------------------------------------------------------------------------------------- //

    private final Map<String, TokenSessionControl> localServerTokenMap;
    private final Map<String, ScheduledTimerTask> removerMap;
    private final Lock lock;
    private final AtomicBoolean useClusterMap;

    /**
     * Initializes a new {@link TokenSessionContainer}.
     */
    private TokenSessionContainer() {
        super(CoreMap.SESSION_TOKEN_MAP, initCodec(), Duration.ofMinutes(1).toMillis(), () -> Services.getService(ClusterMapService.class));
        this.useClusterMap = new AtomicBoolean(false);
        localServerTokenMap = new HashMap<String, TokenSessionControl>();
        removerMap = new ConcurrentHashMap<String, ScheduledTimerTask>();
        lock = new ReentrantLock();
    }

    /**
     * Changes the backing maps from distributed cluster map ones to local ones.
     */
    public void changeBackingMapToLocalMap() {
        Lock lock = getLock();
        lock.lock();
        try {
            // This happens if cluster map service is removed in the meantime. We cannot copy any information back to the local map.
            useClusterMap.set(false);
            LOG.info("Token-session backing map changed to local");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Changes the backing maps from local ones to distributed cluster map ones.
     *
     * @throws OXException If operation fails
     */
    public void changeBackingMapToClusterMap() throws OXException {
        Lock lock = getLock();
        lock.lock();
        try {
            if (useClusterMap.get()) {
                return;
            }

            ClusterMap<TokenSessionControl> serverTokenClusterMap = getMap();
            if (null == serverTokenClusterMap) {
                throw OXException.general("Cluster map for remote token-session is not available.");
            }

            // This MUST be synchronous!
            for (Map.Entry<String, TokenSessionControl> entry : localServerTokenMap.entrySet()) {
                TokenSessionControl tsc = entry.getValue();
                serverTokenClusterMap.put(entry.getKey(), tsc);
            }
            localServerTokenMap.clear();

            for (Iterator<ScheduledTimerTask> iter = removerMap.values().iterator(); iter.hasNext();) {
                ScheduledTimerTask timerTask = iter.next();
                timerTask.cancel();
                iter.remove();
            }

            useClusterMap.set(true);
            LOG.info("Token-session backing map changed to cluster map");
        } finally {
            lock.unlock();
        }
    }

    private void putIntoMap(String serverToken, TokenSessionControl control) throws OXException {
        if (useClusterMap.get()) {
            ClusterMap<TokenSessionControl> serverTokenClusterMap = getMap();

            if (serverTokenClusterMap != null) {
                serverTokenClusterMap.put(serverToken, control);
            } else {
                LOG.error("Unable to put session into cluster map! Map not found.");
            }
        } else {
            localServerTokenMap.put(serverToken, control);
        }
    }

    private TokenSessionControl removeFromMap(String serverToken) throws OXException {
        if (false == useClusterMap.get()) {
            return localServerTokenMap.remove(serverToken);
        }

        ClusterMap<TokenSessionControl> serverTokenClusterMap = getMap();
        if (serverTokenClusterMap == null) {
            return null;
        }
        return serverTokenClusterMap.remove(serverToken);
    }

    private Map<String, ScheduledTimerTask> getRemoverMap() {
        return useClusterMap.get() ? null : removerMap;
    }

    private Lock getLock() {
        return useClusterMap.get() ? Session.EMPTY_LOCK : lock;
    }

    public TokenSessionControl addSession(SessionImpl session, String clientToken, String serverToken) throws OXException {
        TokenSessionControl control;

        Lock lock = getLock();
        lock.lock();
        try {
            control = new TokenSessionControl(session, clientToken, serverToken, System.currentTimeMillis());
            putIntoMap(serverToken, control);
        } finally {
            lock.unlock();
        }

        scheduleRemover(control);
        return control;
    }

    public TokenSessionControl getSession(String clientToken, String serverToken) throws OXException {
        TokenSessionControl control;

        Lock lock = getLock();
        lock.lock();
        try {
            control = removeFromMap(serverToken);
            if ((null == control) || (System.currentTimeMillis() - control.getCreationStamp() > 60000)) {
                // No such token-session or token-session already elapsed
                throw com.openexchange.sessiond.SessionExceptionCodes.NO_SESSION_FOR_SERVER_TOKEN.create(serverToken, clientToken);
            }
            if (!control.getServerToken().equals(serverToken)) {
                throw com.openexchange.sessiond.SessionExceptionCodes.NO_SESSION_FOR_SERVER_TOKEN.create(serverToken, clientToken);
            }
            if (!control.getClientToken().equals(clientToken)) {
                throw com.openexchange.sessiond.SessionExceptionCodes.NO_SESSION_FOR_CLIENT_TOKEN.create(serverToken, clientToken);
            }
        } finally {
            lock.unlock();
        }

        unscheduleRemover(control);
        return control;
    }

    public TokenSessionControl removeSession(TokenSessionControl control) throws OXException {
        TokenSessionControl removed;

        Lock lock = getLock();
        lock.lock();
        try {
            removed = removeFromMap(control.getServerToken());
        } finally {
            lock.unlock();
        }

        return removed;
    }

    private void scheduleRemover(TokenSessionControl control) {
        Map<String, ScheduledTimerTask> removerMap = getRemoverMap();
        if (null != removerMap) {
            TimerService timerService = Services.optService(TimerService.class);
            if (null == timerService) {
                return;
            }
            ScheduledTimerTask task = timerService.schedule(new TokenSessionTimerRemover(control), 60, TimeUnit.SECONDS);
            removerMap.put(control.getSession().getSessionID(), task);
        }
    }

    private void unscheduleRemover(TokenSessionControl control) {
        Map<String, ScheduledTimerTask> removerMap = getRemoverMap();
        if (null != removerMap) {
            ScheduledTimerTask task = removerMap.get(control.getSession().getSessionID());
            if (null != task) {
                task.cancel();
            }
        }
    }

}
