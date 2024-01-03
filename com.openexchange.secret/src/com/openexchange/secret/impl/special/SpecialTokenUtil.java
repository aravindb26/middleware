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
package com.openexchange.secret.impl.special;

import java.util.function.Function;
import java.util.stream.Stream;
import com.openexchange.secret.impl.Token;

/**
 * {@link SpecialTokenUtil} provides help methods to determine the special {@link Token} for a given token string.
 * Special tokens are those which can also contain an argument. E.g. <session-parameter:paramName>
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class SpecialTokenUtil {

    /**
     * Gets the special {@link Token} for the given token string
     *
     * @param token The token string
     * @return The special token or null
     */
    public static Token getSpecialToken(String token) {
        String[] split = token.split(":");
        if(split.length != 2) {
            return null;
        }
        Function<String, Token> factory = SpecialTokenFactory.getFactoryForName(split[0]);
        return factory == null ? null : factory.apply(split[1]);
    }

    /**
     * {@link SpecialTokenFactory} - is a factory for a given special token
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     */
    private enum SpecialTokenFactory {

        /**
         * A session parameter token factory
         */
        SESSION_PARAMETER("session-parameter", t -> new SessionParamToken(t));

        private final Function<String, Token> factory;
        private final String name;

        /**
         * Initializes a new {@link SpecialTokenFactory}.
         *
         * @param name The name of the token
         * @param factory The factory for this token
         */
        private SpecialTokenFactory(String name, Function<String, Token> factory) {
            this.name = name;
            this.factory = factory;
        }

        /**
         * Gets the token name
         *
         * @return The name
         */
        private String getName() {
            return name;
        }

        /**
         * Gets the factory for a given token name
         *
         * @param name The token name
         * @return The factory or null
         */
        private static Function<String, Token> getFactoryForName(String name) {
            return Stream.of(values())
                         .filter(factory -> factory.getName()
                                                   .equals(name))
                         .findAny()
                         .map(fac -> fac.factory)
                         .orElse(null);
        }

    }

}
