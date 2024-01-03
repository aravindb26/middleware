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

package com.openexchange.ajax.resource.actions;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.resource.Resource;
import com.openexchange.resource.json.ResourceWriter;

/**
 * {@link ResourceNewRequest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class ResourceNewRequest extends AbstractResourceRequest<ResourceNewResponse> {

    private final JSONObject resourceJSON;

    private final boolean failOnError;

    /**
     * Initializes a new {@link ResourceNewRequest}
     *
     * @param createMe
     *            The resource to create
     * @param failOnError
     *            <code>true</code> to fail on error; otherwise
     *            <code>false</code>
     * @throws JSONException
     *             If a JSON error occurs
     */
    public ResourceNewRequest(final Resource createMe, final boolean failOnError) throws JSONException {
        super();
        this.failOnError = failOnError;
        resourceJSON = createMe != null ? ResourceWriter.writeResource(createMe) : null;
    }

    public ResourceNewRequest(final Resource resource) throws JSONException {
        this(resource, true);
    }

    @Override
    public Object getBody() throws JSONException {
        return resourceJSON;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    public Parameter[] getParameters() {
        final List<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_NEW));
        return params.toArray(new Parameter[params.size()]);
    }

    @Override
    public ResourceNewParser getParser() {
        return new ResourceNewParser(failOnError);
    }

    private static final class ResourceNewParser extends AbstractAJAXParser<ResourceNewResponse> {

        /**
         * Default constructor.
         */
        ResourceNewParser(final boolean failOnError) {
            super(failOnError);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ResourceNewResponse createResponse(final Response response) throws JSONException {
            return new ResourceNewResponse(response);
        }
    }

}