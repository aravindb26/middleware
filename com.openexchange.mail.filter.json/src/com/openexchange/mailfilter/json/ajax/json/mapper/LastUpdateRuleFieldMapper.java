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

package com.openexchange.mailfilter.json.ajax.json.mapper;

import static com.openexchange.java.Autoboxing.L;
import org.apache.jsieve.SieveException;
import org.json.JSONException;
import com.openexchange.exception.OXException;
import com.openexchange.jsieve.commands.Rule;
import com.openexchange.jsieve.commands.RuleComment;
import com.openexchange.mailfilter.json.ajax.json.fields.RuleField;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link LastUpdateRuleFieldMapper} - Maps the update timestamp of a rule
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class LastUpdateRuleFieldMapper implements RuleFieldMapper {

    /**
     * Initialises a new {@link LastUpdateRuleFieldMapper}.
     */
    public LastUpdateRuleFieldMapper() {
        super();
    }

    @Override
    public RuleField getAttributeName() {
        return RuleField.lastModified;
    }

    @Override
    public boolean isNull(Rule rule) {
        return !(rule.getRuleComment() != null && rule.getRuleComment().getUpdateTimestamp() > 0);
    }

    @Override
    public Object getAttribute(Rule rule) throws JSONException, OXException {
        RuleComment ruleComment = rule.getRuleComment();
        return ruleComment == null ? L(-1) : L(ruleComment.getUpdateTimestamp());
    }

    @Override
    public void setAttribute(Rule rule, Object attribute, ServerSession session) throws JSONException, SieveException, OXException {
        // Explicitly set only in WRITE operations, such as CREATE and UPDATE
    }
}
