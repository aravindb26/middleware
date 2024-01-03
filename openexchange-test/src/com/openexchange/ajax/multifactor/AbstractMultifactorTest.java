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

package com.openexchange.ajax.multifactor;

import static com.openexchange.java.Autoboxing.B;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import com.openexchange.ajax.chronos.scheduling.RESTUtilities;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.MultifactorDeleteResponse;
import com.openexchange.testing.httpclient.models.MultifactorDevice;
import com.openexchange.testing.httpclient.models.MultifactorDeviceParameters;
import com.openexchange.testing.httpclient.models.MultifactorDeviceResponse;
import com.openexchange.testing.httpclient.models.MultifactorDevicesResponse;
import com.openexchange.testing.httpclient.models.MultifactorFinishAuthenticationData;
import com.openexchange.testing.httpclient.models.MultifactorFinishRegistrationData;
import com.openexchange.testing.httpclient.models.MultifactorFinishRegistrationResponse;
import com.openexchange.testing.httpclient.models.MultifactorProvider;
import com.openexchange.testing.httpclient.models.MultifactorProvidersResponse;
import com.openexchange.testing.httpclient.models.MultifactorStartAuthenticationResponse;
import com.openexchange.testing.httpclient.models.MultifactorStartAuthenticationResponseData;
import com.openexchange.testing.httpclient.models.MultifactorStartRegistrationResponse;
import com.openexchange.testing.httpclient.models.MultifactorStartRegistrationResponseData;
import com.openexchange.testing.httpclient.modules.MultifactorApi;
import com.openexchange.testing.restclient.modules.AdminApi;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractMultifactorTest}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.1
 */
public class AbstractMultifactorTest extends AbstractConfigAwareAPIClientSession {

    private static String EMPTY_PROVIDER_FILTER = "";
    private MultifactorApi multifactorApi;
    private AdminApi adminApi;


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        multifactorApi = new MultifactorApi(getApiClient());

