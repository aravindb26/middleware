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


package com.openexchange.sessiond.redis.util;

import static com.openexchange.sessiond.redis.util.BrandNames.getBrandIdentifierFrom;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.openexchange.exception.OXException;
import com.openexchange.java.Functions;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.session.Session;
import com.openexchange.sessiond.redis.RedisSessiondService;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;

/**
 * {@link RemovalCollection} - Helper class to collect keys/members to remove.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RemovalCollection {

    private List<String> keysToRemove = null;
    private Map<String, List<String>> sessionsToRemoveFromSets = null;
    private Map<String, List<String>> sessionsToRemoveFromSortedSets = null;

    /**
     * Initializes a new {@link RemovalCollection}.
     */
    public RemovalCollection() {
        super();
    }

    /**
     * Resets this collection for being re.used.
     */
    public void reset() {
        keysToRemove = null;
        sessionsToRemoveFromSets = null;
        sessionsToRemoveFromSortedSets = null;
    }

    /**
     * Adds the session to this remove collection.
     *
     * @param session The session to add
     * @param withUserSet Whether to consider session set per user; e.g. <code>"ox-sessionids:1337:3"</code>
     * @param withSortedSet Whether to consider main sorted set; <code>"ox-sessionids-longlife"</code> or <code>"ox-sessionids-shortlife"</code>
     * @param sessiondService The Redis sessiond service instance to acquire Redis keys
     * @return This collection
     */
    public RemovalCollection addSession(Session session, boolean withUserSet, boolean withSortedSet, RedisSessiondService sessiondService) {
        Object alternativeId = session.getParameter(Session.PARAM_ALTERNATIVE_ID);
        if (alternativeId != null) {
            addKey(sessiondService.getSessionAlternativeKey(alternativeId.toString()));
        }
        String authId = session.getAuthId();
        if (authId != null) {
            addKey(sessiondService.getSessionAuthIdKey(authId));
        }
        String brandId = getBrandIdentifierFrom(session);
        if (brandId != null) {
            addSortedSetMember(sessiondService.getSetKeyForBrand(brandId), session.getSessionID());
        }
        if (withUserSet) {
            addSetMember(sessiondService.getSetKeyForUser(session.getUserId(), session.getContextId()), session.getSessionID());
        }
        if (withSortedSet) {
            addSortedSetMember(sessiondService.getSortSetKeyForSession(session), session.getSessionID());
        }
        return this;
    }

    /**
     * Adds the given key to remove.
     *
     * @param key The key to remove
     * @return This collection
     */
    public RemovalCollection addKey(String key) {
        if (key == null) {
            return this;
        }
        if (keysToRemove == null) {
            keysToRemove = new ArrayList<>();
        }
        keysToRemove.add(key);
        return this;
    }

    /**
     * Adds the given keys to remove.
     *
     * @param keys The keys to remove
     * @return This collection
     */
    public RemovalCollection addKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return this;
        }
        if (keysToRemove == null) {
            keysToRemove = new ArrayList<>();
        }
        keysToRemove.addAll(keys.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        return this;
    }

    /**
     * Adds the given member to remove from specified set.
     *
     * @param setKey The key of the set to remove member from
     * @param member The member to remove
     * @return This collection
     */
    public RemovalCollection addSetMember(String setKey, String member) {
        if (setKey == null || member == null) {
            return this;
        }
        if (sessionsToRemoveFromSets == null) {
            sessionsToRemoveFromSets = new HashMap<>();
        }
        sessionsToRemoveFromSets.computeIfAbsent(setKey, Functions.getNewLinkedListFuntion()).add(member);
        return this;
    }

    /**
     * Adds the given member to remove from specified set.
     *
     * @param setKey The key of the sorted set to remove member from
     * @param member The member to remove
     * @return This collection
     */
    public RemovalCollection addSortedSetMember(String setKey, String member) {
        if (setKey == null || member == null) {
            return this;
        }
        if (sessionsToRemoveFromSortedSets == null) {
            sessionsToRemoveFromSortedSets = new HashMap<>();
        }
        sessionsToRemoveFromSortedSets.computeIfAbsent(setKey, Functions.getNewLinkedListFuntion()).add(member);
        return this;
    }

    /**
     * Removes the collected keys/members.
     *
     * @param sessiondService The Redis sessiond service instance to acquire Redis connector & commands
     * @throws OXException If removal fails
     */
    public void removeCollected(RedisSessiondService sessiondService) throws OXException {
        sessiondService.getConnector().executeVoidOperation(this::removeCollected);
    }

    /**
     * Removes the collected keys/members.
     *
     * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
     * @param sessiondService The Redis sessiond service instance to acquire Redis commands
     * @throws RedisException If removal fails
     */
    public void removeCollected(RedisCommandsProvider commandsProvider) {
        removeCollected(commandsProvider.getKeyCommands(), commandsProvider.getSetCommands(), commandsProvider.getSortedSetCommands());
    }

    /**
     * Removes the collected keys/members.
     *
     * @param keyCommands The key commands
     * @param setCommands The set commands
     * @param sortedSetCommands The sorted set commands
     * @throws RedisException If removal fails
     */
    public void removeCollected(RedisKeyCommands<String, InputStream> keyCommands, RedisSetCommands<String, String> setCommands, RedisSortedSetCommands<String, String> sortedSetCommands) {
        if (keysToRemove != null) {
            keyCommands.del(keysToRemove.toArray(new String[keysToRemove.size()]));
            keysToRemove = null;
        }
        if (sessionsToRemoveFromSets != null) {
            for (Map.Entry<String, List<String>> e : sessionsToRemoveFromSets.entrySet()) {
                List<String> sessionIds2Drop = e.getValue();
                setCommands.srem(e.getKey(), sessionIds2Drop.toArray(new String[e.getValue().size()]));
            }
            sessionsToRemoveFromSets = null;
        }
        if (sessionsToRemoveFromSortedSets != null) {
            for (Map.Entry<String, List<String>> e : sessionsToRemoveFromSortedSets.entrySet()) {
                List<String> sessionIdsToDrop = e.getValue();
                sortedSetCommands.zrem(e.getKey(), sessionIdsToDrop.toArray(new String[sessionIdsToDrop.size()]));
            }
            sessionsToRemoveFromSortedSets = null;
        }
    }

}
