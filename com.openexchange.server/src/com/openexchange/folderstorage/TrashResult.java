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

package com.openexchange.folderstorage;


/**
 * {@link TrashResult} is the result of a delete folder operation
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.4
 */
public class TrashResult {

    private final boolean trashed;
    private final String path;
    private final String oldPath;
    private final boolean failed;
    private boolean isSupported=true;


    public static TrashResult createUnsupportedTrashResult(){
       return new TrashResult();
    }

    /**
     * Initializes a new {@link TrashResult}.
     *
     * @param path The new path (maybe null)
     * @param oldPath The old path
     */
    public TrashResult() {
        super();
        this.trashed = false;
        this.path = null;
        this.oldPath = null;
        this.failed = false;
        this.isSupported = false;
    }


    /**
     * Initializes a new {@link TrashResult}.
     *
     * @param path The new path (maybe null)
     * @param oldPath The old path
     */
    public TrashResult(String path, String oldPath) {
        super();
        this.trashed = path != null && !path.equals(oldPath);
        this.path = path;
        this.oldPath = oldPath;
        this.failed = false;
    }

    /**
     * Initializes a new {@link TrashResult}.
     *
     * @param path The new path (maybe null)
     * @param oldPath The old path
     * @param isTrashed Flag indicating whether the folder is trashed or not.
     */
    public TrashResult(String path, String oldPath, boolean isTrashed) {
        super();
        this.trashed = isTrashed;
        this.path = path;
        this.oldPath = oldPath;
        this.failed = false;
    }

    /**
     * Initializes a new {@link TrashResult}.
     *
     * @param oldPath The old path
     * @param failed Indicating whether the delete operations has failed or not
     */
    public TrashResult(String oldPath, boolean failed) {
        super();
        this.trashed = false;
        this.path = null;
        this.oldPath = oldPath;
        this.failed = failed;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public boolean hasFailed() {
        return failed;
    }

    public String getNewPath() {
        return path;
    }

    public String getOldPath() {
        return oldPath;
    }

    public boolean isSupported(){
        return isSupported;
    }

}