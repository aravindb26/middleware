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
import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.CurrentUserResponse;
import com.openexchange.testing.httpclient.models.LoginResponse;
import com.openexchange.testing.httpclient.models.MultifactorDeleteResponse;
import com.openexchange.testing.httpclient.models.MultifactorDevice;
import com.openexchange.testing.httpclient.models.MultifactorDeviceResponse;
import com.openexchange.testing.httpclient.models.MultifactorProvider;
import com.openexchange.testing.httpclient.models.MultifactorStartAuthenticationResponseData;
import com.openexchange.testing.httpclient.models.MultifactorStartRegistrationResponseData;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import com.openexchange.testing.httpclient.modules.LoginApi;
import com.openexchange.testing.httpclient.modules.MultifactorApi;
import com.openexchange.testing.httpclient.modules.UserMeApi;

/**
 * {@link AbstractMultifactorProviderTest} is an abstract "Template Method Pattern" class which provides common tests for Multifactor Providers
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.1
 */
public abstract class AbstractMultifactorProviderTest extends AbstractMultifactorTest {

    protected Integer contextId;
    protected Integer userId;

    //----------------------------------------------------------------------------------------------
    //Template methods

    /**
     * Gets the provider name
     *
     * @return The provider's name
     */
    protected abstract String getProviderName();

    /**
     * Returns if the provider is meant to be a backup provider
     * @return True, if the provider is meant to be a backup provider, false otherwise
     */
    protected abstract boolean isBackupProvider();

    /**
     * Returns if the provider is meant to be a backup provider
     * @return True, if the provider is meant to be a backup provider, false otherwise
     */
    protected abstract boolean isBackupOnlyProvider();

    /**
     * Start registration of a new mulitfactor device
     *
     * @return The result of starting the registration
     * @throws Exception
     */
    protected abstract MultifactorStartRegistrationResponseData doStartRegistration() throws Exception;

    /**
     *  Validates the start registration result
     *
     * @param startRegistrationData the result to validate
     * @throws Exception
     */
    protected abstract void validateStartRegistrationResponse(MultifactorStartRegistrationResponseData startRegistrationData) throws Exception;

    /**
     * Finishes the registration process of a new device
     *
     * @param startRegistrationData The start registration result
     * @return The result of the registration
     * @throws Exception
     */
    protected abstract MultifactorDevice doFinishRegistration(MultifactorStartRegistrationResponseData startRegistrationData) throws Exception;

    /**
     * Validates the registered device
     *
     * @param finishRegistrationData The data to validate
     * @throws Exception
     */
    protected abstract void validateRegisteredDevice(MultifactorDevice device) throws Exception;


    /**
     * Validates the start authentication result
     *
     * @param startAuthenticationData The result to validate
     * @throws Exception
     */
    protected abstract void validateStartAuthenticationResponse(MultifactorStartAuthenticationResponseData startAuthenticationData) throws Exception;

    /**
     * Performs the actual multifactor authentication
     *
     * @param startRegistrationData The registration data
     * @param startAuthenticationData The start authentication data
     * @return The response
     * @throws Exception
     */
    protected abstract CommonResponse doAuthentication(MultifactorStartRegistrationResponseData startRegistrationData, MultifactorStartAuthenticationResponseData startAuthenticationData) throws Exception;

    /**
     * Performs an multifactor authentication with a wrong factor
     *
     * @param startRegistrationData The registration data
     * @param startAuthenticationData The start authentication data
     * @return The response
     * @throws Exception
     */
    protected abstract CommonResponse doWrongAuthentication(MultifactorStartRegistrationResponseData startRegistrationData, MultifactorStartAuthenticationResponseData startAuthenticationData) throws Exception;

    //----------------------------------------------------------------------------------------------

    private  MultifactorStartAuthenticationResponseData startAuthenticationInternal(String deviceId) throws Exception{
        return super.startAuthentication(getProviderName(), deviceId);
    }

