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

package com.openexchange.imagetransformation.internal;

import com.openexchange.imagetransformation.ImageTransformationConfig;

/**
 * {@link DefaultConfig} - The default configuration implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class DefaultConfig extends ImageTransformationConfig {

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DefaultConfig</code> */
    public static class Builder {

        private int maxHeight;
        private int maxWidth;
        private int transformationWaitTimeoutSeconds;
        private long transformationMaxSize;
        private long transformationMaxResolution;
        private float transformationPreferThumbnailThreshold;

        Builder() {
            this.maxHeight = 4096;
            this.maxWidth = 4096;
            this.transformationWaitTimeoutSeconds = 10;
            this.transformationMaxSize = 10485760; // 10 MB
            this.transformationMaxResolution = 26824090; // ~ 6048x4032 (24 megapixels) + 10%
            this.transformationPreferThumbnailThreshold = 0.8f;
        }

        /**
         * Sets the max. height allowed for scaling.
         *
         * @param maxHeight The max. height to set
         * @return This builder
         */
        public Builder withMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        /**
         * Sets the max. width allowed for scaling.
         *
         * @param maxWidth The max. width to set
         * @return This builder
         */
        public Builder withMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        /**
         * Sets the transformation wait timeout seconds.
         *
         * @param transformationWaitTimeoutSeconds The transformations wait timeout seconds to set
         * @return This builder
         */
        public Builder withTransformationWaitTimeoutSeconds(int transformationWaitTimeoutSeconds) {
            this.transformationWaitTimeoutSeconds = transformationWaitTimeoutSeconds;
            return this;
        }

        /**
         * Sets the transformation max. size.
         *
         * @param transformationMaxSize The transformations max. size to set
         * @return This builder
         */
        public Builder withTransformationMaxSize(long transformationMaxSize) {
            this.transformationMaxSize = transformationMaxSize;
            return this;
        }

        /**
         * Sets the transformation max. resolution.
         *
         * @param transformationMaxResolution The transformation max. resolution to set
         * @return This builder
         */
        public Builder withTransformationMaxResolution(long transformationMaxResolution) {
            this.transformationMaxResolution = transformationMaxResolution;
            return this;
        }

        /**
         * Sets the transformation prefer thumbnail threshold.
         *
         * @param transformationPreferThumbnailThreshold The transformations prefer thumbnail threshold to set
         * @return This builder
         */
        public Builder withTransformationPreferThumbnailThreshold(float transformationPreferThumbnailThreshold) {
            this.transformationPreferThumbnailThreshold = transformationPreferThumbnailThreshold;
            return this;
        }

        /**
         * Creates the appropriate instance of <code>DefaultConfig</code> from this builder's properties.
         *
         * @return The resulting instance of <code>DefaultConfig</code>
         */
        public DefaultConfig build() {
            return new DefaultConfig(maxHeight, maxWidth, transformationWaitTimeoutSeconds, transformationMaxSize, transformationMaxResolution, transformationPreferThumbnailThreshold);
        }

    } // End of class Builder

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int maxHeight;
    private final int maxWidth;
    private final int transformationWaitTimeoutSeconds;
    private final long transformationMaxSize;
    private final long transformationMaxResolution;
    private final float transformationPreferThumbnailThreshold;

    /**
     * Initializes a new {@link ImageTransformationConfig}.
     *
     * @param maxHeight The max. height allowed for scaling
     * @param maxWidth The max. width allowed for scaling
     * @param transformationWaitTimeoutSeconds The transformation wait timeout seconds
     * @param transformationMaxSize The transformation max. size
     * @param transformationMaxResolution The transformation max. resolution
     * @param transformationPreferThumbnailThreshold The transformation prefer thumbnail threshold
     */
    DefaultConfig(int maxHeight, int maxWidth, int transformationWaitTimeoutSeconds, long transformationMaxSize, long transformationMaxResolution, float transformationPreferThumbnailThreshold) {
        super();
        this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
        this.transformationWaitTimeoutSeconds = transformationWaitTimeoutSeconds;
        this.transformationMaxSize = transformationMaxSize;
        this.transformationMaxResolution = transformationMaxResolution;
        this.transformationPreferThumbnailThreshold = transformationPreferThumbnailThreshold;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getMaxWidth() {
        return maxWidth;
    }

    @Override
    public int getTransformationWaitTimeoutSeconds() {
        return transformationWaitTimeoutSeconds;
    }

    @Override
    public long getTransformationMaxSize() {
        return transformationMaxSize;
    }

    @Override
    public long getTransformationMaxResolution() {
        return transformationMaxResolution;
    }

    @Override
    public float getTransformationPreferThumbnailThreshold() {
        return transformationPreferThumbnailThreshold;
    }

}
