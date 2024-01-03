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

package com.openexchange.gotenberg.client;

import static com.openexchange.java.Autoboxing.F;
import java.util.Optional;

/**
 * {@link PageProperties} - The page properties to set for a conversion request
 *
 * @see <a href="https://gotenberg.dev/docs/modules/chromium#routes">https://gotenberg.dev/docs/modules/chromium#routes</a>
 */
public class PageProperties {

    /**
     * {@link PagePropertiesBuilder} - Builds the {@link PageProperties}
     */
    public static class PagePropertiesBuilder {

        private Float paperWidth;
        private Float paperHeight;
        private Float marginTop;
        private Float marginBottom;
        private Float marginLeft;
        private Float marginRight;

        public PagePropertiesBuilder withPaperWidth(float paperWidth) {
            this.paperWidth = F(paperWidth);
            return this;
        }

        public PagePropertiesBuilder withPaperHeight(float paperHeight) {
            this.paperHeight = F(paperHeight);
            return this;
        }

        public PagePropertiesBuilder withMarginTop(float marginTop) {
            this.marginTop = F(marginTop);
            return this;
        }

        public PagePropertiesBuilder withMarginBottom(float marginBottom) {
            this.marginBottom = F(marginBottom);
            return this;
        }

        public PagePropertiesBuilder withMarginLeft(float marginLeft) {
            this.marginLeft = F(marginLeft);
            return this;
        }

        public PagePropertiesBuilder withMarginRight(float marginRight) {
            this.marginRight = F(marginRight);
            return this;
        }

        public PageProperties build() {
            //@formatter:off
            return new PageProperties(Optional.ofNullable(paperWidth),
                                      Optional.ofNullable(paperHeight),
                                      Optional.ofNullable(marginTop),
                                      Optional.ofNullable(marginBottom),
                                      Optional.ofNullable(marginLeft),
                                      Optional.ofNullable(marginRight));
            //@formatter:on
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    private Optional<Float> paperWidth = Optional.empty();
    private Optional<Float> paperHeight = Optional.empty();

    private Optional<Float> marginTop = Optional.empty();
    private Optional<Float> marginBottom = Optional.empty();
    private Optional<Float> marginLeft = Optional.empty();
    private Optional<Float> marginRight = Optional.empty();

    //---------------------------------------------------------------------------------------------------------------------------------------

    private PageProperties(Optional<Float> paperWidth, Optional<Float> paperHeight, Optional<Float> marginTop, Optional<Float> marginBottom, Optional<Float> marginLeft, Optional<Float> marginRight) {
        this.paperWidth = paperWidth;
        this.paperHeight = paperHeight;
        this.marginTop = marginTop;
        this.marginBottom = marginBottom;
        this.marginLeft = marginLeft;
        this.marginRight = marginRight;
    }

    //---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the paper width, in inches
     *
     * @return The paper width, in inces
     */
    public Optional<Float> getPaperWidth() {
        return paperWidth;
    }

    /**
     * Gets the paper height, in inches
     *
     * @return
     */
    public Optional<Float> getPaperHeight() {
        return paperHeight;
    }

    /**
     * Sets the paper height, in inches
     *
     * @param paperHeight The paper height, in inches
     */
    public void setPaperHeight(float paperHeight) {
        this.paperHeight = Optional.of(F(paperHeight));
    }

    /**
     * Sets the paper width in inches
     *
     * @param paperWidth The paperWidth to set
     */
    public void setPaperWidth(float paperWidth) {
        this.paperWidth = Optional.of(F(paperWidth));
    }

    /**
     * Gets the top margin, in inches
     *
     * @return The top margin, in inches
     */
    public Optional<Float> getMarginTop() {
        return marginTop;
    }

    /**
     * Sets the top margin, in inches
     *
     * @param marginTop The top margin, in inches
     */
    public void setMarginTop(float marginTop) {
        this.marginTop = Optional.of(F(marginTop));
    }

    /**
     * Gets the bottom margin, in inches
     *
     * @return The bottom margin, in inches
     */
    public Optional<Float> getMarginBottom() {
        return marginBottom;
    }

    /**
     * Sets the bottom margin, in inches
     *
     * @param marginBottom The bottom margin, in inches
     */
    public void setMarginBottom(float marginBottom) {
        this.marginBottom = Optional.of(F(marginBottom));
    }

    /**
     * Gets the left margin, in inches
     *
     * @return The left margin, in inches
     */
    public Optional<Float> getMarginLeft() {
        return marginLeft;
    }

    /**
     * Sets the left margin, in inches
     *
     * @param marginLeft The left margin, in inches
     */
    public void setMarginLeft(float marginLeft) {
        this.marginLeft = Optional.of(F(marginLeft));
    }

    /**
     * Gets the right margin, in inches
     *
     * @return The right margin, in inches
     */
    public Optional<Float> getMarginRight() {
        return marginRight;
    }

    /**
     * Sets the right margin, in inches
     *
     * @param marginRight The right margin, in inches
     */
    public void setMarginRight(float marginRight) {
        this.marginRight = Optional.of(F(marginRight));
    }
}
