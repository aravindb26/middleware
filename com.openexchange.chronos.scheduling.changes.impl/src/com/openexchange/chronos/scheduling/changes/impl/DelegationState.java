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

package com.openexchange.chronos.scheduling.changes.impl;

import com.openexchange.chronos.ParticipationStatus;

/**
 * 
 * {@link DelegationState}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.8
 */
interface DelegationState {

    /**
     * Describes a changed participant status
     *
     * @param none The participant status
     * @return A description of the change
     */
    String statusChange(ParticipationStatus none);

    /**
     * Describes a changed participant status for a specific series instance
     *
     * @param none The updated status of the participant
     * @param ofSeries The summary of the series master
     * @return A description of the change
     */
    String statusChangeInstance(ParticipationStatus none, String ofSeries);

    /**
     * Description for a removed or deleted participant
     *
     * @return A description of the change
     */
    String getDeleteIntroduction();

    /**
     * Description for a removed or deleted participant for a specific series instance
     * 
     * @param ofSeries The summary of the series master
     * @return A description of the change
     */
    String getDeleteInstanceIntroduction(String ofSeries);

    /**
     * Description for an event update
     *
     * @return A description of the change
     */
    String getUpdateIntroduction();

    /**
     * Description for an event update for a specific series instance
     *
     * @param ofSeries The summary of the series master
     * @return A description of the change
     */
    String getUpdateInstanceIntroduction(String ofSeries);

    /**
     * Description for declined event update proposal
     *
     * @return A description of the change
     */
    String getDeclineCounterIntroduction();

    /**
     * Description for a new event
     *
     * @return A description of the change
     */
    String getCreateIntroduction();

    /**
     * Description for a new event instance
     *
     * @param ofSeries The summary of the series master
     * @return A description of the change
     */
    String getCreateInstanceIntroduction(String ofSeries);

}
