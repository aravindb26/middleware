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

package com.openexchange.mail.authenticity.test.bugs;

import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.junit.Test;
import com.openexchange.mail.authenticity.MailAuthenticityResultKey;
import com.openexchange.mail.authenticity.MailAuthenticityStatus;
import com.openexchange.mail.authenticity.mechanism.MailAuthenticityMechanismResult;
import com.openexchange.mail.authenticity.mechanism.dkim.DKIMResult;
import com.openexchange.mail.authenticity.mechanism.dmarc.DMARCResult;
import com.openexchange.mail.authenticity.mechanism.spf.SPFResult;
import com.openexchange.mail.authenticity.test.AbstractTestMailAuthenticity;

/**
 * {@link TestMWB1410}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class TestMWB1410 extends AbstractTestMailAuthenticity {

    /**
     * Initialises a new {@link TestMWB1410}.
     */
    public TestMWB1410() {
        super();
    }

    /**
     * MWB1410 Test
     */
    @Test
    public void test() throws AddressException {
        fromAddresses[0] = new InternetAddress("Jane Doe <jane.doe@aliceland.com>");
        perform("ox.io; dkim=fail (\"body hash did not verify\") header.d=foobar.com header.s=minecraft20220222 header.b=blah; arc=reject (\"signature check failed: fail, {[1] = sig:evil.com:reject}\");dmarc=fail reason=\"SPF not aligned (relaxed)\" header.from=linux.com (policy=quarantine);spf=pass (ox.io: domain of project-bounces@linux.org designates 5227:7db7:c36e:f653:1bd7:571a:713b:00ac as permitted sender) smtp.mailfrom=project-bounces@linux.org");
        assertStatus(MailAuthenticityStatus.SUSPICIOUS, result.getStatus());
        assertDomain("linux.org", result.getAttribute(MailAuthenticityResultKey.FROM_DOMAIN, String.class));
        assertAmount(3);
        assertAuthenticityMechanismResult((MailAuthenticityMechanismResult) result.getAttribute(MailAuthenticityResultKey.MAIL_AUTH_MECH_RESULTS, List.class).get(0), "linux.com", DMARCResult.FAIL);
        assertAuthenticityMechanismResult((MailAuthenticityMechanismResult) result.getAttribute(MailAuthenticityResultKey.MAIL_AUTH_MECH_RESULTS, List.class).get(1), "foobar.com", DKIMResult.FAIL);
        assertAuthenticityMechanismResult((MailAuthenticityMechanismResult) result.getAttribute(MailAuthenticityResultKey.MAIL_AUTH_MECH_RESULTS, List.class).get(2), "linux.org", SPFResult.PASS);
    }

}
