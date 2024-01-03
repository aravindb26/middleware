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

package com.openexchange.mail.categories;

/**
 * {@link MailCategoryConfig}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public class MailCategoryConfig {

    /**
     * Builder for {@link MailCategoryConfig} instances;
     */
    public static class Builder {

        private String category;
        private String flag;
        private boolean force;
        private boolean enabled;
        private String name;
        private String description;
        private boolean isSystemCategory = false;

        /**
         * Initializes a new {@link Builder}.
         */
        public Builder() {
            super();
        }

        /**
         * Sets the category identifier.
         *
         * @param category The category identifier
         * @return This builder
         */
        public Builder category(String category) {
            this.category = category;
            return this;
        }

        /**
         * Sets the flag name.
         *
         * @param flag The flag name
         * @return This builder
         */
        public Builder flag(String flag) {
            this.flag = flag;
            return this;
        }


        /**
         * Sets the <code>force</code> flag.
         *
         * @param force The <code>force</code> flag
         * @return This builder
         */
        public Builder force(boolean force) {
            this.force = force;
            return this;
        }

        /**
         * Sets the <code>enabled</code> flag.
         *
         * @param active The <code>enabled</code> flag
         * @return This builder
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the <code>isSystemCategory</code> flag.
         *
         * @param isSystemCategory The <code>isSystemCategory</code> flag
         * @return This builder
         */
        public Builder isSystemCategory(boolean isSystemCategory) {
            this.isSystemCategory = isSystemCategory;
            return this;
        }

        /**
         * Sets the name.
         *
         * @param name The name
         * @return This builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the {@link MailCategoryConfig} instance according to this builder's settings.
         *
         * @return The new {@link MailCategoryConfig} instance
         */
        public MailCategoryConfig build() {
            return new MailCategoryConfig(category, flag, force, enabled, name, isSystemCategory, description);
        }

    }// End of class Builder

    /**
     * Creates a copy of specified instance with given active flag applied.
     *
     * @param categoryConfig The instance to copy from
     * @param active The <code>active</code> flag
     * @return The copied instance
     */
    public static MailCategoryConfig copyOf(MailCategoryConfig categoryConfig, boolean active) {
        if (null == categoryConfig) {
            return null;
        }

        return new MailCategoryConfig(categoryConfig.category, categoryConfig.flag, categoryConfig.force, active, categoryConfig.name, categoryConfig.isSystemCategory, categoryConfig.description);
    }

    /**
     * Creates a copy of specified instance..
     *
     * @param categoryConfig The instance to copy from
     * @return The copied instance
     */
    public static MailCategoryConfig copyOf(MailCategoryConfig categoryConfig) {
        return copyOf(categoryConfig, categoryConfig.enabled);
    }

    // ----------------------------------------------------------------------------------------------------------

    private final String category;
    private final String flag;
    private final boolean force;
    private final boolean enabled;
    private final String name;
    private final boolean isSystemCategory;
    private final String description;
    private int hash = 0;

    /**
     * Initializes a new {@link MailCategoryConfig}.
     */
    MailCategoryConfig(String category, String flag, boolean force, boolean enabled, String name, boolean isSystemCategory, String description) {
        super();
        this.category = category;
        this.flag = flag;
        this.force = force;
        this.enabled = enabled;
        this.isSystemCategory = isSystemCategory;
        this.name = name;
        this.description = description;

    }

    /**
     * Gets the category identifier
     *
     * @return The category name
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets the flag name (to be used for search/filter expressions).
     *
     * @return The flag name
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Checks if the associated mail category is forced to be active.
     *
     * @return <code>true</code> if forcefully active; otherwise <code>false</code>
     */
    public boolean isForced() {
        return force;
    }

    /**
     * Checks if the associated mail category is active.
     *
     * @return <code>true</code> if active; otherwise <code>false</code>
     */
    public boolean isActive() {
        return force || enabled;
    }

    /**
     * Checks if the associated mail category is enabled.
     *
     * @return <code>true</code> if enabled; otherwise <code>false</code>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if the associated mail category is a system category.
     *
     * @return <code>true</code> if it is a system category; otherwise <code>false</code>
     */
    public boolean isSystemCategory() {
        return isSystemCategory;
    }

    /**
     * Gets the name.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString(){
        return getCategory();
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            final int prime = 31;
            result = 1;
            result = prime * result + ((category == null) ? 0 : category.hashCode());
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MailCategoryConfig)) {
            return false;
        }
        MailCategoryConfig other = (MailCategoryConfig) obj;
        if (category == null) {
            if (other.category != null) {
                return false;
            }
        } else if (!category.equals(other.category)) {
            return false;
        }
        return true;
    }

}
