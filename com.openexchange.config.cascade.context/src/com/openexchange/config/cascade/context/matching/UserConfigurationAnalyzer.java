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

package com.openexchange.config.cascade.context.matching;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;


/**
 * {@link UserConfigurationAnalyzer}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class UserConfigurationAnalyzer {

    private static final LoadingCache<Integer, Set<String>> TAGS_CACHE = CacheBuilder.newBuilder().initialCapacity(64).maximumSize(2048).build(new TagCollector());

	/**
	 * Gets the tags for given user configuration.
	 *
	 * @param perms The user permission bits
	 * @return The tags
	 */
	public Set<String> getTags(UserPermissionBits perms) {
	    if (null == perms) {
            return Collections.emptySet();
        }
        try {
            return TAGS_CACHE.get(I(perms.getPermissionBits()));
        } catch (ExecutionException e) {
            org.slf4j.LoggerFactory.getLogger(UserConfigurationAnalyzer.class).error("Unexpected error getting tags", e);
            throw new RuntimeException(e.getCause());
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class TagCollector extends CacheLoader<Integer, Set<String>> {

        @Override
        public Set<String> load(Integer key) throws Exception {
            List<Permission> permissions = Permission.byBits(i(key));
            if (null == permissions || permissions.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> tags = new HashSet<String>(permissions.size());
            for (Permission permission : permissions) {
                tags.add("uc" + permission.getTagName());
            }
            return Collections.unmodifiableSet(tags);
        }

    }

}