    private MultifactorStartRegistrationResponseData registerNewDevice() throws Exception {

        //Start the registration process of a new multifactor device
        MultifactorStartRegistrationResponseData startRegistrationResult = doStartRegistration();
        assertThat(startRegistrationResult.getChallenge(), is(not(nullValue())));
        assertThat(startRegistrationResult.getDeviceId(), not(emptyOrNullString()));

        //Validate the response of the registration
        validateStartRegistrationResponse(startRegistrationResult);


        //Finish the registration of the new multifactor device
        MultifactorDevice device = doFinishRegistration(startRegistrationResult);
        //As a result the new device should have been returned
        assertThat(device, is(not(nullValue())));
        //..with an ID assigned
        assertThat(device.getId(), not(emptyOrNullString()));
        //..it must match the device ID returned from start registration
        assertThat(device.getId(), is(equalTo((startRegistrationResult.getDeviceId()))));
        //.. and the provider must match
        assertThat(device.getProviderName(), is(equalTo(getProviderName())));

        //Further provider specific validations
        validateRegisteredDevice(device);

        //Ensure the device is now present
        requireDevice(device.getId());
        return startRegistrationResult;
    }

    protected void clearAllMultifactorDevices() throws Exception {
        if (contextId != null && userId != null) {
            getAdminApi().multifactorDeleteDevices(contextId, userId);
        }
    }

    protected void clearAllMultifactorDevicesByUser() throws Exception {
       List<MultifactorDevice> devices = getDevices();
       for(MultifactorDevice device : devices) {
           MultifactorApi().multifactorDeviceActionDelete(device.getProviderName(), device.getId());
       }
    }

