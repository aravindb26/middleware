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
package com.openexchange.admin.console;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.DuplicateExtensionException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.MissingOptionException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchGroupException;
import com.openexchange.admin.rmi.exceptions.NoSuchResourceException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.java.Strings;

/**
 * This abstract class declares an abstract method to get the object name with which the command line tool
 * deals. This is used for output
 *
 * @author d7
 *
 */
public abstract class ObjectNamingAbstraction extends BasicCommandlineOptions {

    protected abstract String getObjectName();

    protected final void displayCreatedMessage(final String id, final Integer ctxid, final AdminParser parser) {
        createMessageForStdout(id, ctxid, "created", parser);
    }

    protected final void displayChangedMessage(final String id, final Integer ctxid, final AdminParser parser) {
        createMessageForStdout(id, ctxid, "changed", parser);
    }

    protected final void displayDeletedMessage(final String id, final Integer ctxid, final AdminParser parser) {
        createMessageForStdout(id, ctxid, "deleted", parser);
    }

    protected void createMessageForStdout(final String id, final Integer ctxid, final String type, final AdminParser parser) {
        createMessage(id, ctxid, type, System.out, parser, false);
    }

    protected void createMessageForStdout(final String message, final AdminParser parser) {
        createMessage(message, System.out, parser, false);
    }

    protected void createLinefeedForStdout(AdminParser parser) {
        createMessage("", System.out, parser, false);
    }

    /**
     * @param id
     * @param ctxid
     * @param type
     * @param ps
     * @param parser
     * @param followingtext Used to define if further text is following, this is especially
     *        used for displaying error messages, because with the nonl option we have to
     *        remove the newlines
     */
    private void createMessage(final String id, final Integer ctxid, final String type, final PrintStream ps, final AdminParser parser, final boolean followingtext) {
        final StringBuilder sb = new StringBuilder(getObjectName());
        if (null != id) {
            sb.append(' ');
            sb.append(id);
        }
        if (null != ctxid) {
            sb.append(" in context ");
            sb.append(ctxid);
        }
        if (null != type) {
            sb.append(' ');
            sb.append(type);
        }
        createMessage(sb.toString(), ps, parser, followingtext);
    }

    private void createMessage(String message, PrintStream ps, AdminParser parser, boolean followingtext) {
        if (null != parser && parser.checkNoNewLine()) {
            String output = null == message || 0 == message.length() ? "" : Strings.dropCRLFFrom(message);
            if (followingtext) {
                ps.print(output);
            } else {
                ps.println(output);
            }
        } else {
            ps.println(message);
        }
    }

    protected void createMessageForStderr(final String id, final Integer ctxid, final String type, final AdminParser parser) {
        createMessage(id, ctxid, type, System.err, parser, true);
    }

    protected final void printError(final String id, final Integer ctxid, final String msg, final AdminParser parser) {
        printFirstPartOfErrorText(id, ctxid, parser);
        printError(msg, parser);
    }

    protected final void printInvalidInputMsg(final String id, final Integer ctxid, final String msg, final AdminParser parser) {
        printFirstPartOfErrorText(id, ctxid, parser);
        printInvalidInputMsg(msg);
    }

    protected void printServerException(final String id, final Integer ctxid, final Exception e, final AdminParser parser) {
        printFirstPartOfErrorText(id, ctxid, parser);
        printServerException(e, parser);
    }

    protected final void printNotBoundResponse(@SuppressWarnings("unused") final Integer id, @SuppressWarnings("unused") final Integer ctxid, final NotBoundException nbe){
        System.err.println("RMI module "+nbe.getMessage()+" not available on server");
    }

    protected void printFirstPartOfErrorText(final String id, final Integer ctxid, final AdminParser parser) {
        if (getClass().getName().matches("^.*\\.\\w*(?i)create\\w*$")) {
            createMessageForStderr(id, ctxid, "could not be created: ", parser);
        } else if (getClass().getName().matches("^.*\\.\\w*(?i)change\\w*$")) {
            createMessageForStderr(id, ctxid, "could not be changed: ", parser);
        } else if (getClass().getName().matches("^.*\\.\\w*(?i)delete\\w*$")) {
            createMessageForStderr(id, ctxid, "could not be deleted: ", parser);
        } else if (getClass().getName().matches("^.*\\.\\w*(?i)list\\w*$")) {
            createMessageForStderr(id, ctxid, "could not be listed: ", parser);
        } else if (getClass().getName().matches("^.*\\.\\w*(?i)get\\w*$")) {
            createMessageForStderr(id, ctxid, "could not be retrieved: ", parser);
        }
    }

