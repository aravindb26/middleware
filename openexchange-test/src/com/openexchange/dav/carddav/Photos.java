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

package com.openexchange.dav.carddav;

import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import com.openexchange.test.common.configuration.AJAXConfig;

/**
 * {@link Photos}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.3
 */
public enum Photos {

    FALLBACK_PICTURE("fallback.png"),
    PNG_100x100("PNG_100x100.png"),
    PNG_200x200("PNG_200x200.png"),
    GIF_100x100("GIF_100x100.gif"),
    JPG_400x250("JPG_400x250.jpg"),
    JPG_1000x750("JPG_1000x750.jpg");

    private final String filename;
    private byte[] data = null;

    /**
     * Initializes a new {@link Photos}.
     */
    private Photos(String filename) {
        this.filename = filename;
    }

    public byte[] getBytes() {
        if (data != null) {
            return data;
        }
        synchronized (this) {
            if (data != null) {
                return data;
            }

            try {
                data = readFile(this.filename);
                return data;
            } catch (IOException e) {
                fail("Unable to load photo for test: " + e.getMessage());
                e.printStackTrace();
                return new byte[0];
            }
        }
    }

    private static byte[] readFile(String filename) throws IOException {
        String testDataDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR);
        File imagesFolder = new File(testDataDir, "images");
        final File file = new File(imagesFolder, filename);
        final InputStream is = new FileInputStream(file);
        final byte[] bytes = IOUtils.toByteArray(is);
        return bytes;
    }

}
