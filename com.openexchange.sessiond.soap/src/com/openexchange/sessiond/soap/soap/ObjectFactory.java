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

package com.openexchange.sessiond.soap.soap;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.openexchange.sessiond.soap.soap package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.openexchange.sessiond.soap.soap
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link InvalidDataException }
     *
     */
    public InvalidDataException createInvalidDataException() {
        return new InvalidDataException();
    }

    /**
     * Create an instance of {@link NoSuchContextException }
     *
     */
    public NoSuchContextException createNoSuchContextException() {
        return new NoSuchContextException();
    }

    /**
     * Create an instance of {@link ClearUserSessions }
     *
     */
    public ClearUserSessions createClearUserSessions() {
        return new ClearUserSessions();
    }

    /**
     * Create an instance of {@link NoSuchUserException }
     *
     */
    public NoSuchUserException createNoSuchUserException() {
        return new NoSuchUserException();
    }

    /**
     * Create an instance of {@link RemoteException }
     *
     */
    public RemoteException createRemoteException() {
        return new RemoteException();
    }

    /**
     * Create an instance of {@link InvalidCredentialsException }
     *
     */
    public InvalidCredentialsException createInvalidCredentialsException() {
        return new InvalidCredentialsException();
    }

    /**
     * Create an instance of {@link Exception }
     *
     */
    public Exception createException() {
        return new Exception();
    }

}
