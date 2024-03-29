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

package com.openexchange.webdav.action.behaviour;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.openexchange.consistency.ConsistencyExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.webdav.action.WebdavRequest;


public class UserAgentBehaviour implements Behaviour{

	private final Map<Class<? extends Object>, Object> classes;
	private final Pattern pattern;

	public UserAgentBehaviour(final String userAgentPattern, final Object...implementations) throws OXException {
        super();
        pattern = Pattern.compile(userAgentPattern);
		classes = new HashMap<Class<? extends Object>, Object>();
		for (Object object : new HashSet<Object>(Arrays.asList(implementations))) {
		    Class<? extends Object> addMe = object.getClass();
            while (addMe != null) {
                Class[] interfaces = addMe.getInterfaces();
                for (Class<? extends Object> iFace : interfaces) {
                    if (classes.putIfAbsent(iFace, object) != null) {
                        throw ConsistencyExceptionCodes.REGISTRATION_FAILED.create("Two implemenations for " + iFace);
                    }
                }
                addMe = addMe.getSuperclass();
            }
        }
	}

	@Override
    public <T> T get(final Class<T> clazz) {
		return (T) classes.get(clazz);
	}

	@Override
    public boolean matches(final WebdavRequest req) {
		String userAgent = req.getHeader("user-agent");
        return userAgent == null ? false : pattern.matcher(userAgent).find();
	}

	@Override
    public Set<Class<? extends Object>> provides() {
		return classes.keySet();
	}

	@Override
	public String toString(){
		return "UserAgent matcher: "+pattern.toString();
	}

}
