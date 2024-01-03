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

package com.openexchange.test.common.test.pool;

import java.io.Serializable;

/**
 * {@link Icon}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class Icon implements Serializable {

    private static final long serialVersionUID = 1308641904934372441L;

    private String mimeType;

    private byte[] data;

    /**
     * Initializes a new {@link Icon}.
     */
    public Icon() {
        super();
    }

    /**
     * Sets the mimeType. Allowed types are <code>image/png</code>, <code>image/jpg</code>
     * and <code>image/jpeg</code>.
     *
     * @param mimeType The mimeType
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Gets the mimeType.
     *
     * @return The mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the icon data as raw bytes. Icons should be <code>128x128 px</code> in size
     * and must not exceed 256kb.
     *
     * @param data The data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Gets the icon data as raw bytes.
     *
     * @return The data
     */
    public byte[] getData() {
        return data;
    }

}
