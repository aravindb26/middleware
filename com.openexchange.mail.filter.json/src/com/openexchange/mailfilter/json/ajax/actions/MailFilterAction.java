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

package com.openexchange.mailfilter.json.ajax.actions;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.jsieve.SieveException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import org.json.JSONValue;
import com.google.common.collect.ImmutableSet;
import com.openexchange.exception.OXException;
import com.openexchange.jsieve.commands.ActionCommand;
import com.openexchange.jsieve.commands.JSONMatchType;
import com.openexchange.jsieve.commands.Rule;
import com.openexchange.jsieve.commands.test.ITestCommand;
import com.openexchange.jsieve.registry.TestCommandRegistry;
import com.openexchange.mailfilter.Credentials;
import com.openexchange.mailfilter.MailFilterService;
import com.openexchange.mailfilter.MailFilterService.FilterType;
import com.openexchange.mailfilter.exceptions.MailFilterExceptionCode;
import com.openexchange.mailfilter.json.ajax.Parameter;
import com.openexchange.mailfilter.json.ajax.actions.AbstractRequest.Parameters;
import com.openexchange.mailfilter.json.ajax.json.RuleParser;
import com.openexchange.mailfilter.json.ajax.servlet.MailFilterExtensionCapabilities;
import com.openexchange.mailfilter.json.osgi.Services;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * @author <a href="mailto:dennis.sieben@open-xchange.org">Dennis Sieben</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class MailFilterAction extends AbstractAction<Rule, MailFilterRequest> {

    private static final MailFilterAction INSTANCE = new MailFilterAction();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static MailFilterAction getInstance() {
        return INSTANCE;
    }

    private static final RuleParser CONVERTER = new RuleParser();

    // -------------------------------------------------------------------------------------------------------------------------------- //

    /**
     * Default constructor.
     */
    public MailFilterAction() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONObject actionConfig(MailFilterRequest request) throws OXException {
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        Set<String> capabilities = mailFilterService.getCapabilities(credentials);
        try {
            JSONObject result = getTestAndActionObjects(capabilities);
            addSupportedCapabilities(capabilities, result);
            return result;
        } catch (JSONException e) {
            throw MailFilterExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Adds all supported capabilities to the {@link JSONObject}
     *
     * @param capabilities All sieve capabilities
     * @param jsonObject The json object
     * @throws JSONException
     */
    private void addSupportedCapabilities(Set<String> capabilities, JSONObject jsonObject) throws JSONException {
        JSONArray caps = new JSONArray();
        for (MailFilterExtensionCapabilities cap : MailFilterExtensionCapabilities.values()) {
            if (capabilities.contains(cap.name())) {
                caps.put(cap.name());
            }
        }
        jsonObject.putOpt("capabilities", caps);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void actionDelete(MailFilterRequest request) throws OXException {
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        try {
            JSONObject json = getJsonBody(request);
            int uid = getUniqueId(json).intValue();
            mailFilterService.deleteFilterRule(credentials, uid);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONArray actionList(MailFilterRequest request) throws OXException {
        Parameters parameters = request.getParameters();
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        String flag = parameters.getParameter(Parameter.FLAG);
        FilterType filterType;
        if (flag != null) {
            try {
                filterType = FilterType.valueOf(flag);
            } catch (IllegalArgumentException e) {
                throw MailFilterExceptionCode.INVALID_FILTER_TYPE_FLAG.create(flag);
            }
        } else {
            filterType = FilterType.all;
        }
        List<Rule> rules = mailFilterService.listRules(credentials, filterType);
        try {
            return CONVERTER.write(rules.toArray(new Rule[rules.size()]));
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int actionNew(MailFilterRequest request) throws OXException {
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        try {
            Credentials credentials = request.getCredentials();
            Rule rule = CONVERTER.parse(getJsonBody(request), ServerSessionAdapter.valueOf(request.getSession()));
            setMetadata(rule, request);
            return mailFilterService.createFilterRule(credentials, rule);
        } catch (SieveException e) {
            throw MailFilterExceptionCode.handleSieveException(e);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Sets the rule's metadata, i.e. the update timestamp and the source of update (client ip)
     *
     * @param rule The rule
     * @param request The request
     */
    private void setMetadata(Rule rule, MailFilterRequest request) {
        rule.getRuleComment().setUpdateTimestamp(System.currentTimeMillis());
        rule.getRuleComment().setSourceOfUpdate(request.getSession().getLocalIp());
    }

    private JSONObject getJsonBody(MailFilterRequest request) throws JSONException, OXException {
        JSONObject jsonObject = JSONServices.parseObject(request.getBody());
        checkJsonValue(jsonObject, null, jsonObject);
        return jsonObject;
    }

    private static Set<String> MUST_NOT_BE_EMPTY = ImmutableSet.of("values");

    private void checkJsonValue(JSONValue jValue, String name, JSONValue parent) throws OXException {
        if (null != jValue) {
            if (jValue.isArray()) {
                JSONArray jArray = jValue.toArray();
                int length = jArray.length();
                if (0 == length) {
                    if (null != name && MUST_NOT_BE_EMPTY.contains(name)) {
                        // SIEVE does not support empty arrays
                        throw MailFilterExceptionCode.INVALID_SIEVE_RULE.create(parent.toString());
                    }
                }
                for (int i = 0; i < length; i++) {
                    Object object = jArray.opt(i);
                    if (object instanceof JSONValue) {
                        checkJsonValue((JSONValue) object, null, parent);
                    }
                }
            } else if (jValue.isObject()) {
                JSONObject jObject = jValue.toObject();
                for (Entry<String, Object> entry : jObject.entrySet()) {
                    Object object = entry.getValue();
                    if (object instanceof JSONValue) {
                        checkJsonValue((JSONValue) object, entry.getKey(), parent);
                    }
                }
            }
        }
    }

    @Override
    protected void actionReorder(MailFilterRequest request) throws OXException {
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);

        try {
            String body = request.getBody();
            JSONArray json = JSONServices.parseArray(body);
            int[] uids = new int[json.length()];
            for (int i = 0; i < json.length(); i++) {
                uids[i] = json.getInt(i);
            }
            mailFilterService.reorderRules(credentials, uids);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void actionUpdate(MailFilterRequest request) throws OXException {
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        try {
            JSONObject json = getJsonBody(request);
            int uid = getUniqueId(json).intValue();
            Rule rule = mailFilterService.getFilterRule(credentials, uid);
            if (rule == null) {
                throw MailFilterExceptionCode.NO_SUCH_ID.create(I(uid), credentials.getRightUsername(), credentials.getContextString());
            }
            CONVERTER.parse(rule, json, ServerSessionAdapter.valueOf(request.getSession()));
            setMetadata(rule, request);
            mailFilterService.updateFilterRule(credentials, rule, uid);
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e);
        } catch (SieveException e) {
            throw MailFilterExceptionCode.handleSieveException(e);
        }
    }

    @Override
    protected void actionDeleteScript(MailFilterRequest request) throws OXException {
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        mailFilterService.purgeFilters(credentials);
    }

    @Override
    protected String actionGetScript(MailFilterRequest request) throws OXException {
        Credentials credentials = request.getCredentials();
        MailFilterService mailFilterService = Services.getService(MailFilterService.class);
        return mailFilterService.getActiveScript(credentials);
    }

    private JSONArray getActionArray(Set<String> capabilities) {
        JSONArray actionarray = new JSONArray();
        for (ActionCommand.Commands command : ActionCommand.Commands.values()) {
            List<String> required = command.getRequired();
            if (required.isEmpty()) {
                actionarray.put(command.getJsonName());
            } else {
                for (String req : required) {
                    if (capabilities.contains(req)) {
                        actionarray.put(command.getJsonName());
                        break;
                    }
                }
            }
        }
        return actionarray;
    }

    /**
     * Fills up the config object
     *
     * @param hashSet A set of sieve capabilities
     * @return
     * @throws JSONException
     */
    private JSONObject getTestAndActionObjects(Set<String> capabilities) throws JSONException {
        JSONObject retval = new JSONObject();
        retval.put("tests", getTestArray(capabilities));
        retval.put("actioncommands", getActionArray(capabilities));
        return retval;
    }

    private JSONArray getTestArray(Set<String> capabilities) throws JSONException {
        TestCommandRegistry testCommandRegistry = Services.getService(TestCommandRegistry.class);
        Collection<ITestCommand> commands = testCommandRegistry.getCommands();
        JSONArray jTestArray = new JSONArray(commands.size());
        for (ITestCommand command : commands) {
            if (isSupported(command, capabilities)) {
                JSONObject jTestObject = new JSONObject(4);
                jTestObject.put("test", command.getCommandName());
                List<JSONMatchType> jsonMatchTypes = command.getJsonMatchTypes();
                JSONArray jComparisons;
                if (null != jsonMatchTypes) {
                    jComparisons = new JSONArray(jsonMatchTypes.size());
                    for (JSONMatchType matchtype : jsonMatchTypes) {
                        String value = matchtype.getRequired();
                        if (matchtype.getVersionRequirement() <= 1 && ("".equals(value) || capabilities.contains(value))) {
                            jComparisons.put(matchtype.getJsonName());
                        }
                    }
                } else {
                    jComparisons = JSONArray.EMPTY_ARRAY;
                }
                jTestObject.put("comparison", jComparisons);
                jTestArray.put(jTestObject);
            }
        }
        return jTestArray;
    }

    private boolean isSupported(ITestCommand command, Set<String> capabilities) {
        List<String> requiredCapabilities = command.getRequired();
        if (null == requiredCapabilities || requiredCapabilities.isEmpty()) {
            return true;
        }

        for (String requiredCapability : requiredCapabilities) {
            if (false == capabilities.contains(requiredCapability)) {
                return false;
            }
        }
        return true;
    }

    private Integer getUniqueId(JSONObject json) throws OXException {
        if (json.hasAndNotNull("id")) {
            try {
                return Integer.valueOf(json.getInt("id"));
            } catch (JSONException e) {
                throw MailFilterExceptionCode.ID_MISSING.create();
            }
        }
        throw MailFilterExceptionCode.MISSING_PARAMETER.create("id");
    }

}
