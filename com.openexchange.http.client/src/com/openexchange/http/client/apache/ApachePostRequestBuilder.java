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

package com.openexchange.http.client.apache;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import com.openexchange.http.client.builder.HTTPPostRequestBuilder;

/**
 *
 * {@link ApachePostRequestBuilder}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class ApachePostRequestBuilder extends CommonApacheHTTPRequest<HTTPPostRequestBuilder> implements HTTPPostRequestBuilder {

    private HttpEntity requestEntity = null;
    protected Map<String, String> urlParameters = new TreeMap<String, String>();

	public ApachePostRequestBuilder(final ApacheClientRequestBuilder coreBuilder) {
		super(coreBuilder);
	}

	@Override
	protected HttpRequestBase createMethod(final String encodedSite) {
	    HttpPost method = new HttpPost(encodedSite);
	    if (null != requestEntity) {
	        method.setEntity(requestEntity);
	    }
		return method;
	}

    @Override
    public void urlParameter(String parameter, String value) {
        urlParameters.put(parameter, value);
    }

	@Override
    public void setRequestEntity(String requestEntity, String contentType) {
        this.requestEntity = new StringEntity(requestEntity, ContentType.create(contentType, "UTF-8"));
    }
	
    @Override
    protected void addParams(HttpRequestBase m) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(m.getURI());

        for (Map.Entry<String, String> entry : urlParameters.entrySet()) {
            uriBuilder.addParameter(entry.getKey(), entry.getValue());
        }
        m.setURI(uriBuilder.build());

        if (parameters.isEmpty()) {
            return;
        }
        // only allow to add parameters if POST if requestEntity is not already set
        if (null != requestEntity) {
            throw new IllegalArgumentException("not allowed to set POST Parameters and RequestEntity at the same time");
        }

        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
        for (final Map.Entry<String, String> entry : parameters.entrySet()) {
            nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        HttpPost post = (HttpPost) m;
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs, Charset.forName("UTF-8")));
    }

}
