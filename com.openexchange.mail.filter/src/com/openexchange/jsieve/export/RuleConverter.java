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

package com.openexchange.jsieve.export;

import java.util.ArrayList;
import java.util.List;
import org.apache.jsieve.NumberArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.parser.generated.ASTargument;
import org.apache.jsieve.parser.generated.ASTarguments;
import org.apache.jsieve.parser.generated.ASTblock;
import org.apache.jsieve.parser.generated.ASTcommand;
import org.apache.jsieve.parser.generated.ASTcommands;
import org.apache.jsieve.parser.generated.ASTstart;
import org.apache.jsieve.parser.generated.ASTstring;
import org.apache.jsieve.parser.generated.ASTstring_list;
import org.apache.jsieve.parser.generated.ASTtest;
import org.apache.jsieve.parser.generated.ASTtest_list;
import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.Token;
import com.openexchange.jsieve.commands.ActionCommand;
import com.openexchange.jsieve.commands.Command;
import com.openexchange.jsieve.commands.ElseCommand;
import com.openexchange.jsieve.commands.IfCommand;
import com.openexchange.jsieve.commands.IfOrElseIfCommand;
import com.openexchange.jsieve.commands.RequireCommand;
import com.openexchange.jsieve.commands.Rule;
import com.openexchange.jsieve.commands.TestCommand;

/**
 * This class contains all the methods to convert a list of rules into the AST
 * notation
 *
 * @author d7
 *
 */
public class RuleConverter {

    /**
     * Initializes a new {@link RuleConverter}.
     */
    private RuleConverter() {
        super();
    }

    private static void addArguments(final ASTarguments targuments, final List<Object> argumentslist, final int[] js, final int[] p) {
        for (int k = 0; k < argumentslist.size(); k++) {
            Object object = argumentslist.get(k);
            if (object instanceof List) {
                final List<String> sublist = (List<String>) object;
                final ASTargument arg = new ASTargument(js[0]++);
                arg.jjtAddChild(createStringList(sublist, js), 0);
                targuments.jjtAddChild(arg, p[0]++);
            } else if (object instanceof NumberArgument arg) {
                final ASTargument targument = new ASTargument(js[0]++);
                targument.setValue(arg);
                targuments.jjtAddChild(targument, p[0]++);
            } else if (object instanceof TagArgument tag) {
                addTagArgument(targuments, tag.getTag(), js, p);
            }
        }
    }

    private static void addTagArgument(final ASTarguments targuments2, final String text, final int[] js, final int[] p) {
        final Token token = new Token();
        token.image = text;
        final ASTargument targument = new ASTargument(js[0]++);
        targument.setValue(new TagArgument(token));
        targuments2.jjtAddChild(targument, p[0]++);
    }

    private static ASTblock createActionBlockForTest(final List<ActionCommand> actioncommands, final int[] js, final int linenumber) {
        final ASTblock tblock = new ASTblock(js[0]++);
        tblock.setFirstToken(getDummyToken("{"));
        tblock.setLastToken(getDummyToken("}"));
        tblock.jjtAddChild(createActionCommands(actioncommands, js, linenumber), 0);
        return tblock;
    }

    private static ASTcommand createActionCommand(final ArrayList<Object> arguments, final String commandname, final int[] js, final int line) {
        final ASTcommand tcommand = createCommand(commandname, js, line);
        if (!arguments.isEmpty()) {
            tcommand.jjtAddChild(createArguments(arguments, js), 0);
        }
        return tcommand;
    }

    private static ASTcommands createActionCommands(final List<ActionCommand> actioncommands, final int[] js, final int linenumber) {
        final ASTcommands tcommands = new ASTcommands(js[0]++);
        for (int k = 0; k < actioncommands.size(); k++) {
            final ActionCommand actionCommand = actioncommands.get(k);
            final String commandname = actionCommand.getCommand().getCommandName();
            final ASTcommand tcommand = createActionCommand(actionCommand.getArguments(), commandname, js, linenumber);
            tcommands.jjtAddChild(tcommand, k);
        }
        return tcommands;
    }

