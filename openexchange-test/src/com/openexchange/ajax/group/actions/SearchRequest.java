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

package com.openexchange.ajax.group.actions;

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class SearchRequest extends AbstractGroupRequest<SearchResponse> {

    private final String pattern;

    private final boolean failOnError;

    /**
     * @param pattern
     * @param failOnError
     */
    public SearchRequest(final String pattern, final boolean failOnError) {
        super();
        this.pattern = pattern;
        this.failOnError = failOnError;
    }

    /**
     * @param pattern
     */
    public SearchRequest(final String pattern) {
        this(pattern, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getBody() throws JSONException {
        final JSONObject json = new JSONObject();
        json.put(AJAXServlet.PARAMETER_SEARCHPATTERN, pattern);
        return json;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter[] getParameters() {
        return new Parameter[] { new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_SEARCH),
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchParser getParser() {
        return new SearchParser(failOnError);
    }

}
