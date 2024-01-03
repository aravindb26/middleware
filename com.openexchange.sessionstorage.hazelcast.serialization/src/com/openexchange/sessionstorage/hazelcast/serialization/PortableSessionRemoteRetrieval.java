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

package com.openexchange.sessionstorage.hazelcast.serialization;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.openexchange.hazelcast.serialization.AbstractCustomPortable;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link PortableSessionRemoteRetrieval}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class PortableSessionRemoteRetrieval extends AbstractCustomPortable implements Callable<PortableSession> {

    private static final AtomicReference<SessiondService> SERVICE_REFERENCE = new AtomicReference<SessiondService>();

    /**
     * Sets the service reference
     *
     * @param service The service reference or <code>null</code>
     */
    public static void setSessiondServiceReference(SessiondService service) {
        SERVICE_REFERENCE.set(service);
    }

    private static final AtomicReference<ObfuscatorService> OBFUSCATOR_REFERENCE = new AtomicReference<ObfuscatorService>();

    /**
     * Sets the service reference
     *
     * @param service The service reference or <code>null</code>
     */
    public static void setObfuscatorServiceReference(ObfuscatorService service) {
        OBFUSCATOR_REFERENCE.set(service);
    }

    // ---------------------------------------------------------------------------------------------------------------------

    /** The unique portable class ID of the {@link PortableSessionRemoteRetrieval} */
    public static final int CLASS_ID = 404;

    private static final String FIELD_ID = "id";

    private String id;

    /**
     * Initializes a new {@link PortableSessionRemoteRetrieval}.
     */
    public PortableSessionRemoteRetrieval() {
        super();
    }

    /**
     * Initializes a new {@link PortableSessionRemoteRetrieval}.
     *
     * @param id The session identifier
     */
    public PortableSessionRemoteRetrieval(String id) {
        super();
        this.id = id;
    }

    @Override
    public PortableSession call() throws Exception {
        SessiondService service = SERVICE_REFERENCE.get();
        if (null == service) {
            return null;
        }

        Session ses = service.getSession(id, false);
        if (null == ses) {
            return null;
        }

        PortableSession portableSession = new PortableSession(ses);
        portableSession.setPassword(OBFUSCATOR_REFERENCE.get().obfuscate(portableSession.getPassword()));
        return portableSession;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeUTF(FIELD_ID, id);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        this.id = reader.readUTF(FIELD_ID);
    }

}
