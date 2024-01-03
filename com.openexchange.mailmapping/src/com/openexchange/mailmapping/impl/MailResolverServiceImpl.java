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

package com.openexchange.mailmapping.impl;

import java.util.HashMap;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.mailmapping.MailResolver;
import com.openexchange.mailmapping.MailResolverService;
import com.openexchange.mailmapping.MultipleMailResolver;
import com.openexchange.mailmapping.ResolveReply;
import com.openexchange.mailmapping.ResolvedMail;
import com.openexchange.osgi.ServiceListing;


/**
 * The {@link MailResolverServiceImpl} is a utility class for consulting mail mapping services
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> Added constructor
 */
public class MailResolverServiceImpl implements MailResolverService {

    private final ServiceListing<MailResolver> mailResolvers;

    /**
     * Initializes a new {@link MailResolverServiceImpl}.
     */
    public MailResolverServiceImpl(ServiceListing<MailResolver> mailResolvers) {
        super();
        this.mailResolvers = mailResolvers;
    }

    @Override
    public ResolvedMail resolve(String mail) throws OXException {
        for (MailResolver resolver : mailResolvers) {
            ResolvedMail resolved = resolver.resolve(mail);
            if (resolved != null) {
                ResolveReply reply = resolved.getResolveReply();
                if (ResolveReply.ACCEPT.equals(reply)) {
                    // Return resolved instance
                    return resolved;
                }
                if (ResolveReply.DENY.equals(reply)) {
                    // No further processing allowed
                    return null;
                }
                // Otherwise NEUTRAL reply; next in chain
            }
        }
        return null;
    }

    @Override
    public ResolvedMail[] resolveMultiple(String... mails) throws OXException {
        if (null == mails || mails.length == 0) {
            return new ResolvedMail[0];
        }

        Map<String, Integer> mail2Index= new HashMap<>(mails.length);
        for (int i = 0; i < mails.length; i++) {
            mail2Index.put(mails[i], Integer.valueOf(i));
        }
        Map<Integer, ResolvedMail> index2ResolvedMail = new HashMap<>(mails.length);
        for (MailResolver resolver : mailResolvers) {
            String[] mails2Resolve = mail2Index.keySet().toArray(new String[mail2Index.size()]);
            if (resolver instanceof MultipleMailResolver) {
                // Pass complete mail array to current multiple-capable resolver
                ResolvedMail[] resolvedMails = ((MultipleMailResolver) resolver).resolveMultiple(mails2Resolve);
                for (int i = 0; i < resolvedMails.length; i++) {
                    ResolvedMail resolved = resolvedMails[i];
                    if (resolved != null) {
                        ResolveReply reply = resolved.getResolveReply();
                        if (ResolveReply.ACCEPT.equals(reply)) {
                            // Put resolved instance
                            index2ResolvedMail.put(mail2Index.remove(mails2Resolve[i]), resolved);
                        }
                        if (ResolveReply.DENY.equals(reply)) {
                            // No further processing allowed
                            index2ResolvedMail.put(mail2Index.remove(mails2Resolve[i]), null);
                        }
                        // Otherwise NEUTRAL reply; next in chain
                    }
                }
            } else {
                // Need to iterate mails to pass them one-by-one to current resolver
                for (int i = 0; i < mails2Resolve.length; i++) {
                    ResolvedMail resolved = resolver.resolve(mails2Resolve[i]);
                    if (resolved != null) {
                        ResolveReply reply = resolved.getResolveReply();
                        if (ResolveReply.ACCEPT.equals(reply)) {
                            index2ResolvedMail.put(mail2Index.remove(mails2Resolve[i]), resolved);
                        }
                        if (ResolveReply.DENY.equals(reply)) {
                            index2ResolvedMail.put(mail2Index.remove(mails2Resolve[i]), null);
                        }
                        // Otherwise NEUTRAL reply; next in chain
                    }
                }
            }
            if (mail2Index.isEmpty()) {
                break;
            }
        }

        ResolvedMail[] resolvedMails = new ResolvedMail[mails.length];
        for (int i = resolvedMails.length; i-- > 0;) {
            resolvedMails[i] = index2ResolvedMail.get(Integer.valueOf(i));
        }
        return resolvedMails;
    }

}
