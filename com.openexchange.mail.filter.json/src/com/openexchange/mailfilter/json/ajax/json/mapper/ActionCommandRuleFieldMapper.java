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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jsieve.SieveException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.jsieve.commands.ActionCommand;
import com.openexchange.jsieve.commands.ActionCommand.Commands;
import com.openexchange.jsieve.commands.IfCommand;
import com.openexchange.jsieve.commands.Rule;
import com.openexchange.mailfilter.json.ajax.json.fields.GeneralField;
import com.openexchange.mailfilter.json.ajax.json.fields.RuleField;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.CommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.AddFlagActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.DiscardActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.EnotifyActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.FileIntoActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.KeepActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.PGPEncryptActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.RedirectActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.RejectActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.RemoveFlagActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.SetFlagActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.StopActionCommandParser;
import com.openexchange.mailfilter.json.ajax.json.mapper.parser.action.VacationActionCommandParser;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ActionCommandRuleFieldMapper}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ActionCommandRuleFieldMapper implements RuleFieldMapper {

    private final Map<String, CommandParser<ActionCommand>> parsers;

    /**
     * Initialises a new {@link ActionCommandRuleFieldMapper}.
     */
    public ActionCommandRuleFieldMapper() {
        super();

        Map<String, CommandParser<ActionCommand>> p = new HashMap<>();
        p.put(Commands.KEEP.getJsonName(), new KeepActionCommandParser());
        p.put(Commands.DISCARD.getJsonName(), new DiscardActionCommandParser());
        p.put(Commands.REDIRECT.getJsonName(), new RedirectActionCommandParser());
        p.put(Commands.REJECT.getJsonName(), new RejectActionCommandParser());
        p.put(Commands.FILEINTO.getJsonName(), new FileIntoActionCommandParser());
        p.put(Commands.STOP.getJsonName(), new StopActionCommandParser());
        p.put(Commands.VACATION.getJsonName(), new VacationActionCommandParser());
        p.put(Commands.ENOTIFY.getJsonName(), new EnotifyActionCommandParser());
        p.put(Commands.SETFLAG.getJsonName(), new SetFlagActionCommandParser());
        p.put(Commands.ADDFLAG.getJsonName(), new AddFlagActionCommandParser());
        p.put(Commands.REMOVEFLAG.getJsonName(), new RemoveFlagActionCommandParser());
        p.put(Commands.PGP_ENCRYPT.getJsonName(), new PGPEncryptActionCommandParser());
        parsers = Collections.unmodifiableMap(p);
    }

    @Override
    public RuleField getAttributeName() {
        return RuleField.actioncmds;
    }

    @Override
    public boolean isNull(Rule rule) {
        return rule.getIfCommand() == null;
    }

    @Override
    public Object getAttribute(Rule rule) throws JSONException, OXException {
        if (isNull(rule)) {
            return null;
        }
        JSONArray array = new JSONArray();
        IfCommand ifCommand = rule.getIfCommand();
        List<ActionCommand> actionCommands = ifCommand.getActionCommands();
        for (ActionCommand actionCommand : actionCommands) {
            JSONObject object = new JSONObject();
            CommandParser<ActionCommand> parser = parsers.get(actionCommand.getCommand().getJsonName());
            if (parser != null) {
                parser.parse(object, actionCommand);
            }
            array.put(object);
        }
        return array;
    }

    @Override
    public void setAttribute(Rule rule, Object attribute, ServerSession session) throws JSONException, SieveException, OXException {
        if (isNull(rule)) {
            throw new SieveException("There is no if command where the action command can be applied to in rule " + rule);
        }

        IfCommand ifCommand = rule.getIfCommand();

        // Delete all existing actions, this is especially needed if this is used by update
        ifCommand.setActionCommands(null);

        // Parse action commands
        JSONArray array = (JSONArray) attribute;
        int length = array.length();
        List<ActionCommand> actionCommands = new ArrayList<ActionCommand>(length);
        for (int i = 0; i < length; i++) {
            JSONObject object = array.getJSONObject(i);
            String id = object.getString(GeneralField.id.name());
            CommandParser<ActionCommand> parser = parsers.get(id);
            if (parser == null) {
                throw new JSONException("Unknown action command while creating object: " + id);
            }
            actionCommands.add(parser.parse(object, session));
        }

        // Sanitize/sort them
        sort(actionCommands);

        // Add 'em
        ifCommand.setActionCommands(actionCommands);
    }

    // ---------------------------------------------------------------------------------------------------------------------

    /**
     * Sorts specified action commands associated with an <tt>"if"</tt> command.
     * <p>
     * Ensures a <tt>"fileinto"</tt> happens after any message-modifying command (e.g. <tt>"addflag"</tt>, <tt>"addheader"</tt>, <tt>"deleteheader"</tt>)
     * while trying to maintain the order of other commands.
     *
     * @param actionCommands The action commands to sort
     * @return The sorted action commands
     */
    protected static List<ActionCommand> sort(List<ActionCommand> actionCommands) {
        if (null == actionCommands) {
            return actionCommands;
        }

        int size = actionCommands.size();
        if (size <= 1) {
            return actionCommands;
        }

        int msize = size - 1;
        boolean keepOn = true;
        int start = 0;
        while (keepOn) {
            keepOn = false;

            for (int i = start; i <= msize; i++) {
                ActionCommand actionCommand1 = actionCommands.get(i);
                if (isFileInto(actionCommand1)) {
                    if (i < msize) {
                        int swap = -1;
                        for (int j = i+1; j <= msize; j++) {
                            ActionCommand actionCommand2 = actionCommands.get(j);
                            if (isMessageOp(actionCommand2)) {
                                swap = j;
                                j = size;
                            }
                        }

                        if (swap >= 0) {
                            actionCommands.add(i, actionCommands.remove(swap));
                            keepOn = true;
                            start = i + 1;
                            i = size;
                        }
                    }
                }
            }
        }

        return actionCommands;
    }

    private static boolean isFileInto(ActionCommand actionCommand) {
        return ActionCommand.Commands.FILEINTO.equals(actionCommand.getCommand());
    }

    private static final EnumSet<ActionCommand.Commands> MESSAGE_OPS = EnumSet.of(ActionCommand.Commands.REMOVEFLAG, ActionCommand.Commands.ADDFLAG, ActionCommand.Commands.ADDHEADER, ActionCommand.Commands.DELETEHEADER);

    private static boolean isMessageOp(ActionCommand actionCommand) {
        return MESSAGE_OPS.contains(actionCommand.getCommand());
    }

}
