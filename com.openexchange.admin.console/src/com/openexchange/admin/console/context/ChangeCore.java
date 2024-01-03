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
package com.openexchange.admin.console.context;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.console.context.extensioninterfaces.ContextConsoleChangeInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;

public abstract class ChangeCore extends ContextAbstraction {

    protected void setOptions(final AdminParser parser) {
        setDefaultCommandLineOptionsWithoutContextID(parser);

        setContextOption(parser, NeededQuadState.eitheror);
        setContextNameOption(parser, NeededQuadState.eitheror);

        setContextQuotaOption(parser, false);

        setFurtherOptions(parser);

        parser.allowDynamicOptions(dynamicAttrDesc, dynamicRemoveAttrDesc);
    }

    protected final void commonfunctions(final AdminParser parser, final String[] args) {
        setOptions(parser);
        setExtensionOptions(parser, ContextConsoleChangeInterface.class);

        String successtext = null;
        try {
            Context ctx = null;
            Credentials auth = null;
            try {
                parser.ownparse(args);
                ctx = contextparsing(parser);

                // context name
                parseAndSetContextName(parser, ctx);

                auth = credentialsparsing(parser);

                successtext = nameOrIdSetInt(this.ctxid, this.contextname, "context");

                // context filestore quota
                parseAndSetContextQuota(parser, ctx);

                parseAndSetExtensions(parser, ctx, auth);

                // Dynamic Options
                applyDynamicOptionsToContext(parser, ctx);

            } catch (RuntimeException e) {
                printError(null, null, e.getClass().getSimpleName() + ": " + e.getMessage(), parser);
                sysexit(1);
            }
            maincall(parser, ctx, auth);
        } catch (Exception e) {
            printErrors(successtext, null, e, parser);
        }
        try {
            displayChangedMessage(successtext, null, parser);
            sysexit(0);
        } catch (RuntimeException e) {
            printError(null, null, e.getClass().getSimpleName() + ": " + e.getMessage(), parser);
            sysexit(1);
        }
    }

    protected abstract void maincall(final AdminParser parser, final Context ctx, final Credentials auth) throws MalformedURLException, RemoteException, NotBoundException, InvalidCredentialsException, NoSuchContextException, StorageException, InvalidDataException;

    protected abstract void setFurtherOptions(final AdminParser parser);
}
