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

package com.openexchange.ajax.apiclient.oauth;

import java.util.List;
import java.util.Map;
import com.openexchange.testing.httpclient.invoker.ApiCallback;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.Pair;
import okhttp3.Call;

/**
 * {@link OAuthApiClient}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.5
 */
public class OAuthApiClient extends ApiClient {

    private final ErrorAwareSupplier<String> tokenSupplier;

    /**
     * Initializes a new {@link OAuthApiClient}.
     */
    public OAuthApiClient(ErrorAwareSupplier<String> tokenSupplier) {
        super();
        this.tokenSupplier = tokenSupplier;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Call buildCall(String path, String method, List<Pair> queryParams, List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams, Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback) throws ApiException {
        try {
            headerParams.put("Authorization", tokenSupplier.get());
        } catch (Exception e) {
            throw new ApiException(e);
        }
        return super.buildCall(path, method, queryParams, collectionQueryParams, body, headerParams, cookieParams, formParams, authNames, callback);
    }

    static interface ErrorAwareSupplier<T> {

        T get() throws Exception;

    }

}
