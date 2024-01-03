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

package com.openexchange.chronos.scheduling.impl;

import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.common.DelegatingCalendarResult;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.chronos.scheduling.ProcessResult;
import com.openexchange.chronos.service.CalendarResult;

/**
 * {@link ProcessResultImpl}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ProcessResultImpl extends DelegatingCalendarResult implements ProcessResult {

    private final @NonNull MessageStatus messageStatus;
    private final ITipAnalysis analysis;

    /**
     * Initializes a new {@link ProcessResultImpl}.
     * 
     * @param delegate The underlying calendar result
     * @param analysis The corresponding iTIP analysis
     * @param messageStatus The resulting message status
     */
    public ProcessResultImpl(CalendarResult delegate, ITipAnalysis analysis, @NonNull MessageStatus messageStatus) {
        super(delegate);
        this.analysis = analysis;
        this.messageStatus = messageStatus;
    }

    @Override
    public ITipAnalysis getAnalysis() {
        return analysis;
    }

    @Override
    public @NonNull MessageStatus getMessageStatus() {
        return messageStatus;
    }

    @Override
    public String toString() {
        return "ProcessResultImpl [messageStatus=" + messageStatus + ", delegate=" + delegate + "]";
    }

}
