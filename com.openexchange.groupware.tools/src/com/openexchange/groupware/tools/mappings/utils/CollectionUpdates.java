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

package com.openexchange.groupware.tools.mappings.utils;

import com.openexchange.groupware.tools.mappings.common.CollectionUpdate;
import com.openexchange.groupware.tools.mappings.common.ItemUpdate;
import com.openexchange.groupware.tools.mappings.common.SimpleCollectionUpdate;

/**
 * {@link CollectionUpdates}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class CollectionUpdates {

    /**
     * Initializes a new {@link CollectionUpdates}.
     *
     */
    private CollectionUpdates() {
        super();
    }

    /**
     * Gets a value indicating whether an {@link CollectionUpdate} can be considered empty or not
     *
     * @param <O> The type of element in the collection
     * @param <E> The fields of the type
     * @param collectionUpdate The collection update to check
     * @return <code>true</code> if the collection can be considered empty
     */
    public static <O, E extends Enum<E>> boolean isNullOrEmpty(CollectionUpdate<O, E> collectionUpdate) {
        return null == collectionUpdate || collectionUpdate.isEmpty();
    }

    /**
     * Gets a value indicating whether an {@link SimpleCollectionUpdate} can be considered empty or not
     *
     * @param <O> The type of element in the collection
     * @param collectionUpdate The collection update to check
     * @return <code>true</code> if the collection can be considered empty
     */
    public static <O> boolean isNullOrEmpty(SimpleCollectionUpdate<O> collectionUpdate) {
        return null == collectionUpdate || collectionUpdate.isEmpty();
    }

    /**
     * Gets a value indicating whether an {@link ItemUpdate} can be considered empty or not
     *
     * @param <O> The type of element in the collection
     * @param <E> The fields of the type
     * @param itemUpdate The item update to check
     * @return <code>true</code> if the update can be considered empty
     */
    public static <O, E extends Enum<E>> boolean isNullOrEmpty(ItemUpdate<O, E> itemUpdate) {
        return null == itemUpdate || itemUpdate.isEmpty();
    }

}
