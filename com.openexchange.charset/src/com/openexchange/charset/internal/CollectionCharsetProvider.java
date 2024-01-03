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

package com.openexchange.charset.internal;

import static com.openexchange.java.Strings.asciiLowerCase;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.spi.CharsetProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.java.Strings;

/**
 * {@link <code>CollectionCharsetProvider</code>} - A charset provider which performs the
 * {@link CollectionCharsetProvider#charsetForName(String)} and {@link CollectionCharsetProvider#charsets()} method invocations by iterating
 * over collected charset providers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CollectionCharsetProvider extends CharsetProvider {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CollectionCharsetProvider.class);

    private final AtomicReference<Map<String, Charset>> charsetMapReference;
    private final Map<Class<? extends CharsetProvider>, CharsetProvider> providerList;

    /**
     * Initializes a new {@link <code>CollectionCharsetProvider</code>}
     */
    public CollectionCharsetProvider() {
        super();
        providerList = new ConcurrentHashMap<Class<? extends CharsetProvider>, CharsetProvider>(3);
        charsetMapReference = new AtomicReference<Map<String, Charset>>();
    }

    /**
     * Initializes a new {@link <code>CollectionCharsetProvider</code>} with specified instance of {@link <code>CharsetProvider</code>}
     *
     * @param provider The charset provider to be initially added
     */
    public CollectionCharsetProvider(final CharsetProvider provider) {
        this();
        providerList.put(provider.getClass(), provider);
    }

    /**
     * Adds an instance of {@link <code>CharsetProvider</code>} to this provider's collection
     *
     * @param charsetProvider The charset provider to add
     */
    public void addCharsetProvider(final CharsetProvider charsetProvider) {
        providerList.put(charsetProvider.getClass(), charsetProvider);
        charsetMapReference.set(null);
    }

    /**
     * Removes given charset provider from this charset provider's collection
     *
     * @param provider The provider which shall be removed
     * @return The removed charset provider or <code>null</code> if none present
     */
    public CharsetProvider removeCharsetProvider(final CharsetProvider provider) {
        return removeCharsetProvider(provider.getClass());
    }

    /**
     * Removes the charset provider denoted by specified class argument from this charset provider's collection
     *
     * @param clazz The class of the charset provider which shall be removed
     * @return The removed charset provider or <code>null</code> if no collected charset provider is denoted by given class argument
     */
    public CharsetProvider removeCharsetProvider(final Class<? extends CharsetProvider> clazz) {
        CharsetProvider retval = providerList.remove(clazz);
        if (null != retval) {
            charsetMapReference.set(null);
        }
        return retval;
    }

    @Override
    public Charset charsetForName(final String charsetName) {
        if (Strings.isEmpty(charsetName)) {
            throw new IllegalCharsetNameException(charsetName);
        }
        Map<String, Charset> currentCharsetMap = getCharsetMap();
        Charset charset = currentCharsetMap.get(asciiLowerCase(charsetName));
        if (charset == null) {
            if (charsetName.charAt(0) == '\'') {
                String unquoted = unquote(charsetName, true);
                if (unquoted != null) {
                    charset = currentCharsetMap.get(asciiLowerCase(unquoted));
                }
            } else if (charsetName.charAt(0) == '"') {
                String unquoted = unquote(charsetName, false);
                if (unquoted != null) {
                    charset = currentCharsetMap.get(asciiLowerCase(unquoted));
                }
            }
        }
        return charset;
    }

    /**
     * Removes single or double quotes from charset name.
     *
     * @param charsetName The charset name to be unquoted
     * @param singleQuote Whether charset name starts with a single quote or double quote
     * @return The unquoted charset name or <code>null</code>
     */
    private static String unquote(String charsetName, boolean singleQuote) {
        return charsetName.endsWith(singleQuote ? "'" : "\"") ? charsetName.substring(1, charsetName.length() - 1) : null;
    }

    @Override
    public Iterator<Charset> charsets() {
        return getCharsetMap().values().iterator();
    }

    private Map<String, Charset> getCharsetMap() {
        Map<String, Charset> charsetMap = charsetMapReference.get();
        if (charsetMap == null) {
            synchronized (this) {
                charsetMap = charsetMapReference.get();
                if (charsetMap == null) {
                    ImmutableMap.Builder<String, Charset> newMap = ImmutableMap.builder();
                    Set<String> names = new HashSet<>();
                    for (CharsetProvider provider : providerList.values()) {
                        for (Iterator<Charset> iter = provider.charsets(); iter.hasNext();) {
                            // Put by charset name
                            Charset cs = iter.next();
                            String name = asciiLowerCase(cs.name());
                            if (names.add(name)) {
                                newMap.put(name, cs);
                            } else {
                                LOG.debug("Discarding duplicate charset: {}", name);
                            }

                            // Check charset's aliases
                            for (String aliaz : cs.aliases()) {
                                // Put by charset alias
                                String alias = asciiLowerCase(aliaz);
                                if (names.add(alias)) {
                                    newMap.put(alias, cs);
                                } else {
                                    LOG.debug("Discarding duplicate charset: {}", alias);
                                }
                            }
                        }
                    }
                    charsetMap = newMap.build();
                    charsetMapReference.set(charsetMap);
                }
            }
        }
        return charsetMap;
    }

}
