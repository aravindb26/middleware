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
package com.openexchange.admin.plugins;

import java.util.List;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.dataobjects.ResourcePermission;

/**
 * @author choeger
 *
 */
public interface OXResourcePluginInterface {
    public void create(final Context ctx, final Resource res, final Credentials cred) throws PluginException;

    public void delete(final Context ctx, final Resource res, final Credentials cred) throws PluginException;

    public void change(final Context ctx, final Resource res, final Credentials auth) throws PluginException;

    public Resource get(final Context ctx, final Resource res, final Credentials cred);

    /**
     * Remove permissions from resource
     * 
     * @param ctx The context object
     * @param res The resource object
     * @param permissions List of permissions to remove
     * @param auth The credentials
     */
    public void removePermissions(Context ctx, Resource res, List<ResourcePermission> permissions, Credentials auth) throws PluginException;
}
