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

package com.openexchange.groupware.infostore.utils;

import java.util.Comparator;
import com.openexchange.groupware.infostore.DocumentMetadata;

/**
 * {@link MetadataComparator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MetadataComparator implements Comparator<DocumentMetadata> {

    private final Metadata sortedBy;

    /**
     * Initializes a new {@link MetadataComparator}.
     * 
     * @param field The field to compare
     */
    public MetadataComparator(Metadata field) {
        super();
        this.sortedBy = field;
    }

    @Override
    public int compare(DocumentMetadata document1, DocumentMetadata document2) {
        Object value1 = getValue(document1);
        Object value2 = getValue(document2);
        if (value1 == value2) {
            return 0;
        }
        if (null == value1 && null != value2) {
            return -1;
        }
        if (null == value2) {
            return 1;
        }
        if ((value1 instanceof Comparable)) {
            @SuppressWarnings({ "rawtypes", "unchecked" }) int result = ((Comparable) value1).compareTo(value2);
            return result;
        }
        return 0;
    }

    private Object getValue(DocumentMetadata metadata) {
        return sortedBy.doSwitch(new GetSwitch(metadata));
    }

}
