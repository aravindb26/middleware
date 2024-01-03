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

package com.openexchange.ajax.appPassword;

import java.util.List;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AppPassword;
import com.openexchange.testing.httpclient.models.AppPasswordApplication;
import com.openexchange.testing.httpclient.models.AppPasswordGetAppsResponse;
import com.openexchange.testing.httpclient.models.AppPasswordListResponse;
import com.openexchange.testing.httpclient.models.AppPasswordRegistrationResponse;
import com.openexchange.testing.httpclient.models.AppPasswordRegistrationResponseData;
import com.openexchange.testing.httpclient.modules.AppPasswordApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractAppPasswordTest}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.4
 */
public class AbstractAppPasswordTest extends AbstractConfigAwareAPIClientSession {

    private AppPasswordApi appSpecApi;


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        appSpecApi = new AppPasswordApi(getApiClient());
        super.setUpConfiguration();
    }

    /**
     * Get the list of Application Specific Passwords for the user
     *
     * @return
     * @throws ApiException
     */
    protected List<AppPassword> getList() throws ApiException {
        AppPasswordListResponse response = appSpecApi.listApplicationPassword();
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    /**
     * Get list of applications
     * AppPasswordGetAppsResponsegetApps
     *
     * @return
     * @throws ApiException
     */
    protected List<AppPasswordApplication> getApps() throws ApiException {
        AppPasswordGetAppsResponse response = appSpecApi.getApplications();
        return response.getData();
    }

    /**
     * Add a new application specific password
     *
     * @param appType Application type
     * @return
     * @throws ApiException
     */
    protected AppPasswordRegistrationResponseData addPassword(String appType) throws ApiException {
        AppPasswordRegistrationResponse resp = appSpecApi.addApplicationPassword(appType, "Test");
        return checkResponse(resp.getError(), resp.getErrorDesc(), resp.getData());
    }

    /**
     * Remove password
     *
     * @param uuid The UUID of the password to remove
     * @throws ApiException
     */
    protected void removePassword(String uuid) throws ApiException {
        appSpecApi.removeApplicationPassword(uuid);
    }

    /**
     * Reset the Application Specific Api Client
     * resetApiClient
     *
     * @throws ApiException In case client can't be created
     */
    protected void resetApiClient() throws ApiException {
        appSpecApi = new AppPasswordApi(getApiClient());
    }

}
