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

package com.openexchange.imagetransformation;

import static com.openexchange.java.Autoboxing.I;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.exception.OXException;
import com.openexchange.imagetransformation.internal.DefaultConfig;
import com.openexchange.imagetransformation.internal.StaticConfig;
import com.openexchange.imagetransformation.osgi.Services;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.session.UserAndContext;

/**
 * {@link ImageTransformationConfig} - Provides access to image transformation configration.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public abstract class ImageTransformationConfig {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ImageTransformationConfig.class);
    }

    static {
        ImageTransformationReloadable.getInstance().addReloadable(new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                CONFIGS.invalidateAll();
            }

            @Override
            public Interests getInterests() {
                return null;
            }
        });
    }

    private static final Cache<UserAndContext, ImageTransformationConfig> CONFIGS = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).build();

    /**
     * Gets the user-aware (if possible) image transformation configuration.
     *
     * @return The configuration
     */
    public static ImageTransformationConfig getConfig() {
        // Try to determine by user
        int contextId = Strings.parseUnsignedInt(LogProperties.get(LogProperties.Name.SESSION_CONTEXT_ID));
        if (contextId < 0) {
            // No user-specific config selectable
            return StaticConfig.getInstance();
        }
        int userId = Strings.parseUnsignedInt(LogProperties.get(LogProperties.Name.SESSION_USER_ID));
        if (userId < 0) {
            // No user-specific config selectable
            return StaticConfig.getInstance();
        }

        // Check for required service
        ConfigViewFactory factory = Services.optService(ConfigViewFactory.class);
        if (factory == null) {
            // No user-specific config selectable
            return StaticConfig.getInstance();
        }

        try {
            return getConfig(userId, contextId, factory);
        } catch (Exception e) {
            LoggerHolder.LOG.warn("Failed to load user-specific image transformation configuration. Returning static one instead...", e);
            return StaticConfig.getInstance();
        }
    }

    /**
     * Gets the user-aware image transformation configuration.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param viewFactory The configuration view factory to use
     * @return The configuration
     * @throws OXException If user-aware image transformation configuration cannot be loaded
     */
    public static ImageTransformationConfig getConfig(int userId, int contextId, ConfigViewFactory viewFactory) throws OXException {
        // Check cache first
        UserAndContext key = UserAndContext.newInstance(userId, contextId);
        ImageTransformationConfig config = CONFIGS.getIfPresent(key);
        if (config != null) {
            return config;
        }

        // Atomically load if necessary
        try {
            return CONFIGS.get(key, new ConfigLoader(userId, contextId, viewFactory));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException oxe) {
                throw oxe;
            }
            throw OXException.general("Failed to load user-specific image transformation configuration.", cause == null ? e : cause);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link ImageTransformationConfig}.
     */
    protected ImageTransformationConfig() {
        super();
    }

    /**
     * Gets the max. height allowed for scaling
     *
     * @return The max. height allowed for scaling
     */
    public abstract int getMaxHeight();

    /**
     * Gets the max. width allowed for scaling
     *
     * @return The max. width allowed for scaling
     */
    public abstract int getMaxWidth();

    /**
     * Gets the transformation wait timeout seconds
     *
     * @return The transformation wait timeout seconds
     */
    public abstract int getTransformationWaitTimeoutSeconds();

    /**
     * Gets the transformation max. size
     *
     * @return The transformation max. size
     */
    public abstract long getTransformationMaxSize();

    /**
     * Gets the transformation max. resolution
     *
     * @return The transformation max. resolution
     */
    public abstract long getTransformationMaxResolution();

    /**
     * Gets the transformation prefer thumbnail threshold
     *
     * @return The transformation prefer thumbnail threshold
     */
    public abstract float getTransformationPreferThumbnailThreshold();

    /**
     * Gets the maximum width of the target image.
     *
     * @param maxWidth The requested maximum width of the target image
     * @return The maximum width of the target image possible aligned max. supported width
     */
    public int adjustMaxWidthIfNecessary(int maxWidth) {
        if (maxWidth > getMaxWidth()) {
            LoggerHolder.LOG.info("Requested width {} for image transformation exceeds max. supported width {}. Adjusting width...", I(maxWidth), I(getMaxWidth()));
            return getMaxWidth();
        }

        // Return as-is
        return maxWidth;
    }

    /**
     * Gets the maximum height of the target image.
     *
     * @param maxHeight The requested maximum height of the target image
     * @return The maximum height of the target image possible aligned max. supported height
     */
    public int adjustMaxHeightIfNecessary(int maxHeight) {
        if (maxHeight > getMaxHeight()) {
            LoggerHolder.LOG.info("Requested height {} for image transformation exceeds max. supported height {}. Adjusting height...", I(maxHeight), I(getMaxHeight()));
            return getMaxHeight();
        }

        // Return as-is
        return maxHeight;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class ConfigLoader implements Callable<ImageTransformationConfig> {

        private final int userId;
        private final int contextId;
        private final ConfigViewFactory factory;

        /**
         * Initializes a new {@link ConfigLoader}.
         *
         * @param userId The user identifier
         * @param contextId The context identifier
         * @param factory The config-cascade service to use
         */
        ConfigLoader(int userId, int contextId, ConfigViewFactory factory) {
            super();
            this.userId = userId;
            this.contextId = contextId;
            this.factory = factory;
        }

        @Override
        public ImageTransformationConfig call() throws OXException {
            ConfigView view = factory.getView(userId, contextId);
            DefaultConfig.Builder config = DefaultConfig.builder();

            {
                int defaultMaxHeight = 4096;
                int maxHeight = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.tools.images.maxHeight", defaultMaxHeight, view);
                config.withMaxHeight(maxHeight);
            }

            {
                int defaultMaxWidth = 4096;
                int maxWidth = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.tools.images.maxWidth", defaultMaxWidth, view);
                config.withMaxWidth(maxWidth);
            }

            {
                int defaultWaitTimeoutSeconds = 10;
                int transformationWaitTimeoutSeconds = ConfigViews.getDefinedIntPropertyFrom("com.openexchange.tools.images.transformations.waitTimeoutSeconds", defaultWaitTimeoutSeconds, view);
                config.withTransformationWaitTimeoutSeconds(transformationWaitTimeoutSeconds);
            }

            {
                long defaultTransformationMaxSize = 10485760; // 10 MB
                long transformationMaxSize = ConfigViews.getDefinedLongPropertyFrom("com.openexchange.tools.images.transformations.maxSize", defaultTransformationMaxSize, view);
                config.withTransformationMaxSize(transformationMaxSize);
            }

            {
                long defaultTransformationMaxResolution = 26824090; // ~ 6048x4032 (24 megapixels) + 10%
                long transformationMaxResolution = ConfigViews.getDefinedLongPropertyFrom("com.openexchange.tools.images.transformations.maxResolution", defaultTransformationMaxResolution, view);
                config.withTransformationMaxResolution(transformationMaxResolution);
            }

            {
                float defaultPreferThumbnailThreshold = 0.8f;
                float transformationPreferThumbnailThreshold = ConfigViews.getDefinedFloatPropertyFrom("com.openexchange.tools.images.transformations.preferThumbnailThreshold", defaultPreferThumbnailThreshold, view);
                config.withTransformationPreferThumbnailThreshold(transformationPreferThumbnailThreshold);
            }

            return config.build();
        }
    }

}
