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
package com.openexchange.mail.login.resolver;

/**
 * {@link ResolverResult}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class ResolverResult {

    /**
     * Creates a new {@link ResolverResult} instance indicating {@link ResolverStatus#FAILURE} result.
     *
     * @return A new {@link ResolverResult} instance indicating {@link ResolverStatus#FAILURE} result
     */
    public static ResolverResult FAILURE() {
        return new ResolverResult(-1, -1, null, ResolverStatus.FAILURE);
    }

    /**
     * Creates a new {@link ResolverResult} instance indicating {@link ResolverStatus#SUCCESS} result.
     *
     * @param userID The user identifier
     * @param contextID The context identifier
     * @param mailLogin The user's mail login
     * @return A new {@link ResolverResult} instance indicating {@link ResolverStatus#SUCCESS} result
     */
    public static ResolverResult SUCCESS(int userId, int contextId, String mailLogin) {
        return new ResolverResult(userId, contextId, mailLogin, ResolverStatus.SUCCESS);
    }

    // ----------------------------------------------------------------------------------------------------------------------------

    private ResolverStatus status;
    private int userId;
    private int contextId;
    private String mailLogin;

    /**
     * Initialises a new {@link ResolverResult}.
     */
    public ResolverResult(int userId, int contextId, String mailLogin, ResolverStatus status) {
        this.status = status;
        this.userId = userId;
        this.contextId = contextId;
        this.mailLogin = mailLogin;
    }

    /**
     * Gets the mail login
     *
     * @return The mail login or <code>null</code> if unknown
     *         (typically alongside with resolve type set to {@link ResolverStatus#FAILURE}
     */
    public String getMailLogin() {
        return mailLogin;
    }

    /**
     * Gets the user identifier
     *
     * @return The user identifier or <code>-1</code> if unknown
     *         (typically alongside with resolve status set to {@link ResolverStatus#FAILURE}
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the context identifier
     *
     * @return The context identifier or <code>-1</code> if unknown
     *         (typically alongside with resolve status set to {@link ResolverStatus#FAILURE}
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the resolver status
     * <ul>
     * <li>FAILURE - The {@code MailLoginResolver} cannot resolve the mail login or contextId/userId entity, therefore delegates to the next one in chain.
     * <li>SUCCESS - The {@code MailLoginResolver} successfully resolved mail login or contextId/userId entity.
     * </ul>
     *
     * @return The status
     */
    public ResolverStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ResolverResult [status=" + status + ", mailLogin=" + mailLogin + ", userId=" + userId + ", contextId=" + contextId + "]";
    }

}