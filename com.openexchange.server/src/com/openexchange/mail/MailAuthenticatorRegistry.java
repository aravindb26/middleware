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

package com.openexchange.mail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableList;
import com.openexchange.exception.OXException;
import com.openexchange.mailaccount.Account;
import com.openexchange.session.Session;

/**
 * {@link MailAuthenticatorRegistry} - The registry for mail authenticators.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class MailAuthenticatorRegistry {

    private static final MailAuthenticatorRegistry INSTANCE = new MailAuthenticatorRegistry();

    /**
     * Gets the registry instance.
     *
     * @return The registry instance
     */
    public static MailAuthenticatorRegistry getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final AtomicReference<List<MailAuthenticator>> authenticatorsReference;
    private final Comparator<MailAuthenticator> comparator;

    /**
     * Initializes a new {@link MailAuthenticatorRegistry}.
     */
    private MailAuthenticatorRegistry() {
        super();
        authenticatorsReference = new AtomicReference<List<MailAuthenticator>>(null);
        comparator = new Comparator<MailAuthenticator>() {

            @Override
            public int compare(MailAuthenticator authenticator1, MailAuthenticator authenticator2) {
                int rank1 = authenticator1.getRanking();
                int rank2 = authenticator2.getRanking();
                return (rank1 < rank2) ? 1 : ((rank1 == rank2) ? 0 : -1);
            }
        };
    }

    /**
     * Gets the rank-wise sorted (immutable) list of mail authenticators.
     *
     * @return The mail authenticators.
     */
    public List<MailAuthenticator> getMailAuthenticators() {
        List<MailAuthenticator> authenticators = authenticatorsReference.get();
        return authenticators == null ? Collections.emptyList() : authenticators;
    }

    /**
     * Determines the best-fitting mail authenticator (if any) for given arguments.
     *
     * @param session The session
     * @param account The primary/secondary account
     * @param forMailAccess <code>true</code> if credentials are supposed to be set for mail access; otherwise <code>false</code> for mail transport
     * @return The best-fitting mail authenticator or empty
     * @throws OXException If best-fitting mail authenticator cannot be determined
     */
    public Optional<MailAuthenticator> getMailAuthenticator(Session session, Account account, boolean forMailAccess) throws OXException {
        List<MailAuthenticator> authenticators = authenticatorsReference.get();
        if (authenticators == null) {
            return Optional.empty();
        }

        MailAuthenticator authenticator = null;
        for (Iterator<MailAuthenticator> it = authenticators.iterator(); authenticator == null && it.hasNext();) {
            MailAuthenticator candidate = it.next();
            if (candidate.accept(session, account, forMailAccess)) {
                // Since rank-wise sorted we can assign first match and abort iteration
                authenticator = candidate;
            }
        }
        return Optional.ofNullable(authenticator);
    }

    // --------------------------------------------- Add/remove mail authenticators --------------------------------------------------------

    /**
     * Adds given mail authenticator to this registry.
     *
     * @param authenticator The mail authenticator to add
     */
    public void addMailAuthenticator(MailAuthenticator authenticator) {
        List<MailAuthenticator> current;
        List<MailAuthenticator> newOnes;
        do {
            current = authenticatorsReference.get();
            if (current == null || current.isEmpty()) {
                newOnes = new ArrayList<MailAuthenticator>(1);
                newOnes.add(authenticator);
            } else {
                newOnes = new ArrayList<MailAuthenticator>(current);
                newOnes.add(authenticator);
                Collections.sort(newOnes, comparator);
            }
        } while (authenticatorsReference.compareAndSet(current, ImmutableList.copyOf(newOnes)) == false);
    }

    /**
     * Removes given mail authenticator from this registry.
     *
     * @param authenticator The mail authenticator to remove
     */
    public void removeMailAuthenticator(MailAuthenticator authenticator) {
        List<MailAuthenticator> current;
        List<MailAuthenticator> newOnes;
        do {
            current = authenticatorsReference.get();
            if (current.isEmpty()) {
                newOnes = null;
            } else {
                newOnes = new ArrayList<MailAuthenticator>(current);
                if (newOnes.remove(authenticator) == false) {
                    // No such authenticator
                    return;
                }
                if (newOnes.isEmpty()) {
                    newOnes = null;
                }
            }
        } while (authenticatorsReference.compareAndSet(current, newOnes == null ? null : ImmutableList.copyOf(newOnes)) == false);
    }

}
