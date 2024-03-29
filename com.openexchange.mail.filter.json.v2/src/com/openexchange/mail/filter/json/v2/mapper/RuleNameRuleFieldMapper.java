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

package com.openexchange.mail.filter.json.v2.mapper;

import org.apache.jsieve.SieveException;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.jsieve.commands.Rule;
import com.openexchange.jsieve.commands.RuleComment;
import com.openexchange.mail.filter.json.v2.json.fields.RuleField;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link RuleNameRuleFieldMapper}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class RuleNameRuleFieldMapper implements RuleFieldMapper {

    /**
     * Initializes a new {@link RuleNameRuleFieldMapper}.
     */
    public RuleNameRuleFieldMapper() {
        super();
    }

    @Override
    public RuleField getAttributeName() {
        return RuleField.rulename;
    }

    @Override
    public boolean isNull(Rule rule) {
        RuleComment ruleComment = rule.getRuleComment();
        return (ruleComment == null) || (rule.getRuleComment().getRulename() == null);
    }

    @Override
    public Object getAttribute(Rule rule) throws JSONException, OXException {
        if (isNull(rule)) {
            return JSONObject.NULL;
        }

        RuleComment ruleComment = rule.getRuleComment();
        return ruleComment.getRulename();
    }

    @Override
    public void setAttribute(Rule rule, Object attribute, ServerSession session) throws JSONException, SieveException, OXException {
        RuleComment ruleComment = rule.getRuleComment();
        String newName = Strings.sanitizeString((String) attribute);
        if (null != ruleComment) {
            ruleComment.setRulename(newName);
        } else {
            rule.setRuleComments(new RuleComment(newName));
        }
    }
}