    //----------------------------------------------------------------------------------------------

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.contextId = I(testUser.getContextId());
        this.userId = I(testUser.getUserId());
    }

    //----------------------------------------------------------------------------------------------

    @Test
    public void testBackupProviderFlags() throws Exception {
       MultifactorProvider requireProvider = requireProvider(getProviderName());
        assertThat(requireProvider.getBackupProvider(), is(B(isBackupProvider())));
        assertThat(requireProvider.getBackupOnlyProvider(), is(B(isBackupOnlyProvider())));
    }

    @Test
    public void testRegisterUnregisterDevice() throws Exception {
        MultifactorStartRegistrationResponseData registrationResponseData = registerNewDevice();
        unregisterDevice(getProviderName(), registrationResponseData.getDeviceId());
    }

    @Test
    public void testsAuthentication() throws Exception {
        //Performing a complete registration of a new device
        MultifactorStartRegistrationResponseData registrationResponseData = registerNewDevice();

        //Begin authentication against the new device
        MultifactorStartAuthenticationResponseData startAuthenticationResultData = startAuthenticationInternal(registrationResponseData.getDeviceId());
        assertThat(startAuthenticationResultData, is(not(nullValue())));
        assertThat(startAuthenticationResultData.getChallenge(), is(not(nullValue())));
        validateStartAuthenticationResponse(startAuthenticationResultData);

        //Perform authentication
        CommonResponse response = doAuthentication(registrationResponseData, startAuthenticationResultData);
        assertThat(response.getErrorDesc(), is(nullValue()));
    }

    @Test
    public void testWrongAuthentication() throws Exception {
        MultifactorStartRegistrationResponseData registrationResponseData = registerNewDevice();

        MultifactorStartAuthenticationResponseData startAuthenticationResultData = startAuthenticationInternal(registrationResponseData.getDeviceId());
        assertThat(startAuthenticationResultData, is(not(nullValue())));

        validateStartAuthenticationResponse(startAuthenticationResultData);
        CommonResponse response = doWrongAuthentication(registrationResponseData, startAuthenticationResultData);

        //Authentication must have failed and an appropriated error code should have been returned
        assertThat(response.getError(), is(not(nullValue())));
        assertThat(response.getCode(), is("MFA-0023"));
    }

    @Test
    public void testUnregisterDeviceMustNotBeAllowedIfNotAuthenticated() throws Exception {

        //Register a new device
        MultifactorStartRegistrationResponseData registrationResponseData = registerNewDevice();

        //login with a new session but do not provide 2nd factor
        SessionAwareClient client2 = testUser.generateApiClient();
        MultifactorApi multifactorApi = new MultifactorApi(client2);

        //Try to delete the 2nd factor. THIS MUST FAIL.
        MultifactorDeleteResponse deleteResponse = multifactorApi.multifactorDeviceActionDelete(getProviderName(), registrationResponseData.getDeviceId());
        assertThat(deleteResponse.getError(), not(emptyOrNullString()));
        assertThat(deleteResponse.getCode(), is("MFA-0001"));

        //As a result the device must still be present
        requireDevice(registrationResponseData.getDeviceId());
    }

    @Test
    public void testAuthenticationRequiredForAction() throws Exception {
        //Register a new device
        registerNewDevice();

        //login with a new session but do not provide 2nd factor
        SessionAwareClient client2 = testUser.generateApiClient();

        //Perform some API call which is 2fa protected
        //This MUST FAIL, because the 2nd factor was not provided
        CurrentUserResponse currentUser = new UserMeApi(client2).getCurrentUser();
        assertThat(currentUser.getData(), is(nullValue()));
        assertThat(currentUser.getError(), not(emptyOrNullString()));
        assertThat(currentUser.getCode(), is("MFA-0001"));
    }

    @Test
    public void testAuthenticationRequiredForActionWithPublicSessionCookie() throws Exception {
        //Register a new device
        registerNewDevice();

        //login with a new session but do not provide 2nd factor
        SessionAwareClient client2 = testUser.generateApiClient();

        //Perform an API call which does only require a public session cookie for session retrieval.
        //i.e. no session parameter is sent.
        client2.setSession(null);
        //Execute the action without sending a session as parameter.
        //The other parameters sent do not matter, because we expect the action to fail anyway due missing 2FA.
        //We just want to ensure that this API call is rejected.
        byte[] response = new ContactsApi(client2).getContactPictureBuilder().withContactId("123").execute();

        //The response must not be an actual image, or an error message other than the multifactor error
        assertThat("This API call should have been rejected", new String(response, StandardCharsets.UTF_8).contains("MFA-0001"));
    }

    public void testReauthenticationRequiredAfterAutologin() throws Exception {

        //Register a new device and logout
        MultifactorStartRegistrationResponseData deviceData = registerNewDevice();
        testUser.performLogout();

        //Login again with autologin enabled
        LoginApi loginApi = new LoginApi(getApiClient());
        testUser.performLogin("1.0.0");
        loginApi.refreshAutoLoginCookie();

        //..And provide the 2nd factor - Authentication must not fail!
        MultifactorStartAuthenticationResponseData startAuthData = startAuthenticationInternal(deviceData.getDeviceId());
        CommonResponse authResponse = doAuthentication(deviceData, startAuthData);
        assertThat(authResponse.getErrorDesc(), is(nullValue()));

        //Autologin again
        LoginResponse autologin = loginApi.autologin(null, "1.0.0");
        assertThat(autologin.getErrorDesc(), is(nullValue()));

        //After autologin is must be allowed to perform almost all API actions without re-authenticating
        CurrentUserResponse currentUser = new UserMeApi(getApiClient()).getCurrentUser();
        assertThat(currentUser.getErrorDesc(), is(nullValue()));

        //However, it must not be allowed to perform certain API actions which require re-authenticating.
        //For example deleting multifactor devices
        MultifactorDeleteResponse deleteResponse = MultifactorApi().multifactorDeviceActionDelete(getProviderName(), deviceData.getDeviceId());
        assertThat(deleteResponse.getData(), is(nullValue()));
        assertThat(deleteResponse.getError(), not(emptyOrNullString()));
        assertThat(deleteResponse.getCode(), is("MFA-0015"));
    }

    public void testRegisterNewDeviceAfterDeviceDeletedAndAutologin() throws Exception {
        //After the last device was deleted, it should be possible to register new devices again after autologin was performed

        //Register a new device and logout
        MultifactorStartRegistrationResponseData deviceData = registerNewDevice();
        testUser.performLogout();

        //Login again with autologin enabled
        LoginApi loginApi = new LoginApi(getApiClient());
        testUser.performLogin("1.0.0");
        loginApi.refreshAutoLoginCookie();

        //..And provide the 2nd factor - Authentication must not fail!
        MultifactorStartAuthenticationResponseData startAuthData = startAuthenticationInternal(deviceData.getDeviceId());
        CommonResponse authResponse = doAuthentication(deviceData, startAuthData);
        assertThat(authResponse.getErrorDesc(), is(nullValue()));

        //Delete all devices
        clearAllMultifactorDevicesByUser();
        //Ensure all devices are gone
        assertThat(getDevices(),is(empty()));

        //Autologin again
        //This should clear the requirement for "recent authentication" because no device is left
        LoginResponse autologin = loginApi.autologin(null, "1.0.0");
        assertThat(autologin.getErrorDesc(), is(nullValue()));

        //Thus registering new device should be successful
        if (isBackupOnlyProvider()) {
            //A user cannot register a "backup-only" device if no other devices are registered
            //so we start by adding a primary device first
            MultifactorStartRegistrationResponseData registration = startRegistration(TOTPProviderTests.TOTP_PROVIDER_NAME);
            String token = Integer.toString(new TotpGenerator().create(registration.getChallenge().getSharedSecret()));
            finishRegistration(TOTPProviderTests.TOTP_PROVIDER_NAME, registration.getDeviceId(), token, null, null);
        }
        registerNewDevice();
    }

    @Test
    public void testRenameDevice() throws Exception {
        //Register a new device
        MultifactorStartRegistrationResponseData registerData = registerNewDevice();

        //Rename the device
        MultifactorDevice newDevice = new MultifactorDevice();
        final String newDeviceName = "ThisIsANewDeviceName";
        newDevice.setName(newDeviceName);
        newDevice. setId(registerData.getDeviceId());
        MultifactorDeviceResponse renamedResponse = MultifactorApi().multifactorDeviceActionRename(getProviderName(), newDevice);
        assertThat(renamedResponse, is(not(nullValue())));
        MultifactorDevice renamedDevice = checkResponse(renamedResponse, renamedResponse.getData());

        //Validate that the returned device has a new name
        assertThat(renamedDevice, is(not(nullValue())));
        assertThat(renamedDevice.getId(), is(registerData.getDeviceId()));
        assertThat(renamedDevice.getName(), is(newDeviceName));

        //Re-fetch the device and check if the new name is returned
        MultifactorDevice device = getDevice(newDevice.getId()).get();
        assertThat(device.getName(), is(newDeviceName));
    }

    @Test
    public void testRenameDeviceRequiresAuthentication() throws Exception {
        //Register a new device and get it's name
        MultifactorStartRegistrationResponseData registerData = registerNewDevice();
        MultifactorDevice device = requireDevice(registerData.getDeviceId());
        final String deviceName = device.getName();

        //login with a new session but do not provide 2nd factor
        SessionAwareClient client2 = testUser.generateApiClient();

        //Rename the device. THIS MUST FAIL
        MultifactorDevice newDevice = new MultifactorDevice();
        final String newDeviceName = "ThisIsANewDeviceName";
        newDevice.setName(newDeviceName);
        newDevice. setId(registerData.getDeviceId());
        MultifactorDeviceResponse renamedResponse = new MultifactorApi(client2).multifactorDeviceActionRename(getProviderName(), newDevice);
        assertThat(renamedResponse, is(not(nullValue())));
        assertThat(renamedResponse.getError(), not(emptyOrNullString()));
        assertThat(renamedResponse.getCode(), is("MFA-0001"));

        //As a result the device must still have it's old name
        device = requireDevice(registerData.getDeviceId());
        assertThat(device.getName(), is(deviceName));
    }
}
