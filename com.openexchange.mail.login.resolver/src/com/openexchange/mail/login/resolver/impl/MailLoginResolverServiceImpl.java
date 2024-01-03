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
package com.openexchange.mail.login.resolver.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mail.login.resolver.MailLoginResolver;
import com.openexchange.mail.login.resolver.MailLoginResolverProperties;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.mail.login.resolver.ResolverResult;
import com.openexchange.mail.login.resolver.ResolverStatus;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.session.UserAndContext;

/**
 * {@link MailLoginResolverServiceImpl}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class MailLoginResolverServiceImpl implements MailLoginResolverService {

    private final LeanConfigurationService config;
    private final ServiceListing<MailLoginResolver> mailLoginResolvers;

    /**
     * Initialises a new {@link MailLoginResolverServiceImpl}.
     */
    public MailLoginResolverServiceImpl(LeanConfigurationService config, ServiceListing<MailLoginResolver> mailLoginResolvers) {
        super();
        this.config = config;
        this.mailLoginResolvers = mailLoginResolvers;
    }

    @Override
    public ResolverResult resolveMailLogin(int contextId, String mailLogin) throws OXException {
        for (MailLoginResolver resolver : mailLoginResolvers) {
            ResolverResult result = resolver.resolveMailLogin(contextId, mailLogin);
            if (ResolverStatus.SUCCESS.equals(result.getStatus())) {
                return result;
            }
        }
        return ResolverResult.FAILURE();
    }

    @Override
    public ResolverResult resolveEntity(int contextId, UserAndContext entity) throws OXException {
        for (MailLoginResolver resolver : mailLoginResolvers) {
            ResolverResult result = resolver.resolveEntity(contextId, entity);
            if (ResolverStatus.SUCCESS.equals(result.getStatus())) {
                return result;
            }
        }
        return ResolverResult.FAILURE();
    }

    @Override
    public List<ResolverResult> resolveMultipleMailLogins(int contextId, List<String> mailLogins) throws OXException {
        if (mailLogins == null || mailLogins.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> mailLogin2Index= new HashMap<>(mailLogins.size());
        for (int i = 0; i < mailLogins.size(); i++) {
            mailLogin2Index.put(mailLogins.get(i), Integer.valueOf(i));
        }
        Map<Integer, ResolverResult> index2ResolvedResult = new HashMap<>(mailLogins.size());
        for (MailLoginResolver resolver : mailLoginResolvers) {
            List<String> mailLogins2Resolve = new ArrayList<String>(mailLogin2Index.keySet());
            List<ResolverResult> resolverResults = resolver.resolveMultipleMailLogins(contextId, mailLogins2Resolve);
            for (int i = 0; i < resolverResults.size(); i++) {
                ResolverResult result = resolverResults.get(i);
                if (result != null) {
                    ResolverStatus status = result.getStatus();
                    if (ResolverStatus.SUCCESS.equals(status)) {
                        index2ResolvedResult.put(mailLogin2Index.remove(mailLogins2Resolve.get(i)), result);
                    }
                }
            }
            if (mailLogin2Index.isEmpty()) {
                break;
            }
        }
        return new ArrayList<>(index2ResolvedResult.values());
    }

    @Override
    public List<ResolverResult> resolveMultipleEntities(int contextId, List<UserAndContext> entities) throws OXException {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        Map<UserAndContext, Integer> entities2Index= new HashMap<>(entities.size());
        for (int i = 0; i < entities.size(); i++) {
            entities2Index.put(entities.get(i), Integer.valueOf(i));
        }
        Map<Integer, ResolverResult> index2ResolvedResult = new HashMap<>(entities.size());
        for (MailLoginResolver resolver : mailLoginResolvers) {
            List<UserAndContext> entities2Resolve = new ArrayList<UserAndContext>(entities2Index.keySet());
            List<ResolverResult> resolverResults = resolver.resolveMultipleEntities(contextId, entities2Resolve);
            for (int i = 0; i < resolverResults.size(); i++) {
                ResolverResult result = resolverResults.get(i);
                if (result != null) {
                    ResolverStatus status = result.getStatus();
                    if (ResolverStatus.SUCCESS.equals(status)) {
                        index2ResolvedResult.put(entities2Index.remove(entities2Resolve.get(i)), result);
                    }
                }
            }
            if (entities2Index.isEmpty()) {
                break;
            }
        }
        return new ArrayList<ResolverResult>(index2ResolvedResult.values());
    }

    @Override
    public boolean isEnabled(int contextId) {
        return config.getBooleanProperty(-1, contextId, MailLoginResolverProperties.ENABLED);
    }

}