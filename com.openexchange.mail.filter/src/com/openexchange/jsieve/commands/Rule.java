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

package com.openexchange.jsieve.commands;

import java.util.ArrayList;

/**
 * {@link Rule}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
// Comparable<Rule> allows only a Rule object for compare
public class Rule implements Comparable<Rule> {

    private RuleComment ruleComment;
    private boolean commented;
    private int lineNumber = -1;
    private int endLineNumber = -1;
    private int position = -1;
    private String text;
    private String errorMsg;
    private ArrayList<Command> commands;

    /**
     * Initialises a new {@link Rule}.
     */
    public Rule() {
        commands = new ArrayList<>();
    }

    /**
     * Initialises a new {@link Rule}.
     * 
     * @param ruleComment the rule comment containing metadata
     * @param commands The commands of the rule
     */
    public Rule(RuleComment ruleComment, ArrayList<Command> commands) {
        super();
        this.ruleComment = ruleComment;
        this.commands = commands;
    }

    /**
     * Initialises a new {@link Rule}.
     * 
     * @param commands the commands of the rule
     * @param lineNumber the line number
     */
    public Rule(ArrayList<Command> commands, int lineNumber) {
        super();
        this.commands = commands;
        this.lineNumber = lineNumber;
    }

    /**
     * Initialises a new {@link Rule}.
     * 
     * @param commands The commands of the rule
     * @param lineNumber The line number
     * @param endLineNumber The end line number
     * @param commented Whether the rule is commented-out (i.e. disabled)
     */
    public Rule(ArrayList<Command> commands, int lineNumber, int endLineNumber, boolean commented) {
        super();
        this.commands = commands;
        this.lineNumber = lineNumber;
        this.endLineNumber = endLineNumber;
        this.commented = commented;
    }

    /**
     * Initialises a new {@link Rule}.
     * 
     * @param commented Whether this rule is commented out (i.e. disabled)
     * @param lineNumber The line number
     * @param endLineNumber The end line number
     * @param errorMsg The error message
     */
    public Rule(boolean commented, int lineNumber, int endLineNumber, String errorMsg) {
        super();
        this.commented = commented;
        this.lineNumber = lineNumber;
        this.endLineNumber = endLineNumber;
        this.errorMsg = errorMsg;
    }

    /**
     * Initialises a new {@link Rule}.
     * 
     * @param ruleComment the rule comment containing metadata
     * @param commands The commands
     * @param commented Whether the rule is commented out (i.e. disabled)
     */
    public Rule(RuleComment ruleComment, ArrayList<Command> commands, boolean commented) {
        super();
        this.ruleComment = ruleComment;
        this.commands = commands;
        this.commented = commented;
    }

    /**
     * Initialises a new {@link Rule}.
     * 
     * @param ruleComment The rule comment containing metadata
     * @param commands The commands
     * @param linenumber The line number
     * @param commented Whether the rule is commented out (i.e. disabled)
     */
    public Rule(RuleComment ruleComment, ArrayList<Command> commands, int linenumber, boolean commented) {
        super();
        this.ruleComment = ruleComment;
        this.commands = commands;
        this.lineNumber = linenumber;
        this.commented = commented;
    }

    /**
     * Returns the rule coment
     * 
     * @return The rule coment
     */
    public final RuleComment getRuleComment() {
        return ruleComment;
    }

    /**
     * Adds the specified command to the rule
     * 
     * @param o The command to add
     * @return <code>true</code> if the command was successfully added; <code>false</code> otherwise
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addCommand(Command o) {
        return commands.add(o);
    }

    /**
     * Removes the specified command from the rule
     * 
     * @param o The command to remove
     * @return <code>true</code> if the command was successfully removed; <code>false</code> otherwise
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removeCommand(Object o) {
        return commands.remove(o);
    }

    /**
     * Returns the commands of this rule
     * 
     * @return The commands of this rule
     */
    public final ArrayList<Command> getCommands() {
        if (commands == null) {
            commands = new ArrayList<>();
        }
        return commands;
    }

    /**
     * A convenience method to get the require command if one is contained
     * 
     * @return the require command or null if none is contained
     */
    public final RequireCommand getRequireCommand() {
        // If a require command is contained here it is located at the first position
        if (null == commands) {
            return null;
        }
        if (commands.isEmpty()) {
            return null;
        }
        Command command = commands.get(0);
        if (command instanceof RequireCommand requireCommand) {
            return requireCommand;
        }
        return null;
    }

    /**
     * A convenience method to get the if command if one is contained
     * 
     * @return the if command or null if none is contained
     */
    public final IfCommand getIfCommand() {
        if (null == commands) {
            return null;
        }
        for (Command command : commands) {
            if (command instanceof IfCommand ifCommand) {
                return ifCommand;
            }
        }
        return null;
    }

    /**
     * A convenience method to get the test command if one is contained
     * 
     * @return the test command or null if none is contained
     */
    public final TestCommand getTestCommand() {
        IfCommand ifCommand = getIfCommand();
        if (null == ifCommand) {
            return null;
        }

        return ifCommand.getTestcommand();
    }

    /**
     * A convenience method for directly accessing the unique id
     * 
     * @return -1 if there is no unique id for this rule; a value > -1 otherwise
     */
    public int getUniqueId() {
        if (null == ruleComment) {
            return -1;
        }

        return ruleComment.getUniqueId();
    }

    /**
     * Sets the rule comment object
     * 
     * @param ruleComment The rule comment
     */
    public final void setRuleComments(RuleComment ruleComment) {
        this.ruleComment = ruleComment;
    }

    /**
     * Sets the commands
     * 
     * @param commands the commands to set
     */
    public final void setCommands(ArrayList<Command> commands) {
        this.commands = commands;
    }

    /**
     * Returns the line number
     * 
     * @return the line number
     */
    public final int getLinenumber() {
        return lineNumber;
    }

    /**
     * Sets the line number
     * 
     * @param linenumber the line number to set
     */
    public final void setLinenumber(int linenumber) {
        this.lineNumber = linenumber;
    }

    /**
     * Returns the position of the rule
     * 
     * @return the position of the rule
     */
    public final int getPosition() {
        return position;
    }

    /**
     * Sets the positio of the rule
     * 
     * @param position the position to set
     */
    public final void setPosition(int position) {
        this.position = position;
    }

    /**
     * Returns whether this rule is commented (i.e. disabled)
     * 
     * @return <code>true</code> if the rule is commented-out; <code>false</code> otherwise
     */
    public final boolean isCommented() {
        return commented;
    }

    /**
     * Sets whether this rule is commented-out
     * 
     * @param commented the value
     */
    public final void setCommented(boolean commented) {
        this.commented = commented;
    }

    /**
     * Returns the text of this rule
     * 
     * @return the text of this rule
     */
    public final String getText() {
        return text;
    }

    /**
     * Sets the text for this rule
     * 
     * @param text the text to set
     */
    public final void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the error message (if any)
     * 
     * @return the error message
     */
    public final String getErrormsg() {
        return errorMsg;
    }

    /**
     * Sets the error message
     * 
     * @param errorMsg the error message to set
     */
    public final void setErrormsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * Gets the end line number
     * 
     * @return the end line number
     */
    public final int getEndlinenumber() {
        return endLineNumber;
    }

    /**
     * Sets the end line number
     * 
     * @param endLineNumber the end line number to set
     */
    public final void setEndlinenumber(int endLineNumber) {
        this.endLineNumber = endLineNumber;
    }

    @Override
    public String toString() {
        return "Name: " + ((null != ruleComment && null != ruleComment.getRulename()) ? ruleComment.getRulename() : null) + ": " + commands;
    }

    @Override
    public int compareTo(Rule o) {
        return Integer.compare(lineNumber, o.lineNumber);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((commands == null) ? 0 : commands.hashCode());
        result = prime * result + (commented ? 1231 : 1237);
        result = prime * result + endLineNumber;
        result = prime * result + ((errorMsg == null) ? 0 : errorMsg.hashCode());
        result = prime * result + lineNumber;
        result = prime * result + position;
        result = prime * result + ((ruleComment == null) ? 0 : ruleComment.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Rule other = (Rule) obj;
        if (commands == null) {
            if (other.commands != null) {
                return false;
            }
        } else if (!commands.equals(other.commands)) {
            return false;
        }
        if (commented != other.commented) {
            return false;
        }
        if (endLineNumber != other.endLineNumber) {
            return false;
        }
        if (errorMsg == null) {
            if (other.errorMsg != null) {
                return false;
            }
        } else if (!errorMsg.equals(other.errorMsg)) {
            return false;
        }
        if (lineNumber != other.lineNumber) {
            return false;
        }
        if (position != other.position) {
            return false;
        }
        if (ruleComment == null) {
            if (other.ruleComment != null) {
                return false;
            }
        } else if (!ruleComment.equals(other.ruleComment)) {
            return false;
        }
        if (text == null) {
            if (other.text != null) {
                return false;
            }
        } else if (!text.equals(other.text)) {
            return false;
        }
        return true;
    }
}