    private static ASTarguments createArguments(final ArrayList<Object> arguments, final int[] iarray) {
        final ASTarguments targuments = new ASTarguments(iarray[0]++);
        for (int i = 0; i < arguments.size(); i++) {
            final Object obj = arguments.get(i);
            if (obj instanceof List arrayList) {
                ASTargument targument = new ASTargument(iarray[0]++);
                targument.jjtAddChild(createStringList(arrayList, iarray), 0);
                targuments.jjtAddChild(targument, i);
            } else if (obj instanceof NumberArgument arg) {
                ASTargument targument = new ASTargument(iarray[0]++);
                targument.setValue(arg);
                targuments.jjtAddChild(targument, i);
            } else if (obj instanceof TagArgument) {
                ASTargument targument = new ASTargument(iarray[0]++);
                targument.setValue(obj);
                targuments.jjtAddChild(targument, i);
            }
        }
        return targuments;
    }

    private static ASTcommand createCommand(final String commandname, final int[] iarray, final int line) {
        int i = iarray[0];
        final ASTcommand tcommand = new ASTcommand(i++);
        tcommand.setName(commandname);
        tcommand.setFirstToken(getDummyToken(commandname, line));
        tcommand.setLastToken(getDummyToken(";"));
        iarray[0] = i;
        return tcommand;
    }

    private static ASTtest createCompleteTestPart(final TestCommand testcommand, final int[] js) {
        final ASTtest ttest = new ASTtest(js[0]++);
        final List<TestCommand> testcommands = testcommand.getTestCommands();
        final String commandname = testcommand.getCommand().getCommandName();
        ttest.setName(commandname);
        if ("not".equals(commandname)) {
            ttest.jjtAddChild(createCompleteTestPart(testcommands.get(0), js), 0);
            return ttest;
        }
        if (!testcommands.isEmpty()) {
            final ASTarguments targuments = new ASTarguments(js[0]++);
            ttest.jjtAddChild(targuments, 0);
            final ASTtest_list ttest_list = new ASTtest_list(js[0]++);
            final int size = testcommands.size();
            ttest_list.setFirstToken(getDummyToken("("));
            ttest_list.setLastToken(getDummyToken(")"));
            targuments.jjtAddChild(ttest_list, 0);
            for (int i = 0; i < size; i++) {
                final TestCommand testCommand2 = testcommands.get(i);
                ttest_list.jjtAddChild(createCompleteTestPart(testCommand2, js), i);
            }
        } else {
            ttest.jjtAddChild(createTagAndNormalArgumentsForTest(testcommand, js), 0);
        }
        return ttest;
    }

    private static ASTstring_list createStringList(final List<String> arrayList, final int[] iarray) {
        int i = iarray[0];
        final ASTstring_list tstring_list = new ASTstring_list(i++);
        final int size = arrayList.size();
        // The sieve syntax forces brackets only if there are more than one
        // arguments
        if (size > 1) {
            tstring_list.setFirstToken(getDummyToken("["));
            tstring_list.setLastToken(getDummyToken("]"));
        }
        final StringBuilder builder = new StringBuilder(128).append('"');
        for (int k = 0; k < size; k++) {
            final ASTstring tstring = new ASTstring(i++);
            builder.setLength(1);
            tstring.setValue(builder.append(arrayList.get(k).replace("\\", "\\\\").replace("\"", "\\\"")).append('"').toString());
            tstring_list.jjtAddChild(tstring, k);
        }
        iarray[0] = i;
        return tstring_list;
    }

    private static ASTarguments createTagAndNormalArgumentsForTest(final TestCommand testcommand, final int[] js) {
        final ASTarguments targuments2 = new ASTarguments(js[0]++);
        final int[] p = new int[] { 0 };
        addArguments(targuments2, testcommand.getArguments(), js, p);
        return targuments2;
    }

