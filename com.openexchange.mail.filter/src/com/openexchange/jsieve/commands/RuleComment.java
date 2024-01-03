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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.List;
import java.util.Objects;

/**
 * {@link RuleComment}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class RuleComment {

    private List<String> flags;
    private String ruleName;
    private String errorText;
    private int uniqueId = -1;
    private int line = -1;
    private long updateTimestamp = -1;
    private String sourceOfUpdate;

    /**
     * 
     * Initialises a new {@link RuleComment}.
     * 
     * @param flags the flags
     * @param uniqueid The unique id
     * @param ruleName The rule name
     * @param line The line
     */
    public RuleComment(List<String> flags, int uniqueid, String ruleName, int line) {
        super();
        this.flags = flags;
        this.ruleName = ruleName;
        uniqueId = uniqueid;
        this.line = line;
    }

    /**
     * 
     * Initialises a new {@link RuleComment}.
     * 
     * @param line the line
     * @param errorText the error text
     */
    public RuleComment(int line, String errorText) {
        super();
        this.errorText = errorText;
        this.line = line;
    }

    /**
     * 
     * Initialises a new {@link RuleComment}.
     * 
     * @param uniqueId The unique id
     * @param ruleName The rule name
     * @param line The line
     */
    public RuleComment(int uniqueId, String ruleName, int line) {
        super();
        this.uniqueId = uniqueId;
        this.ruleName = ruleName;
        this.line = line;
    }

    /**
     * 
     * Initialises a new {@link RuleComment}.
     * 
     * @param uniqueId The unique id
     */
    public RuleComment(int uniqueId) {
        super();
        this.uniqueId = uniqueId;
    }

    /**
     * Initialises a new {@link RuleComment}.
     * 
     * @param ruleName the rule name
     */
    public RuleComment(String ruleName) {
        super();
        this.ruleName = ruleName;
    }

    /**
     * Initialises a new {@link RuleComment}.
     * 
     * @param flags the flags
     */
    public RuleComment(List<String> flags) {
        super();
        this.flags = flags;
    }

    /**
     * Returns the rule name
     * 
     * @return The rule name
     */
    public final String getRulename() {
        return ruleName;
    }

    /**
     * Returns the line
     * 
     * @return the line
     */
    public final int getLine() {
        return line;
    }

    /**
     * Sets the rule name
     * 
     * @param ruleName the rule name to set
     */
    public final void setRulename(String ruleName) {
        this.ruleName = ruleName;
    }

    /**
     * Sets the line
     * 
     * @param line the line to set
     */
    public final void setLine(int line) {
        this.line = line;
    }

    /**
     * Returns the flags
     * 
     * @return the flags
     */
    public final List<String> getFlags() {
        return flags;
    }

    /**
     * Sets the flags
     * 
     * @param flags the flags to set
     */
    public final void setFlags(List<String> flags) {
        this.flags = flags;
    }

    /**
     * Returns the error text
     * 
     * @return the error text
     */
    public final String getErrortext() {
        return errorText;
    }

    /**
     * Sets the error text
     * 
     * @param errortext the error text to set
     */
    public final void setErrortext(String errortext) {
        errorText = errortext;
    }

    /**
     * Returns the unique id
     * 
     * @return the unique id
     */
    public final int getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets the unique id
     * 
     * @param uniqueId the unique id to set
     */
    public final void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Returns the unique id
     * 
     * @return the unique id
     * @deprecated Use {@link #getUniqueId()} instead
     */
    @Deprecated(since = "8.17", forRemoval = true)
    public final int getUniqueid() {
        return getUniqueId();
    }

    /**
     * Sets the unique id
     * 
     * @param uniqueId the unique id to set
     * @deprecated Use {@link #setUniqueId(int)} instead
     */
    @Deprecated(since = "8.17", forRemoval = true)
    public final void setUniqueid(int uniqueId) {
        setUniqueId(uniqueId);
    }

    /**
     * Gets the updateTimestamp
     *
     * @return The updateTimestamp
     */
    public long getUpdateTimestamp() {
        return updateTimestamp;
    }

    /**
     * Sets the updateTimestamp
     *
     * @param updateTimestamp The updateTimestamp to set
     */
    public void setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    /**
     * Gets the sourceOfUpdate
     *
     * @return The sourceOfUpdate
     */
    public String getSourceOfUpdate() {
        return sourceOfUpdate;
    }

    /**
     * Sets the sourceOfUpdate
     *
     * @param sourceOfUpdate The sourceOfUpdate to set
     */
    public void setSourceOfUpdate(String sourceOfUpdate) {
        this.sourceOfUpdate = sourceOfUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorText, flags, I(line), ruleName, sourceOfUpdate, I(uniqueId), L(updateTimestamp));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        RuleComment other = (RuleComment) obj;
        //@formatter:off
        return Objects.equals(errorText, other.errorText) 
            && Objects.equals(flags, other.flags) 
            && line == other.line 
            && Objects.equals(ruleName, other.ruleName) 
            && Objects.equals(sourceOfUpdate, other.sourceOfUpdate) 
            && uniqueId == other.uniqueId 
            && updateTimestamp == other.updateTimestamp;
        //@formatter:on
    }

    @Override
    public String toString() {
        return "## Flag: " + flags + "|Unique: " + uniqueId + "|Name: " + ruleName + "...line" + line + "|Last Update: " + updateTimestamp + "|IP: " + sourceOfUpdate;
    }
}
