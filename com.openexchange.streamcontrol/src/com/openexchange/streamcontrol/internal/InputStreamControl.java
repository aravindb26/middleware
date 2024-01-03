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

package com.openexchange.streamcontrol.internal;

import java.time.Duration;
import com.openexchange.java.AbstractOperationsWatcher;
import com.openexchange.java.InterruptibleInputStream;
import com.openexchange.java.Streams;

/**
 * {@link InputStreamControl} - A registry for threads currently reading from an input stream.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class InputStreamControl extends AbstractOperationsWatcher<InputStreamInfo> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InputStreamControl.class);

    private static final InputStreamControl INSTANCE = new InputStreamControl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static InputStreamControl getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link InputStreamControl}.
     */
    private InputStreamControl() {
        super("InputStreamControl", Duration.ofMinutes(5));
    }

    @Override
    protected InputStreamInfo getPoisonElement() {
        return InputStreamInfo.POISON;
    }

    @Override
    protected org.slf4j.Logger getLogger() {
        return LOG;
    }

    @Override
    protected void handleExpiredOperation(InputStreamInfo info) throws Exception {
        InterruptibleInputStream in = info.getIn();
        in.interrupt();
        Streams.close(in);
        info.getProcessingThread().interrupt();
    }

}