    private static Token getDummyToken(final String string) {
        final Token token = new Token();
        token.image = string;
        token.beginColumn = 0;
        token.beginLine = 0;
        token.endColumn = 0;
        token.endLine = 0;
        return token;
    }

    private static Token getDummyToken(final String string, final int beginLine) {
        final Token token = new Token();
        token.image = string;
        token.beginColumn = 0;
        token.beginLine = beginLine;
        token.endColumn = 0;
        token.endLine = 0;
        return token;
    }

    /**
     * Converts specified rules to an appropriate {@link Node} instance.
     *
     * @param rules The rules to convert
     * @return The resulting {@link Node} instance
     */
    public static Node rulesToNodes(List<Rule> rules) {
        Node startnode = new ASTstart(0);
        ASTcommands tcommands = new ASTcommands(1);
        startnode.jjtAddChild(tcommands, 0);
        // The general counter for the node id
        int i = 2;
        // The children counter for tcommand
        int o = 0;
        for (Rule rule : rules) {
            ArrayList<Command> commands = rule.getCommands();
            if (null == commands) {
                continue;
            }
            for (final Command command : commands) {
                final int[] js = new int[] { i };
                if (command instanceof RequireCommand requireCommand) {
                    final ASTcommand tcommand = createCommand("require", js, rule.getLinenumber());

                    final ArrayList<String> list = requireCommand.getList().get(0);
                    final ASTarguments targuments = new ASTarguments(js[0]++);
                    final ASTargument targument = new ASTargument(js[0]++);
                    tcommand.jjtAddChild(targuments, 0);
                    targuments.jjtAddChild(targument, 0);
                    targument.jjtAddChild(createStringList(list, js), 0);
                    tcommands.jjtAddChild(tcommand, o);
                } else if (command instanceof IfOrElseIfCommand ifCommand) {
                    // We need an array here, because we have to make
                    // call-by-reference through the call-by-value of java
                    ASTcommand tcommand;
                    if (command instanceof IfCommand) {
                        tcommand = createCommand("if", js, rule.getLinenumber());
                    } else {
                        tcommand = createCommand("elsif", js, rule.getLinenumber());
                    }
                    final ASTarguments arguments = new ASTarguments(js[0]++);
                    final TestCommand testcommand = ifCommand.getTestcommand();
                    final ASTtest ttest = createCompleteTestPart(testcommand, js);
                    arguments.jjtAddChild(ttest, 0);
                    tcommand.jjtAddChild(arguments, 0);

                    // ... and finally the actioncommand block
                    final ASTblock tblock = createActionBlockForTest(ifCommand.getActionCommands(), js, rule.getLinenumber());
                    tcommand.jjtAddChild(tblock, 1);
                    tcommands.jjtAddChild(tcommand, o);
                } else if (command instanceof ElseCommand elseCommand) {
                    // We need an array here, because we have to make
                    // call-by-reference through the call-by-value of java
                    final ASTcommand tcommand = createCommand("else", js, rule.getLinenumber());

                    final ASTarguments arguments = new ASTarguments(js[0]++);
                    tcommand.jjtAddChild(arguments, 0);

                    // ... and finally the actioncommand block
                    final ASTblock tblock = createActionBlockForTest(elseCommand.getActionCommands(), js, rule.getLinenumber());
                    tcommand.jjtAddChild(tblock, 1);
                    tcommands.jjtAddChild(tcommand, o);
                } else if (command instanceof ActionCommand actionCommand) {
                    // We need an array here, because we have to make
                    // call-by-reference through the call-by-value of java
                    final ASTcommand tcommand = createActionCommand(actionCommand.getArguments(), actionCommand.getCommand().getCommandName(), js, rule.getLinenumber());
                    tcommands.jjtAddChild(tcommand, o);
                }
                o++;
            }
        }
        return startnode;
    }

}
