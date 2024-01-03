
package com.openexchange.multifactor.provider.webauthn.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.multifactor.Challenge;
import com.openexchange.multifactor.ChallengeAnswer;
import com.openexchange.multifactor.MultifactorDevice;
import com.openexchange.multifactor.MultifactorProvider;
import com.openexchange.multifactor.MultifactorRequest;
import com.openexchange.multifactor.RegistrationChallenge;
import com.openexchange.multifactor.exceptions.MultifactorExceptionCodes;
import com.openexchange.multifactor.provider.u2f.impl.MultifactorU2FProperty;
import com.openexchange.multifactor.provider.u2f.impl.U2FMultifactorDevice;
import com.openexchange.multifactor.provider.u2f.storage.U2FMultifactorDeviceStorage;
import com.openexchange.multifactor.storage.MultifactorTokenStorage;
import com.openexchange.server.ServiceLookup;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.extension.appid.AppId;
import com.yubico.webauthn.extension.appid.InvalidAppIdException;

/**
 * {@link MultifactorWebAuthnProvider}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class MultifactorWebAuthnProvider implements MultifactorProvider {

    public static final String NAME = "WebAuthn";

    private static final Logger LOG = LoggerFactory.getLogger(MultifactorWebAuthnProvider.class);

    private static final String PARAMETER_RESPONSE = "response";
    private static final String PARAMETER_CLIENT_DATA_JSON = "clientDataJSON";
    private static final String PARAMETER_CREDENTIALS_GET_JSON_PARAMETER = "credentialsGetJson";
    private static final String PARAMETER_CHALLENGE = "challenge";

    private final ServiceLookup services;
    private final MultifactorTokenStorage<WebAuthnToken> tokenStorage;

    /**
     * Initializes a new {@link MultifactorWebAuthnProvider}.
     *
     * @param configurationService The {@link LeanConfigurationService} to use
     * @param u2fStorage The {@link U2FMultifactorDeviceStorage} to use for backwards compatibility
     * @param tokenStorage The {@link MultifactorTokenStorage} storage to use for storing challenges
     */
    public MultifactorWebAuthnProvider(ServiceLookup services, MultifactorTokenStorage<WebAuthnToken> tokenStorage) {
        this.services = services;
        this.tokenStorage = tokenStorage;
    }

    /**
     * Internal method to obtain the {@link LeanConfigurationService}
     *
     * @return The configuration service
     * @throws OXException
     */
    private LeanConfigurationService getConfigurationService() throws OXException {
        return services.getServiceSafe(LeanConfigurationService.class);
    }

    /**
     * Internal method to obtain the {@link U2FMultifactorDeviceStorage}
     *
     * @return The u2f storage
     * @throws OXException
     */
    private U2FMultifactorDeviceStorage getU2FStorage() throws OXException {
        return services.getServiceSafe(U2FMultifactorDeviceStorage.class);
    }

    /**
     * Gets the U2F "App ID" for backwards compatibility to U2F
     *
     * @param multifactorRequest
     * @return The U2F "App ID" set in the configuration or the host name of the request as fallback
     * @throws OXException
     */
    private String getU2FAppId(MultifactorRequest multifactorRequest) throws OXException {
        final String appId = getConfigurationService().getProperty(multifactorRequest.getUserId(), multifactorRequest.getContextId(), MultifactorU2FProperty.appId);
        return Strings.isEmpty(appId) ? "https://" + multifactorRequest.getHost() : appId;
    }

    /**
     * Gets the WebAuthn Identity
     *
     * @param multifactorRequest The request to create the identity for
     * @return The WebAuthn identity set in the configuration, or the host name of the request as fallback
     * @throws OXException
     */
    private RelyingPartyIdentity getRelyingPartyIdentity(MultifactorRequest multifactorRequest) throws OXException {
        String id = getConfigurationService().getProperty(multifactorRequest.getUserId(), multifactorRequest.getContextId(), MultifactorWebAuthnProperty.rpId);
        if (Strings.isEmpty(id)) {
            String appId = getConfigurationService().getProperty(multifactorRequest.getUserId(), multifactorRequest.getContextId(), MultifactorU2FProperty.appId);
            if (!Strings.isEmpty(appId)) {
                try {
                    URL appIdUrl = new URL(appId);
                    if (appIdUrl.getPath().isEmpty()) {
                        id = appIdUrl.getHost();
                    }
                } catch (MalformedURLException e) {
                    LOG.info("AppId url malformed");
                }
            }
            if (Strings.isEmpty(id)) {
                id = multifactorRequest.getHost();
            }
        }
        return RelyingPartyIdentity.builder().id(id).name(id).build();
    }

    /**
     * Creating a {@link CredentialRepository} to use
     *
     * @param multifactorRequest The {@link MultifactorRequest} to create the repository for
     * @param u2fStorage The storage to use for u2f backwards compatibility
     * @return The {@link CredentialRepository}
     */
    private CredentialRepository createCredentialRepository(MultifactorRequest multifactorRequest, U2FMultifactorDeviceStorage u2fStorage) {
        return new CompositeCredentialRepository(new U2FCredentialRepository(u2fStorage, multifactorRequest), new WebAuthnCredentialRepository());
    }

    /**
     * Creates the {@link RelyingParty} instance to use
     *
     * @param multifactorRequest The request to create the relying party for
     * @param credentialRepository The credential repository to use
     * @return The {@link RelyingParty} instance for the given request
     * @throws OXException
     */
    private RelyingParty createRelyingParty(MultifactorRequest multifactorRequest, CredentialRepository credentialRepository) throws OXException {
        RelyingParty relyingParty;
        try {
            //@formatter:off
            relyingParty = RelyingParty.builder()
                                       .identity(getRelyingPartyIdentity(multifactorRequest))
                                       .credentialRepository(credentialRepository)
                                       .appId(new AppId(getU2FAppId(multifactorRequest)))  //Setting the legacy U2F App ID enables WebAuhtn extensions and allows client to use U2F devices
                                       .build();
            //@formatter:on
            return relyingParty;
        } catch (@SuppressWarnings("unused") InvalidAppIdException e) {
            throw MultifactorExceptionCodes.ERROR_CREATING_FACTOR.create("Invalid AppId");
        }
    }

    private Duration getTokenLifeTime(MultifactorRequest multifactorRequest) throws OXException {
        final int tokenLifetime = getConfigurationService().getIntProperty(multifactorRequest.getUserId(), multifactorRequest.getContextId(), MultifactorWebAuthnProperty.tokenLifetime);
        return Duration.ofSeconds(tokenLifetime);
    }

    private PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> parsePublicKeyCredentials(ChallengeAnswer answer) throws IOException {
        JSONObject answerJSON = new JSONObject(answer.asMap());
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc = PublicKeyCredential.parseAssertionResponseJson(answerJSON.toString());
        return pkc;
    }

    /**
     * Gets a list of {@link MultifactorDevices}s for the given request being able to use with the WebAuthn provider
     *
     * @param multifactorRequest The request to get the devices for
     * @return A collection of devices for the given request which can be used with WebAuthn
     * @throws OXException
     */
    private Collection<MultifactorDevice> getCompatibleDevices(MultifactorRequest multifactorRequest) throws OXException {
        Collection<MultifactorDevice> ret = new ArrayList<>();
        //By now we are only checking for u2f devices since WebAuthn is not fully supported yet
        ret.addAll(getU2FStorage().getDevices(multifactorRequest.getContextId(), multifactorRequest.getUserId()));
        return ret;
    }

    /**
     * Internal method to increment the U2F signature counter
     *
     * @param u2fdevice The device to increment the counter for
     * @param signatureCounter The new value of the counter
     * @param multifactorRequest The request to update the counter for
     * @throws OXException
     */
    private void incrementU2fSignatureCounter(U2FMultifactorDevice u2fdevice, long signatureCounter, MultifactorRequest multifactorRequest) throws OXException {
        if (!getU2FStorage().incrementCounter(multifactorRequest.getContextId(), multifactorRequest.getUserId(), u2fdevice.getId(), signatureCounter)) {
            LOG.error("Unable to increment U2F counter");
        }
    }

    /**
     * Internal method to return a {@link U2FMultifactorDevice} from the list of given devices by specifying a Credential ID / Key Handle
     *
     * @param credentialId The credential ID (i.e the U2F Key Handle) of the device to get from the list
     * @param devices The list of devices to the device from
     * @return An Optional {@link U2FMultifactorDevice} if found in the list of given devices
     */
    private Optional<U2FMultifactorDevice> getU2FDeviceByCredentialId(ByteArray credentialId, Collection<MultifactorDevice> devices) {
        //@formatter:off
        return devices.stream()
                      .filter(device -> device instanceof U2FMultifactorDevice &&
                                       ((U2FMultifactorDevice)device).getKeyHandle().equals(credentialId.getBase64Url()))
                      .map(device -> (U2FMultifactorDevice) device)
                      .findFirst();
        //@formatter:on
    }

    /**
     * Internal method to return the challenge from the given {@link ChallengeAnswer} instance
     *
     * @param answer The answer to get the challenge from
     * @return The challenge, or null if no challenge was found in the given answer
     * @throws OXException
     * @throws JSONException
     */
    private static String getChallenge(ChallengeAnswer answer) throws OXException, JSONException {
        JSONObject jsonAnswer = new JSONObject(answer.asMap());
        if (jsonAnswer.hasAndNotNull(PARAMETER_RESPONSE)) {
            JSONObject response = jsonAnswer.getJSONObject(PARAMETER_RESPONSE);
            if (response.hasAndNotNull(PARAMETER_CLIENT_DATA_JSON)) {
                JSONObject clientDataJSON = JSONServices.parseObject(Base64.getUrlDecoder().decode(response.getString(PARAMETER_CLIENT_DATA_JSON)));
                if (clientDataJSON.hasAndNotNull(PARAMETER_CHALLENGE)) {
                    return clientDataJSON.getString(PARAMETER_CHALLENGE);
                }
                throw MultifactorExceptionCodes.MISSING_PARAMETER.create(PARAMETER_CHALLENGE);
            }
            throw MultifactorExceptionCodes.MISSING_PARAMETER.create(PARAMETER_CLIENT_DATA_JSON);
        }
        throw MultifactorExceptionCodes.MISSING_PARAMETER.create(PARAMETER_CLIENT_DATA_JSON);
    }

    /**
     * Internal method to perform the authentication
     *
     * @param multifactorRequest The request to perform the authentication for
     * @param answer The answer from the client containing the signed challenge
     * @param userDevices A list of related multifactor devices
     * @throws OXException
     */
    private void doAuthenticationInternal(MultifactorRequest multifactorRequest, ChallengeAnswer answer, Collection<MultifactorDevice> userDevices) throws OXException {

        try {
            //Get the temporary token from the storage by using the provided challenge
            String challenge = getChallenge(answer);
            Optional<WebAuthnToken> webAuthnToken = tokenStorage.getAndRemove(multifactorRequest, challenge);
            if (!webAuthnToken.isPresent()) {
                throw MultifactorExceptionCodes.AUTHENTICATION_FAILED.create();
            }

            AssertionRequest assertionRequest = webAuthnToken.get().getValue();
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc = parsePublicKeyCredentials(answer);
            RelyingParty relyingParty = createRelyingParty(multifactorRequest, createCredentialRepository(multifactorRequest, getU2FStorage()));
            //@formatter:off
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                                                               .request(assertionRequest)
                                                               .response(pkc)
                                                               .build());
            //@formatter:on
            result.getWarnings().forEach(warning -> LOG.warn(warning));

            //Increment the signature counter after use
            Optional<U2FMultifactorDevice> usedU2fDevice = getU2FDeviceByCredentialId(result.getCredentialId(), userDevices);
            if (usedU2fDevice.isPresent()) {
                //The device used was a U2F device
                incrementU2fSignatureCounter(usedU2fDevice.get(), result.getSignatureCount(), multifactorRequest);
            }
            // else if(...) {
            //     //The device used was a WebAuthn device
            //     //incrementWebAuthnSignatureCounter(...)
            // }

        } catch (AssertionFailedException e) {
            LOG.info(e.getMessage());
            throw MultifactorExceptionCodes.AUTHENTICATION_FAILED.create();
        } catch (@SuppressWarnings("unused") IOException e) {
            throw MultifactorExceptionCodes.UNKNOWN_ERROR.create("Failed to verify WebAuthn signature.");
        } catch (@SuppressWarnings("unused") RuntimeException e) {
            //Seems like the library throws a RuntimeException in some cases
            throw MultifactorExceptionCodes.UNKNOWN_ERROR.create("Failed to verify WebAuthn signature.");
        } catch (JSONException e) {
            throw MultifactorExceptionCodes.JSON_ERROR.create(e.getMessage());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled(MultifactorRequest multifactorRequest) {
        try {
            LeanConfigurationService configurationService = getConfigurationService();
            boolean webAuthnEnabled = configurationService.getBooleanProperty(multifactorRequest.getUserId(), multifactorRequest.getContextId(), MultifactorWebAuthnProperty.enabled);
            if (!webAuthnEnabled) {
                //If WebAuthn is not enabled by configuration, we still enabled it if U2F is enabled.
                //This allows backwards compatibility. I.e a U2f user can login via webauthn
                return configurationService.getBooleanProperty(multifactorRequest.getUserId(), multifactorRequest.getContextId(), MultifactorU2FProperty.enabled);
            }
            return webAuthnEnabled;
        } catch (OXException e) {
            LOG.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Collection<? extends MultifactorDevice> getDevices(MultifactorRequest multifactorRequest) throws OXException {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends MultifactorDevice> getEnabledDevices(MultifactorRequest multifactorRequest) throws OXException {
        return Collections.emptyList();
    }

    @Override
    public Optional<? extends MultifactorDevice> getDevice(MultifactorRequest multifactorRequest, String deviceId) throws OXException {
        return Optional.empty();
    }

    @Override
    public RegistrationChallenge startRegistration(MultifactorRequest multifactorRequest, MultifactorDevice device) throws OXException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public MultifactorDevice finishRegistration(MultifactorRequest multifactorRequest, String deviceId, ChallengeAnswer answer) throws OXException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void deleteRegistration(MultifactorRequest multifactorRequest, String deviceId) throws OXException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public boolean deleteRegistrations(int contextId, int userId) throws OXException {
        return true;
    }

    @Override
    public boolean deleteRegistrations(int contextId) throws OXException {
        return false;
    }

    @Override
    public Challenge beginAuthentication(MultifactorRequest multifactorRequest, String deviceId) throws OXException {

        RelyingParty relyingParty = createRelyingParty(multifactorRequest, createCredentialRepository(multifactorRequest, getU2FStorage()));

        /*
         * Setting a user-name causes the CredentialRepository::getCredentialIdsForUsername being called and
         * the result is added to list of credentials returned to the client.
         * Since we do not have unique user names we query them by user-id and context-id passed to the
         * repository within the MultifactorRequest instance.
         */
        //@formatter:off
        AssertionRequest assertionRequest = relyingParty.startAssertion(StartAssertionOptions.builder()
                                                                                             .username("")
                                                                                             .build());
        //@formatter:on
        //assertionRequest.getPublicKeyCredentialRequestOptions().getChallenge()

        //Store the token; we use the challenge as key
        String challenge = assertionRequest.getPublicKeyCredentialRequestOptions().getChallenge().getBase64Url();
        tokenStorage.add(multifactorRequest, challenge, new WebAuthnToken(assertionRequest, getTokenLifeTime(multifactorRequest)));

        return new Challenge() {

            @Override
            public Map<String, Object> getChallenge() throws OXException {
                HashMap<String, Object> result = new HashMap<>(1);
                try {
                    result.put(PARAMETER_CREDENTIALS_GET_JSON_PARAMETER, JSONServices.parseObject(assertionRequest.toCredentialsGetJson()));
                } catch (JsonProcessingException | JSONException e) {
                    throw MultifactorExceptionCodes.JSON_ERROR.create(e.getMessage());
                }
                return result;
            }

        };
    }

    @Override
    public void doAuthentication(MultifactorRequest multifactorRequest, String deviceId, ChallengeAnswer answer) throws OXException {
        Collection<MultifactorDevice> devices = getCompatibleDevices(multifactorRequest);
        if (devices == null || devices.isEmpty()) {
            throw MultifactorExceptionCodes.NO_DEVICES.create();
        }
        doAuthenticationInternal(multifactorRequest, answer, devices);
    }

    @Override
    public MultifactorDevice renameDevice(MultifactorRequest multifactorRequest, MultifactorDevice device) throws OXException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
