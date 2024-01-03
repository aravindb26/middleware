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

package com.openexchange.consistency.internal.solver;

import java.io.ByteArrayInputStream;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorage;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;

/**
 * {@link CreateDummyFileSolver}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.8.0
 */
public abstract class CreateDummyFileSolver {

    /**
     * The calculated MD5 hash-sum of a dummy file
     * <p>
     * The sum was calculated using a dummy file and the corresponding function, which consumes the stream
     * 
     * @see com.openexchange.groupware.infostore.utils.FileMD5SumHelper#calculateMD5(java.io.InputStream)
     */
    protected final static String DUMMY_FILE_SUM = "fd7eeb1250dbf1d3331b3d9ff5bbc212";

    /** The associated file storage */
    protected final FileStorage storage;

    protected CreateDummyFileSolver(final FileStorage storage) {
        super();
        this.storage = storage;
    }

    /**
     * This method create a dummy file and returns its file storage location
     *
     * @return The file storage location of the dummy file
     * @throws OXException If dummy file cannot be created
     */
    protected String createDummyFile(FileStorage storage) throws OXException {
        return storage.saveNewFile(getDummyFile());
    }

    /**
     * This method create a dummy file and returns its file storage location
     *
     * @return The file storage location of the dummy file
     * @throws OXException If dummy file cannot be created
     */
    protected String createDummyFile(FileStorage storage, ByteArrayInputStream dummyFile) throws OXException {
        return storage.saveNewFile(dummyFile);
    }

    /**
     * This method create a dummy file and returns its file storage location
     *
     * @return The file storage location of the dummy file
     */
    protected ByteArrayInputStream getDummyFile() {
        String filetext = "This is just a dummy file";
        return Streams.newByteArrayInputStream(filetext.getBytes(Charsets.UTF_8));
    }
}
