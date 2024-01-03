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
package com.openexchange.crypto;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadRenamer;

/**
 * {@link AccountAwareRecryptSecretsTask} - Performs a re-encryption with modern algorithms of secrets still using legacy encryption mechanisms
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public abstract class AccountAwareRecryptSecretsTask implements Task<Void> {

    protected final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AccountAwareRecryptSecretsTask.class);

    protected final String type;
    protected final int accountId;
    protected final int userId;
    protected final int contextId;

    public AccountAwareRecryptSecretsTask(String accountType, int accountId, int userId, int contextId) {
        super();
        this.type = accountType;
        this.accountId = accountId;
        this.userId = userId;
        this.contextId = contextId;
    }

    @Override
    public void setThreadName(ThreadRenamer threadRenamer) {
        // nothing to do
    }

    @Override
    public void beforeExecute(Thread t) {
        // nothing to do
    }

    @Override
    public void afterExecute(Throwable t) {
        LOG.debug("Unable to recrypt passwords for {} account {}, user {} in context {}.", type, I(accountId), I(userId), I(contextId), t);
    }

    @Override
    public Void call() throws Exception {
        LOG.info("Legacy encryption detected for {} account {}, user {} in context {}. Try to recrypt.", type, I(accountId), I(userId), I(contextId));
        recrypt();
        LOG.debug("Successfully recrypted passwords for {} account {}, user {} in context {}.", type, I(accountId), I(userId), I(contextId));
        return null;
    }

    /**
     * Re-encrypt secrets with modern cryptographic mechanisms
     *
     * @throws Exception If an error occurs during re-encryption
     */
    public abstract void recrypt() throws Exception;

}
