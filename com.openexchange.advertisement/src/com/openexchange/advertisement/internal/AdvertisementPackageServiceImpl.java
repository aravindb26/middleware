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

package com.openexchange.advertisement.internal;

import static com.openexchange.reseller.ResellerExceptionCodes.NO_RESELLER_FOUND;
import static com.openexchange.reseller.ResellerExceptionCodes.NO_RESELLER_FOUND_FOR_CTX;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import com.openexchange.advertisement.AdvertisementConfigService;
import com.openexchange.advertisement.AdvertisementPackageService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadables;
import com.openexchange.exception.OXException;
import com.openexchange.reseller.ResellerService;
import com.openexchange.reseller.data.ResellerAdmin;
import com.openexchange.tools.functions.ErrorAwareSupplier;

/**
 * {@link AdvertisementPackageServiceImpl}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.3
 */
public class AdvertisementPackageServiceImpl implements AdvertisementPackageService {

    private static final Logger LOG = LoggerFactory.getLogger(AdvertisementConfigService.class);
    private static final String CONFIG_SUFFIX = ".packageScheme";

    private final ErrorAwareSupplier<ResellerService> resellerServiceSupplier;
    private final AtomicReference<AdvertisementConfigService> globalReference;
    private final AtomicReference<Map<String, String>> reseller2Scheme;
    private final ConcurrentMap<String, AdvertisementConfigService> configServices;
    private final ConfigurationService configService;

    /**
     * Initializes a new {@link AdvertisementPackageServiceImpl}.
     *
     * @param resellerServiceSupplier A supplier for the reseller service
     * @param configService The configuration service to use
     */
    public AdvertisementPackageServiceImpl(ErrorAwareSupplier<ResellerService> resellerServiceSupplier, ConfigurationService configService) {
        super();
        this.resellerServiceSupplier = resellerServiceSupplier;
        this.configService = configService;
        reseller2Scheme = new AtomicReference<>(null);
        configServices = new ConcurrentHashMap<>(8, 0.9F, 1);
        globalReference = new AtomicReference<>(null);
    }

    @Override
    public AdvertisementConfigService getScheme(int contextId) {
        // Resolve context tom its reseller name
        String reseller;
        try {
            ResellerAdmin admin = resellerServiceSupplier.get().getReseller(contextId);
            reseller = admin.getName();
        } catch (OXException e) {
            if (false == (NO_RESELLER_FOUND.equals(e) ||
                          NO_RESELLER_FOUND_FOR_CTX.equals(e))) {
                return globalReference.get();
            }
            reseller = DEFAULT_RESELLER;
        }

        try {
            Optional<String> optScheme = optSchemeFor(reseller);
            return optScheme.map(s -> configServices.get(s))
                            .orElseGet(() -> globalReference.get());
        } catch (@SuppressWarnings("unused") OXException e) {
            LOG.warn("Missing reseller service. Falling back to default scheme.");
            return globalReference.get();
        }
    }

    @Override
    public AdvertisementConfigService getDefaultScheme() {
        return globalReference.get();
    }

    // ---------------- reloadable implementation -------------

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        try {
            reseller2Scheme.set(computeReseller2SchemeMapping(configService));
        } catch (OXException e) {
            LOG.error("Error while reloading configuration", e);
        }
    }

    @Override
    public Interests getInterests() {
        return Reloadables.getInterestsForAll();
    }

    // ----------------- other public methods -------------

    /**
     * Adds newly appeared advertisement configuration and reloads this advertisement package service.
     *
     * @param advertisementConfig The advertisement configuration to add
     * @return <code>true</code> if successfully added; otherwise <code>false</code>
     */
    public boolean addServiceAndReload(AdvertisementConfigService advertisementConfig) {
        if (null == advertisementConfig || (null != configServices.putIfAbsent(advertisementConfig.getSchemeId(), advertisementConfig))) {
            // Either null or there is already such an AdvertisementConfigService
            return false;
        }

        if (DEFAULT_SCHEME_ID.equals(advertisementConfig.getSchemeId())) {
            globalReference.set(advertisementConfig);
        }
        return true;
    }

    /**
     * Removes disappeared advertisement configuration and reloads this advertisement package service.
     *
     * @param advertisementConfig The advertisement configuration to remove
     */
    public void removeServiceAndReload(AdvertisementConfigService advertisementConfig) {
        if (null != advertisementConfig) {
            AdvertisementConfigService removed = configServices.remove(advertisementConfig.getSchemeId());
            if (null != removed && DEFAULT_SCHEME_ID.equals(removed.getSchemeId())) {
                globalReference.set(null);
            }
        }
    }

    // ---------------------- private methods ---------------------

    /**
     * Gets the optional scheme for the given reseller
     *
     * @return The optional scheme
     * @throws OXException
     */
    private Optional<String> optSchemeFor(String reseller) throws OXException {
        if (reseller2Scheme.get() != null) {
            String result = reseller2Scheme.get().get(reseller);
            return Optional.ofNullable(result);
        }
        synchronized (this) {
            if (reseller2Scheme.get() != null) {
                String scheme = reseller2Scheme.get().get(reseller);
                return Optional.ofNullable(scheme);
            }
            reseller2Scheme.set(computeReseller2SchemeMapping(configService));
            String scheme = reseller2Scheme.get().get(reseller);
            return Optional.ofNullable(scheme);
        }
    }

    /**
     * Calculates the schema 2 reseller mapping
     *
     * @param configService The config service to use
     * @return
     * @throws OXException
     */
    private Map<String, String> computeReseller2SchemeMapping(ConfigurationService configService) throws OXException {
        ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        StringBuilder propNameBuilder = new StringBuilder(AdvertisementConfigService.CONFIG_PREFIX);
        int reslen = propNameBuilder.length();
        boolean containsDefault = false;
        for (ResellerAdmin res : resellerServiceSupplier.get().getAll()) {
            propNameBuilder.setLength(reslen);
            String packageScheme = configService.getProperty(propNameBuilder.append(res.getName()).append(CONFIG_SUFFIX).toString());
            if (packageScheme == null) {
                // Fall-back to reseller identifier
                propNameBuilder.setLength(reslen);
                packageScheme = configService.getProperty(propNameBuilder.append(res.getId()).append(CONFIG_SUFFIX).toString());

                if (packageScheme == null) {
                    // Fall-back to global as last resort
                    packageScheme = DEFAULT_SCHEME_ID;
                }
            }
            if (res.getName().equals(DEFAULT_RESELLER)) {
                containsDefault = true;
            }
            map.put(res.getName(), packageScheme);
        }

        if (!containsDefault) {
            // Add 'default' as a default reseller
            String oxall = DEFAULT_RESELLER;

            propNameBuilder.setLength(reslen);
            String packageScheme = configService.getProperty(propNameBuilder.append(oxall).append(CONFIG_SUFFIX).toString());
            if (packageScheme == null) {
                //fallback to global
                packageScheme = DEFAULT_SCHEME_ID;
            }
            map.put(oxall, packageScheme);
        }
        return map.build();
    }

}
