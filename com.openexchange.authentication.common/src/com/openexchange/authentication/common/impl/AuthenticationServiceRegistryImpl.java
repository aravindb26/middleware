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

package com.openexchange.authentication.common.impl;

import static com.openexchange.authentication.AuthenticationResult.Status.FAILED;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableList;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.authentication.BasicAuthenticationService;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.application.exceptions.AppPasswordExceptionCodes;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.exception.OXException;

/**
 * {@link AuthenticationServiceRegistryImpl} - Collects all registered {@link AuthenticationService} implementations, sorts them by service ranking and selects responsible
 * service for every request by checking request parameters like refering hostname, headers or cookies.
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class AuthenticationServiceRegistryImpl implements AuthenticationServiceRegistry, Reloadable {

    private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationServiceRegistryImpl.class);

    private final AtomicReference<BasicAuthenticationService> basicAuthenticationService;
    private final Comparator<AuthenticationService> comparator;
    private final AtomicReference<List<AuthenticationService>> authenticationServicesReference;

    /**
     * Initializes a new {@link AuthenticationServiceRegistryImpl}.
     */
    public AuthenticationServiceRegistryImpl() {
        super();
        basicAuthenticationService = new AtomicReference<>();
        comparator = new Comparator<AuthenticationService>() {

            @Override
            public int compare(AuthenticationService o1, AuthenticationService o2) {
                // Swap parameters to get a sort order from higher ranking to lower ranking
                int result = Integer.compare(o2.getRanking(), o1.getRanking());
                return result == 0 ? o1.getIdentifier().compareTo(o2.getIdentifier()) : result;
            }
        };
        authenticationServicesReference = new AtomicReference<List<AuthenticationService>>(null);
    }

    @Override
    public AuthenticationResult doLogin(AuthenticationRequest authenticationRequest, boolean autologin) throws OXException {
        List<AuthenticationService> authenticationServices = authenticationServicesReference.get();
        if (authenticationServices == null) {
            // No authentication services available
            LOG.error("No authentication service available to handle login request {} ({}).", authenticationRequest.getLogin(), mapToString(authenticationRequest.getProperties()));
        } else {
            // Find suitable authentication service
            FailedAuthenticationResult failedAuthResult = null;
            for (AuthenticationService service : authenticationServices) {
                AuthenticationResult result = service.doLogin(authenticationRequest, autologin);
                if (FAILED.equals(result.getStatus())) {
                    failedAuthResult = handleFailedAuthentication(service, result, authenticationRequest);
                    if (failedAuthResult.dontTryOther()) {
                        LOG.debug("Login failed with all authentication services for login request {} ({}).", authenticationRequest.getLogin(), mapToString(authenticationRequest.getProperties()));
                        return handleFailedAuthResult(failedAuthResult, autologin);
                    }
                } else {
                    LOG.debug("Login successful with authentication service {} for login request {} ({})", service.getIdentifier(), authenticationRequest.getLogin(), mapToString(authenticationRequest.getProperties()));
                    return result;
                }
            }
            if (null != failedAuthResult) {
                return handleFailedAuthResult(failedAuthResult, autologin);
            }
        }
        return createGenericFailedAuthResult(autologin);
    }

    /**
     * Handles {@link FailedAuthenticationResult}s
     *
     * @param result The result to handle
     * @param autologin <code>true</code> if services should try an auto-login attempt, <code>false</code> otherwise
     * @return The {@link AuthenticationResult}
     * @throws OXException in case the result contains an error
     */
    private AuthenticationResult handleFailedAuthResult(FailedAuthenticationResult result, boolean autologin) throws OXException {
        if (result.optError().isPresent()) {
            throw result.optError().get();
        }
        return result.optResult().orElseGet(() -> createGenericFailedAuthResult(autologin));
    }

    /**
     * Creates a generic failed {@link AuthenticationResult}
     *
     * @param autologin <code>true</code> if services should try an auto-login attempt, <code>false</code> otherwise
     * @return The failed {@link AuthenticationResult}
     */
    private AuthenticationResult createGenericFailedAuthResult(boolean autologin) {
        if (autologin) {
            return AuthenticationResult.failed(LoginExceptionCodes.INVALID_COOKIE.create());
        }
        return AuthenticationResult.failed(LoginExceptionCodes.INVALID_CREDENTIALS.create());
    }

    /**
     * Handles failed {@link AuthenticationResult}s
     *
     * @param service The current tested service
     * @param result The failed {@link AuthenticationResult}
     * @param authenticationRequest The authentication request
     * @return The {@link FailedAuthenticationResult}
     */
    public FailedAuthenticationResult handleFailedAuthentication(AuthenticationService service, AuthenticationResult result, AuthenticationRequest authenticationRequest) {
        Optional<OXException> optionalException = result.getException();
        if (optionalException.isPresent()) {
            OXException x = optionalException.get();
            if (AppPasswordExceptionCodes.UNSUPPORTED_CLIENT.equals(x)) {
                // not an app-specific password, try next service
                return new FailedAuthenticationResult(null, true, null);
            }
            if (AppPasswordExceptionCodes.INVALID_CREDENTIALS.equals(x)) {
                LOG.debug("Password in app-specific password format, but invalid.");
                return new FailedAuthenticationResult(x, true, null);
            }
            LOG.debug("Login failed with authentication service {} for login request {} ({}).", service.getIdentifier(), authenticationRequest.getLogin(), mapToString(authenticationRequest.getProperties()));
            if ("SVL".equals(x.getPrefix()) && 8 == x.getCode()) {
                // Failed auto-login attempt
                return new FailedAuthenticationResult(x, false, null);
            }
            return new FailedAuthenticationResult(null, false, AuthenticationResult.failed(x));
        }
        LOG.debug("Authentication service {} skipped for login request {} ({}).", service.getIdentifier(), authenticationRequest.getLogin(), mapToString(authenticationRequest.getProperties()));
        return new FailedAuthenticationResult(null, true, null);
    }

    /**
     * Adds given authentication service.
     *
     * @param authenticationService The added authentication service
     */
    public void addService(AuthenticationService authenticationService) {
        List<AuthenticationService> current;
        List<AuthenticationService> newList;
        do {
            current = authenticationServicesReference.get();
            if (current == null) {
                newList = new ArrayList<AuthenticationService>(1);
                newList.add(authenticationService);
            } else {
                newList = new ArrayList<AuthenticationService>(current);
                newList.add(authenticationService);
                Collections.sort(newList, comparator);
            }
        } while (authenticationServicesReference.compareAndSet(current, ImmutableList.copyOf(newList)) == false);
    }

    /**
     * Removes specified authentication service.
     *
     * @param authenticationService The service to remove
     */
    public void removeService(AuthenticationService authenticationService) {
        List<AuthenticationService> current;
        List<AuthenticationService> newList;
        do {
            current = authenticationServicesReference.get();
            if (current == null) {
                // Nothing to remove
                return;
            }
            newList = new ArrayList<AuthenticationService>(current);
            boolean removed = newList.remove(authenticationService);
            if (removed == false) {
                // Nothing removed
                return;
            }
            if (newList.isEmpty()) {
                newList = null;
            }
        } while (authenticationServicesReference.compareAndSet(current, newList == null ? ImmutableList.of() : ImmutableList.copyOf(newList)) == false);
    }

    /**
     * Sets the basic authentication service.
     *
     * @param service The basic authentication service
     */
    public void setBasicAuthenticationService(BasicAuthenticationService service) {
        if (null == service) {
            LOG.info("Removing basic authentication service.");
            basicAuthenticationService.set(null);
            return;
        }
        BasicAuthenticationService previous = basicAuthenticationService.getAndSet(service);
        if (null == previous) {
            LOG.info("Set basic authentication service {}.", service.getIdentifier());
        } else {
            LOG.warn("Replaced basic authentication service {} by {}", previous.getIdentifier(), service.getIdentifier());
        }
    }

    @Override
    public Optional<BasicAuthenticationService> optBasicAuthenticationService() {
        BasicAuthenticationService service = basicAuthenticationService.get();
        if (null == service) {
            LOG.warn("No basic authentication service set.");
        }
        return Optional.ofNullable(service);
    }

    /**
     * Formats keys and values of a {@link Map} for logging purposes
     *
     * @param map The map to format
     * @return The resulting string for logging
     */
    private static String mapToString(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(',');
        }
        sb.setLength(sb.length() - 1);
        sb.append(']');
        return sb.toString();
    }

    /**
     * {@link FailedAuthenticationResult} contains internal informations about what to do with a failed authentication result
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @since v8.0.0
     */
    private static class FailedAuthenticationResult {

        private final boolean tryOther;
        private final Optional<OXException> error;
        private final Optional<AuthenticationResult> result;

        /**
         * Initializes a new {@link FailedAuthenticationResult}.
         *
         * @param error An optional error
         * @param tryOther Whether to try other {@link AuthenticationService}s
         * @param result An optional result to return
         */
        FailedAuthenticationResult(OXException error, boolean tryOther, AuthenticationResult result) {
            super();
            this.tryOther = tryOther;
            this.error = Optional.ofNullable(error);
            this.result = Optional.ofNullable(result);
        }

        /**
         * Whether to try other {@link AuthenticationService}s or not
         *
         * @return <code>true</code> to try other services, <code>false</code> otherwise
         */
        boolean tryOther() {
            return tryOther;
        }

        /**
         * Whether to <b>not</b> try other {@link AuthenticationService}s
         *
         * @return <code>true</code> to <b>not</b> try other services, <code>false</code> otherwise
         */
        boolean dontTryOther() {
            return tryOther() == false;
        }

        /**
         * Gets the optional error
         *
         * @return The optional error
         */
        Optional<OXException> optError() {
            return error;
        }

        /**
         * Gets the optional result
         *
         * @return The optional result
         */
        public Optional<AuthenticationResult> optResult() {
            return result;
        }

    }

    @Override
    public Interests getInterests() {
        return Reloadables.interestsForProperties("com.openexchange.authentication.*");
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        List<AuthenticationService> current;
        List<AuthenticationService> newList;
        do {
            current = authenticationServicesReference.get();
            if (current == null) {
                return;
            }
            newList = new ArrayList<AuthenticationService>(current);
            Collections.sort(newList, comparator);
        } while (authenticationServicesReference.compareAndSet(current, ImmutableList.copyOf(newList)) == false);
    }

}