    protected void printErrors(final String id, final Integer ctxid, final Exception e, final AdminParser parser) {
        // Remember that all the exceptions in this list must be written in the order with the lowest exception first
        // e.g. if Aexception extends Bexception then Aexception has to be written before Bexception in this list. Otherwise
        // the if clause for Bexception will match beforehand
        if (e instanceof ConnectException) {
            final ConnectException new_name = (ConnectException) e;
            printError(id, ctxid, new_name.getMessage(), parser);
            sysexit(SYSEXIT_COMMUNICATION_ERROR);
        } else if (e instanceof NumberFormatException) {
            printInvalidInputMsg(id, ctxid, "Ids must be numbers!", parser);
            sysexit(1);
        } else if (e instanceof MalformedURLException) {
            final MalformedURLException exc = (MalformedURLException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else if (e instanceof RemoteException) {
            final RemoteException exc = (RemoteException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(SYSEXIT_REMOTE_ERROR);
        } else if (e instanceof NotBoundException) {
            NotBoundException origin = (NotBoundException) e;
            Exception exc = new Exception("Look-up failed. Service \"" + origin.getMessage() + "\" is not available.");
            exc.setStackTrace(origin.getStackTrace());
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else if (e instanceof InvalidCredentialsException) {
            if (parser.getOptionValue(this.adminPassOption) == null && parser.getOptionValue(this.adminUserOption) == null) {
                final String msg = "Options \"" + this.adminUserOption.longForm() + ", " + this.adminPassOption.longForm() + "\" missing";
                printError(id, ctxid, msg, parser);
                parser.printUsage();
                sysexit(SYSEXIT_MISSING_OPTION);
            } else {
                final InvalidCredentialsException exc = (InvalidCredentialsException) e;
                printServerException(id, ctxid, exc, parser);
                sysexit(SYSEXIT_INVALID_CREDENTIALS);
            }
        } else if (e instanceof NoSuchContextException) {
            final NoSuchContextException exc = (NoSuchContextException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(SYSEXIT_NO_SUCH_CONTEXT);
        } else if (e instanceof InvocationTargetException) {
            printError(id, ctxid, e.getMessage(), parser);
            sysexit(1);
        } else if (e instanceof StorageException) {
            final StorageException exc = (StorageException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(SYSEXIT_SERVERSTORAGE_ERROR);
        } else if (e instanceof InvalidDataException) {
            final InvalidDataException exc = (InvalidDataException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(SYSEXIT_INVALID_DATA);
        } else if (e instanceof IllegalArgumentException) {
            printError(id, ctxid, e.getMessage(), parser);
            sysexit(1);
        } else if (e instanceof IllegalAccessException) {
            printError(id, ctxid, e.getMessage(), parser);
            sysexit(1);
        } else if (e instanceof CLIParseException) {
            final CLIParseException exc = (CLIParseException) e;
            printError(id, ctxid, "Failed parsing command line: " + exc.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_ILLEGAL_OPTION_VALUE);
        } else if (e instanceof CLIIllegalOptionValueException) {
            final CLIIllegalOptionValueException exc = (CLIIllegalOptionValueException) e;
            printError(id, ctxid, "Illegal option value : " + exc.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_ILLEGAL_OPTION_VALUE);
        } else if (e instanceof CLIUnknownOptionException) {
            final CLIUnknownOptionException exc = (CLIUnknownOptionException) e;
            printError(id, ctxid, "Unrecognized options on the command line: " + exc.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_UNKNOWN_OPTION);
        } else if (e instanceof MissingOptionException) {
            final MissingOptionException missing = (MissingOptionException) e;
            printError(id, ctxid, missing.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_MISSING_OPTION);
        } else if (e instanceof DatabaseUpdateException) {
            final DatabaseUpdateException exc = (DatabaseUpdateException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else if (e instanceof NoSuchUserException) {
            final NoSuchUserException exc = (NoSuchUserException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(SYSEXIT_NO_SUCH_USER);
        } else if (e instanceof NoSuchGroupException) {
            final NoSuchGroupException exc = (NoSuchGroupException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(SYSEXIT_NO_SUCH_GROUP);
        } else if (e instanceof NoSuchResourceException) {
            printServerException(id, ctxid, e, parser);
            sysexit(SYSEXIT_NO_SUCH_RESOURCE);
        } else if (e instanceof DuplicateExtensionException) {
            final DuplicateExtensionException exc = (DuplicateExtensionException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else if (e instanceof ContextExistsException) {
            final ContextExistsException exc = (ContextExistsException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else if (e instanceof URISyntaxException) {
            final URISyntaxException exc = (URISyntaxException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else if (e instanceof RuntimeException) {
            final RuntimeException exc = (RuntimeException) e;
            printServerException(id, ctxid, exc, parser);
            sysexit(1);
        } else {
            printServerException(id, ctxid, e, parser);
            sysexit(1);
        }
    }
}
