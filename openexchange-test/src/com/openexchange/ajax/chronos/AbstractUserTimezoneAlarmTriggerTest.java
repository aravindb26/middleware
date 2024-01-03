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

package com.openexchange.ajax.chronos;

import org.junit.jupiter.api.TestInfo;
import java.util.TimeZone;
import java.util.stream.Stream;

/**
 * {@link AbstractUserTimezoneAlarmTriggerTest} runs the tests with different user timezone configurations.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public abstract class AbstractUserTimezoneAlarmTriggerTest extends AbstractAlarmTriggerTest {

    public static Stream<TimeZone> data() {
        return Stream.of(
                TimeZone.getTimeZone("UTC"),
                TimeZone.getTimeZone("Europe/Berlin"),
                TimeZone.getTimeZone("America/New_York")
        );
    }

    public void prepareTest(TimeZone tz, TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        changeTimezone(tz);
    }

}
