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

package com.openexchange.ajax.mail.filter.api.conversion.writer.test;

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.mail.filter.api.dao.test.NotTest;
import com.openexchange.ajax.mail.filter.api.dao.test.Test;
import com.openexchange.ajax.mail.filter.api.dao.test.argument.NotTestArgument;
import com.openexchange.ajax.mail.filter.api.dao.test.argument.TestArgument;

/**
 * {@link NotWriterImpl}
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class NotWriterImpl extends AbstractWriterImpl<NotTestArgument> {

    /**
     * Initialises a new {@link NotWriterImpl}.
     */
    public NotWriterImpl() {
        super();
    }

    @Override
    public JSONObject write(Test<? extends TestArgument> type, JSONObject jsonObject) throws JSONException {
        final JSONObject jsonObj = new JSONObject();
        final NotTest notTest = (NotTest) type;
        final Test<?> test = (Test<?>) notTest.getTestArgument(NotTestArgument.test);

        final TestWriter testWriter = TestWriterFactory.getWriter(test.getTestCommand());
        final JSONObject jsonTestObj = testWriter.write(test, new JSONObject());

        jsonObj.put("id", notTest.getTestCommand().name().toLowerCase());
        jsonObj.put("test", jsonTestObj);

        return jsonObj;
    }
}
