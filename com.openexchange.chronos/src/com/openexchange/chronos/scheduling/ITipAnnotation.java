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

package com.openexchange.chronos.scheduling;

import java.util.List;
import java.util.Map;
import com.openexchange.chronos.Event;

/**
 * 
 * {@link ITipAnnotation}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public interface ITipAnnotation {

    /**
     * The event the annotation describes
     *
     * @return The event or <code>null</code>
     */
    Event getEvent();

    /**
     * The display message for the user
     *
     * @return The localized message
     */
    String getMessage();

    /**
     * The arguments for the message
     * 
     * @return The arguments, might be empty
     */
    List<Object> getArgs();

    /**
     * Gets a map containing additional parameters of the annotation.
     * 
     * @return Additional parameters, or <code>null</code> if not set
     */
    Map<String, Object> getAdditionals();

}
