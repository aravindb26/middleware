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

package com.openexchange.share.groupware;

/**
 * {@link VirtualTargetProxyType} - A generic {@link TargetProxyType} for shares that point to a virtual module iow. a third party plugin
 * like messenger. If needed a more detailed type or further data should be looked up (probably via a service that asks the referenced
 * module).
 *
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 * @since v7.8.0
 */
public class VirtualTargetProxyType implements TargetProxyType {

    private static VirtualTargetProxyType instance = null;

    private VirtualTargetProxyType() {}

    public static VirtualTargetProxyType getInstance() {
        if (instance == null) {
            instance = new VirtualTargetProxyType();
        }
        return instance;
    }

    @Override
    public String getId() {
        return "virtual";
    }

    @Override
    public String getDisplayName() {
        return "virtual";
    }

}