        com.openexchange.testing.restclient.invoker.ApiClient adminRestClient = RESTUtilities.createRESTClient(testContext.getUsedBy());
        adminRestClient.setBasePath(getRestBasePath());
        String authorizationHeaderValue = "Basic " + Base64.encodeBase64String((admin.getUser() + ":" + admin.getPassword()).getBytes(StandardCharsets.UTF_8));
        adminRestClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        // @formatter:off
        adminRestClient.setHttpClient(new OkHttpClient.Builder()
                                                      .connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS))
                                                      .hostnameVerifier((hostname, session) -> true)
                                                      .build());
        // @formatter:on
        adminApi = new AdminApi(adminRestClient);

        super.setUpConfiguration();
    }

    protected MultifactorApi MultifactorApi() {
        return multifactorApi;
    }

    protected <T> T checkResponse(MultifactorProvidersResponse response, T data) {
        return super.checkResponse(response.getError(), response.getErrorDesc(), data);
    }

    protected <T> T checkResponse(MultifactorDevicesResponse response, T data) {
        return super.checkResponse(response.getError(), response.getErrorDesc(), data);
    }

    protected <T> T checkResponse(MultifactorDeviceResponse response, T data) {
       return super.checkResponse(response.getError(), response.getErrorDesc(), data);
    }

    protected <T> T checkResponse(MultifactorStartRegistrationResponse response, T data) {
        return super.checkResponse(response.getError(), response.getErrorDesc(), data);
    }

    protected <T> T checkResponse(MultifactorFinishRegistrationResponse response, T data) {
       return super.checkResponse(response.getError(), response.getErrorDesc(), data) ;
    }

    protected <T> T checkResponse(MultifactorStartAuthenticationResponse response, T data) {
        return super.checkResponse(response.getError(), response.getErrorDesc(), data);
    }

    protected <T> T checkResponse(MultifactorDeleteResponse response, T data) {
        return super.checkResponse(response.getError(), response.getErrorDesc(), data);
    }

    protected String getRestBasePath() {
        String hostname = AJAXConfig.getProperty(AJAXConfig.Property.SERVER_HOSTNAME);
        String protocol = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL);
        if (protocol == null) {
            protocol = "http";
        }
        return protocol + "://" + hostname + (protocol.equals("https") ? ":443" : ":80");
    }

    /**
     * Returns the {@link AdminApi}
     *
     * @return The {@link AdminApi}
     */
    protected AdminApi getAdminApi() {
        return adminApi;
    }

    /**
     * Gets a list of providers available for the current session.
     *
     * @return A list of available providers
     * @throws ApiException
     */
    protected List<MultifactorProvider> getProviders(String filter) throws ApiException {
        MultifactorProvidersResponse resp = this.multifactorApi.multifactorProviderActionAll(filter);
        return checkResponse(resp, resp.getData());
    }

    /**
     * Gets a list of providers available for the current session.
     *
     * @return A list of available providers
     * @throws ApiException
     */
    protected List<MultifactorProvider> getProviders() throws ApiException {
        return getProviders(EMPTY_PROVIDER_FILTER);
    }

    /**
     * Gets a provider by name
     *
     * @param name The name of the provider
     * @return The provider with the given name, or an empty optional
     * @throws ApiException
     */
    protected Optional<MultifactorProvider> getProvider(String name) throws ApiException {
        return getProviders().stream().filter(p -> p.getName().contentEquals(name)).findFirst();
    }

    /**
     * Gets a provider by name and asserts that the provider exists
     *
     * @param name The name of the provider
     * @return The provider with the given name
     * @throws ApiException
     */
    protected MultifactorProvider requireProvider(String name) throws ApiException {
        Optional<MultifactorProvider> provider = getProvider(name);
        assertThat(B(provider.isPresent()), is(Boolean.TRUE));
        return provider.get();
    }

    /**
     * Gets a list of available device for the ensures that the list contains at least one device
     *
     * @return A list of devices for the given provider
     * @throws ApiException
     */
    protected List<MultifactorDevice> requireDevices() throws ApiException{
        List<MultifactorDevice> devices = getDevices();
        assertThat(devices, is(not(empty())));
        return devices;
    }

    /**
     * Gets a list of availbale device for the given provider
     *
     * @return A list of devices for the given provider
     * @throws ApiException
     */
    protected List<MultifactorDevice> getDevices() throws ApiException{
        MultifactorDevicesResponse resp = multifactorApi.multifactorDeviceActionAll();
        return checkResponse(resp, resp.getData());
    }

    /**
     * Gets a device by ID
     *
     * @param deviceId The ID of the device to get
     * @return The device or an empty Optional
     * @throws ApiException
     */
    protected Optional<MultifactorDevice> getDevice(String deviceId) throws ApiException {
        List<MultifactorDevice> devices = getDevices();
        return devices.stream().filter(d -> deviceId.equals(d.getId())).findFirst();
    }

    /**
     * Gets the device with the given ID and ensures that the device is present
     *
     * @param deviceId The ID of the device to get
     * @return The device
     * @throws ApiException
     */
    protected MultifactorDevice requireDevice(String deviceId) throws ApiException {
        Optional<MultifactorDevice> device = getDevice(deviceId);
        return device.orElseThrow(() -> new AssertionError("The device with the given ID \"" + deviceId + "\" must be present."));
    }

    /**
     * Starts registering a new multifactor device
     *
     * @param providerName The name of the provider.
     * @return The response data
     * @throws ApiException
     */
    protected MultifactorStartRegistrationResponseData startRegistration(String providerName) throws ApiException {
        final String phoneNumber = null;
        final String deviceName = null;
        final Boolean backupDevice = Boolean.FALSE;
        return startRegistration(providerName, deviceName, phoneNumber, backupDevice);
    }

    /**
     * Starts registering a new multifactor device for the given provider
     *
     * @param providerName The name of the provider
     * @param deviceName The name of the new device, or null to choose a default name
     * @param phoneNumber [SMS] The phone number for the SMS provider or null for other providers
     * @param backup Whether the device should be used as backup device or not
     * @return The response data
     * @throws ApiException
     */
    protected MultifactorStartRegistrationResponseData startRegistration(String providerName, String deviceName, String phoneNumber, Boolean backup) throws ApiException {
        return startRegistration(getApiClient(), multifactorApi, providerName, deviceName, phoneNumber, backup);
    }

    /**
     * Starts registering a new multifactor device for the given provider
     *
     * @param client The {@link SessionAwareClient}
     * @param api The API instance to use
     * @param providerName The name of the provider
     * @param deviceName The name of the new device, or null to choose a default name
     * @param phoneNumber [SMS] The phone number for the SMS provider or null for other providers
     * @param backup Whether the device should be used as backup device or not
     * @return The response data
     * @throws ApiException
     */
    protected MultifactorStartRegistrationResponseData startRegistration(SessionAwareClient client, MultifactorApi api, String providerName, String deviceName, String phoneNumber, Boolean backup) throws ApiException {

        final MultifactorDevice deviceData = new MultifactorDevice();
        deviceData.setProviderName(providerName);
        deviceData.setName(deviceName);
        deviceData.backup(backup);

        if (phoneNumber != null) {
            MultifactorDeviceParameters paramters = new MultifactorDeviceParameters();
            paramters.setPhoneNumber(phoneNumber);
            deviceData.setParameters(paramters);
        }

        MultifactorStartRegistrationResponse resp = api.multifactorDeviceActionStartRegistration(deviceData);
        return checkResponse(resp, resp.getData());
    }

    /**
     * Finishes the registration of new mulitfactor device for the given provider
     *
     * @param providerName The name of the provider
     * @param deviceId The ID of the device to finish registration for
     * @param secretToken [TOTP, SMS] The secret authentication token
     * @param clientData [U2F] clientData
     * @param registrationData [U2F] registrationData
     * @return The response data
     * @throws ApiException
     */
    protected MultifactorDevice finishRegistration(String providerName, String deviceId, String secretToken, String clientData, String registrationData) throws ApiException {
        return finishRegistration(getApiClient(), multifactorApi, providerName, deviceId, secretToken, clientData, registrationData);
    }

    /**
     * Finishes the registration of new mulitfactor device for the given provider
     *
     * @param client The {@link SessionAwareClient}
     * @param api The API instance to use
     * @param providerName The name of the provider
     * @param deviceId The ID of the device to finish registration for
     * @param secretToken [TOTP, SMS] The secret authentication token
     * @param clientData [U2F] clientData
     * @param registrationData [U2F] registrationData
     * @return The response data
     * @throws ApiException
     */
    protected MultifactorDevice finishRegistration(SessionAwareClient client, MultifactorApi api, String providerName, String deviceId, String secretToken, String clientData, String registrationData) throws ApiException {
        MultifactorFinishRegistrationData  data = new MultifactorFinishRegistrationData();
        data.setSecretCode(secretToken);
        data.setClientData(clientData);
        data.setRegistrationData(registrationData);

        MultifactorFinishRegistrationResponse resp = api.multifactorDeviceActionfinishRegistration(providerName, deviceId, data);
        return checkResponse(resp, resp.getData());
    }

    /**
     * Unregisters a device and asserts that it was removed
     *
     * @param providerName The name of the provider to unregister the device for
     * @param deviceId The ID of the device to delete
     * @return A list of device ID which were deleted
     * @throws ApiException
     */
    protected List<String> unregisterDevice(String providerName, String deviceId) throws ApiException {
        MultifactorDeleteResponse resp = MultifactorApi().multifactorDeviceActionDelete(providerName, deviceId);
        List<String> deletedDeviceIds = checkResponse(resp.getError(), resp.getErrorDesc(), resp.getData());

        //ensure it's gone
        assertThat(B(deletedDeviceIds.isEmpty()), is(Boolean.FALSE));
        Optional<MultifactorDevice> device = getDevice(deviceId);
        assertThat(device, is(Optional.empty()));

        return deletedDeviceIds;
    }

    /**
     * Start the authentication for a given device
     *
     * @param providerName The name of the provider
     * @param deviceId The ID of the device to start authentication for
     * @return The response data containing the challenge
     * @throws ApiException
     */
    protected MultifactorStartAuthenticationResponseData startAuthentication(String providerName, String deviceId) throws ApiException {
        MultifactorStartAuthenticationResponse resp = MultifactorApi().multifactorDeviceActionStartAuthentication(providerName, deviceId);
        return checkResponse(resp, resp.getData());
    }

    /**
     * Performs the authentication for a given device
     *
     * @param providerName The name of the provider
     * @param deviceId The ID of the device to use for authentication
     * @param secretCode [SMS | TOTP | BACKUP_STRING] The secret code for authentication
     * @param clientData [U2F] The client-data for authentication
     * @param signatureData [U2F] The signature data for authentication
     * @param keyHandle [U2F] The key handle data for authentication
     * @return The response
     * @throws ApiException
     */
    protected CommonResponse finishAuthentication(String providerName, String deviceId, String secretCode, String clientData, String signatureData, String keyHandle) throws ApiException {
        MultifactorFinishAuthenticationData data = new MultifactorFinishAuthenticationData();
        data.setSecretCode(secretCode);
        data.setClientData(clientData);
        data.setKeyHandle(keyHandle);
        data.setSignatureData(signatureData);
        return MultifactorApi().multifactorDeviceActionfinishAuthentication(providerName, deviceId, data);
    }
}