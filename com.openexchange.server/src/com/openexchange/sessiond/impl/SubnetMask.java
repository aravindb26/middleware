
package com.openexchange.sessiond.impl;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.net.util.SubnetUtils;
import com.openexchange.java.Strings;
import com.openexchange.net.IPAddressUtil;

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

/**
 * {@link SubnetMask}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class SubnetMask {

    private static final Pattern ipv4Dotted = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    private static final Pattern ipv4CIDR = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,2})?(/\\d{1,2})");

    private static final Pattern ipv6CIDR = Pattern.compile("/(\\d{1,3})");

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String v4Mask;
    private final BigInteger v6Mask;
    private final boolean v4CIDR;

    /**
     * Initializes a new {@link SubnetMask}.
     *
     * @param v4 The IPv4 sub-mask representation or <code>null</code>
     * @param v6 The IPv6 sub-mask representation or <code>null</code>
     */
    public SubnetMask(String v4, String v6) {
        super();
        if (Strings.isNotEmpty(v4)) {
            String toParse = v4.trim();
            Matcher m;
            if ((m = ipv4Dotted.matcher(toParse)).matches()) {
                this.v4Mask = toParse;
                v4CIDR = false;
            } else if ((m = ipv4CIDR.matcher(toParse)).matches()) {
                this.v4Mask = m.group(2);
                v4CIDR = true;
            } else {
                throw new IllegalArgumentException(toParse + " is neither a valid CIDR nor a valid dotted representation of an IPv4 subnet mask.");
            }
        } else {
            v4Mask = null;
            v4CIDR = false;
        }

        if (Strings.isNotEmpty(v6)) {
            String toParse = v6.trim();
            if (ipv6CIDR.matcher(toParse).matches()) {
                v6Mask = BigInteger.valueOf(2).pow(128).subtract(BigInteger.valueOf(2).pow(128 - Integer.parseInt(toParse.substring(1))));
            } else {
                throw new IllegalArgumentException(toParse + " is not a valid CIDR representation of an IPv6 subnet mask.");
            }
        } else {
            v6Mask = null;
        }
    }

    /**
     * Checks if given IP addresses are in the same sub-net.
     *
     * @param firstIP The first IP representation
     * @param secondIP The second IP representation
     * @return <code>true</code> if given IP addresses are in the same sub-net; otherwise <code>false</code>
     */
    public boolean areInSameSubnet(String firstIP, String secondIP) {
        if (firstIP == null || secondIP == null) {
            return false;
        }

        if (v4Mask != null) {
            if (ipv4Dotted.matcher(firstIP).matches() && ipv4Dotted.matcher(secondIP).matches()) {
                SubnetUtils subnet = v4CIDR ? new SubnetUtils(firstIP + v4Mask) : new SubnetUtils(firstIP, v4Mask);
                subnet.setInclusiveHostCount(true);
                return subnet.getInfo().isInRange(secondIP);
            }
        }

        if (v6Mask != null) {
            byte[] firstV6Octets = IPAddressUtil.textToNumericFormatV6(firstIP);
            if (firstV6Octets == null) {
                return false;
            }
            byte[] secondV6Octets = IPAddressUtil.textToNumericFormatV6(secondIP);
            if (secondV6Octets == null) {
                return false;
            }
            BigInteger firstV6 = ipToBigIntegerV6(firstV6Octets);
            BigInteger secondV6 = ipToBigIntegerV6(secondV6Octets);

            return firstV6.and(v6Mask).equals(secondV6.and(v6Mask));
        }
        return false;
    }

    private static BigInteger ipToBigIntegerV6(final byte[] octets) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < octets.length; i++) {
            result = result.or(BigInteger.valueOf(octets[i]).and(BigInteger.valueOf(0xff)));
            if (i < octets.length - 1) {
                result = result.shiftLeft(8);
            }
        }
        return result;
    }

}
