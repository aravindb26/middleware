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

package com.openexchange.mail.mime.crypto.impl;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizer;

/**
 * Keeps list of registered CryptoMailRecognizers
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.6
 * @since Guard 2.10.7
 */
public class CryptoMailRecognizerRegistry {

    private static volatile CryptoMailRecognizerRegistry instance;

    /**
     * Gets instance of CryptoMailRecognizerRegistry
     *
     * @return current instance or new if none existing
     */
    public static CryptoMailRecognizerRegistry getInstance() {
        CryptoMailRecognizerRegistry tmp = instance;
        if (null == tmp) {
            synchronized (CryptoMailRecognizerRegistry.class) {
                tmp = instance;
                if (tmp == null) {
                    instance = tmp = new CryptoMailRecognizerRegistry();
                }
            }
        }
        return tmp;
    }

    /**
     * Releases the registry instance.
     */
    public static void releaseInstance() {
        CryptoMailRecognizerRegistry tmp = instance;
        if (null != tmp) {
            synchronized (CryptoMailRecognizerRegistry.class) {
                tmp = instance;
                if (tmp != null) {
                    instance = null;
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    private List<CryptoMailRecognizer> recognizers;

    /**
     *
     * Initializes a new {@link CryptoMailRecognizerRegistry}.
     */
    private CryptoMailRecognizerRegistry() {
        this.recognizers = new ArrayList<CryptoMailRecognizer>();
    }

    /**
     * Return list of registered CryptoMailRecognizers
     *
     * @return The complete list of registered {@link CryptoMailRecognizer}
     */
    public List<CryptoMailRecognizer> getRecognizers() {
        return recognizers;
    }

    /**
     * Adds recognizer to registered list
     *
     * @param recognizer The recognizer to add
     * @return <code>true</code> if this collection changed as a result of the call
     */
    public boolean addRecognizer(CryptoMailRecognizer recognizer) {
        return recognizers.add(recognizer);
    }

    /**
     * Removes recognizer from registered
     *
     * @param recognizer The recognizer to remove
     * @return <code>true</code> if this list contained the specified element
     */
    public boolean removeRecognizer(CryptoMailRecognizer recognizer) {
        return recognizers.remove(recognizer);
    }

}
