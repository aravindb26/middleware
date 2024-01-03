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

package com.openexchange.chronos.scheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.chronos.Attendee;

/**
 * {@link AnalyzedChange}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class AnalyzedChange {

    private List<ITipAnnotation> annotations;
    private ITipChange change;
    private Set<ITipAction> actions;
    private Attendee targetedAttendee;

    /**
     * Initializes a new {@link AnalyzedChange}.
     */
    public AnalyzedChange() {
        super();
        this.annotations = new ArrayList<ITipAnnotation>();
        this.actions = new LinkedHashSet<ITipAction>();
    }

    /**
     * Adds an annotation for the analyzed change.
     * 
     * @param annotation The annotation to add
     * @return This instance for chaining
     */
    public AnalyzedChange addAnnotation(ITipAnnotation annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds annotations for the analyzed change.
     * 
     * @param annotations The annotations to add
     * @return This instance for chaining
     */
    public AnalyzedChange addAnnotations(Collection<ITipAnnotation> annotations) {
        if (null != annotations) {
            this.annotations.addAll(annotations);
        }
        return this;
    }

    /**
     * Gets the previously added annotations describing the analyzed change.
     * 
     * @return The annotations, or an empty list if there are none
     */
    public List<ITipAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * Gets the change providing further details about new/modified/cancelled
     * appointments.
     * 
     * @return The change
     * @return This instance for chaining
     */
    public ITipChange getChange() {
        return change;
    }

    /**
     * Sets the change providing further details about new/modified/cancelled
     * appointments.
     * 
     * @param change The change to set
     * @return This instance for chaining
     */
    public AnalyzedChange setChange(ITipChange change) {
        this.change = change;
        return this;
    }

    /**
     * Gets the attendee targeted by the scheduling message.
     * 
     * @return The targeted attendee, or <code>null</code> if not applicable
     */
    public Attendee getTargetedAttendee() {
        return targetedAttendee;
    }

    /**
     * Sets the change providing further details about new/modified/cancelled
     * appointments.
     * 
     * @param change The change to set
     * @return This instance for chaining
     */
    public AnalyzedChange setTargetedAttendee(Attendee targetedAttendee) {
        this.targetedAttendee = targetedAttendee;
        return this;
    }

    /**
     * Gets the previously added actions that are suggested for the analyzed change.
     * 
     * @return The actions, or an empty list if there are none
     */
    public Set<ITipAction> getActions() {
        return actions;
    }

    /**
     * Adds one or more suggested actions for the analyzed change.
     * 
     * @param actions The actions to add
     * @return This instance for chaining
     */
    public AnalyzedChange addActions(ITipAction... actions) {
        if (null != actions) {
            this.actions.addAll(Arrays.asList(actions));
        }
        return this;
    }

    /**
     * Adds one or more suggested actions for the analyzed change.
     * 
     * @param actions The actions to add
     * @return This instance for chaining
     */
    public AnalyzedChange addActions(Collection<ITipAction> actions) {
        if (null != actions) {
            this.actions.addAll(actions);
        }
        return this;
    }

}
