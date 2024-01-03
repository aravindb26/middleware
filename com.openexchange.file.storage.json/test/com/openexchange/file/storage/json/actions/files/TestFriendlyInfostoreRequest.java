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

package com.openexchange.file.storage.json.actions.files;

import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.SimContext;
import com.openexchange.groupware.ldap.SimUser;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.SimServerSession;


/**
 * {@link TestFriendlyInfostoreRequest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class TestFriendlyInfostoreRequest extends AJAXInfostoreRequest {

    public TestFriendlyInfostoreRequest() {
        this("UTC");
    }

    public TestFriendlyInfostoreRequest(String userTimeZone) {
        super(new AJAXRequestData(), new SimServerSession(new SimContext(1), new SimUser(), null));
        ((SimUser) getSimSession().getUser()).setTimeZone(userTimeZone);
    }

    public TestFriendlyInfostoreRequest param(String key, String value) {
        data.putParameter(key, value);
        return this;
    }

    public TestFriendlyInfostoreRequest body(Object body) {
        data.setData(body);
        return this;
    }

    public SimServerSession getSimSession() {
        return (SimServerSession) getSession();
    }

    @Override
    public InfostoreRequest requireBody() throws OXException {
        if (data.getData() == null) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create("data");
        }
        return this;
    }

}