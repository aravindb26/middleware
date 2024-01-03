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

package com.openexchange.pgp.core.packethandling;

import org.bouncycastle.bcpg.Packet;

/**
 * {@link PGPPacket} represents a wrapper around a BouncyCastle PGP Message Packet
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.8.4
 */
public class PGPPacket {

    private final Packet bcPacket;
    private final byte[] firstData;

    /**
     * Initializes a new {@link PGPPacket}.
     *
     * @param bcPacket The Bouncy Castle {@link org.bouncycastle.bcpg.Packet.Packet} to wrap
     * @param firstData The "first" data of the packet, containing at least the header data, but might be more
     */
    public PGPPacket(Packet bcPacket, byte[] firstData) {
        this.bcPacket = bcPacket;
        this.firstData = firstData;
    }

    /**
     * Gets the first data of the package.
     * <br> <br>
     * The "first" data consist at least of the packet header; more additional data can be followed after the header
     *
     * @return The first data of the package
     */
    public byte[] getBcFirstData() {
        return firstData;
    }

    /**
     * Gets the packet
     *
     * @return The packet
     */
    public Packet getBcPacket() {
        return bcPacket;
    }
}
