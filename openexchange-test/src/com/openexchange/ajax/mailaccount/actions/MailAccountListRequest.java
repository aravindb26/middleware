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

package com.openexchange.ajax.mailaccount.actions;

import org.json.JSONArray;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.ajax.framework.Header;

/**
 * {@link MailAccountListRequest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class MailAccountListRequest implements AJAXRequest<MailAccountListResponse> {

    private final boolean failOnError;
    private final int[] cols;
    private final String columns;
    private final JSONArray idArray;

    public MailAccountListRequest(boolean failOnError, int[] ids, int[] cols) {
        this.failOnError = failOnError;
        this.cols = cols;

        StringBuilder bob = new StringBuilder();
        for (int colId : cols) {
            bob.append(colId).append(',');
        }
        bob.setLength(bob.length() - 1);
        this.columns = bob.toString();

        this.idArray = new JSONArray();
        for (int id : ids) {
            idArray.put(id);
        }
    }

    public MailAccountListRequest(int[] ids, int[] cols) {
        this(true, ids, cols);
    }

    @Override
    public Object getBody() {
        return idArray;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return Method.PUT;
    }

    @Override
    public Header[] getHeaders() {
        return NO_HEADER;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() {
        return new Parameter[] { new Parameter("action", "list"), new Parameter("columns", columns)
        };
    }

    @Override
    public AbstractAJAXParser<MailAccountListResponse> getParser() {
        return new MailAccountListParser(failOnError, cols);
    }

    @Override
    public String getServletPath() {
        return "/ajax/account";
    }

}
