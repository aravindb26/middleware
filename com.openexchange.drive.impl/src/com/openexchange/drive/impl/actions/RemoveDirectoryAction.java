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

package com.openexchange.drive.impl.actions;

import com.openexchange.drive.Action;
import com.openexchange.drive.DirectoryVersion;
import com.openexchange.drive.DriveAction;
import com.openexchange.drive.impl.comparison.ThreeWayComparison;

/**
 * {@link RemoveDirectoryAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RemoveDirectoryAction extends AbstractDirectoryAction {

    public RemoveDirectoryAction(DirectoryVersion version, ThreeWayComparison<DirectoryVersion> comparison) {
        super(Action.REMOVE, version, null, comparison);
    }

    @Override
    public int compareTo(DriveAction<DirectoryVersion> other) {
        /*
         * compare actions
         */
        int result = super.compareTo(other);
        if (0 == result && (other instanceof RemoveDirectoryAction)) {
            /*
             * compare paths (inner paths before their parents)
             */
            RemoveDirectoryAction otherRemoveDirectoryAction = (RemoveDirectoryAction)other;
            if (null != this.getVersion() && null != otherRemoveDirectoryAction.getVersion()) {
                result = -1 * this.getVersion().getPath().compareTo(otherRemoveDirectoryAction.getVersion().getPath());
            }
        }
        return result;
    }

}
