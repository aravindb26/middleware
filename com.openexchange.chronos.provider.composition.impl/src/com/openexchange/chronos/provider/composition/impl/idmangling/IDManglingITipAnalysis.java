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

package com.openexchange.chronos.provider.composition.impl.idmangling;

import static com.openexchange.chronos.provider.composition.impl.idmangling.IDMangling.withUniqueID;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.chronos.scheduling.ITipChange;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.common.DefaultChange;
import com.openexchange.chronos.scheduling.common.ITipAnnotationBuilder;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.tools.arrays.Collections;

/**
 * 
 * {@link IDManglingITipAnalysis}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class IDManglingITipAnalysis extends ITipAnalysis {

    private final ITipAnalysis delegatee;
    protected final int accountId;

    /**
     * Initializes a new {@link IDManglingITipAnalysis}.
     * 
     * @param delegatee The delegatee to delegate method calls to
     * @param accountId The account identifier
     *
     */
    public IDManglingITipAnalysis(ITipAnalysis delegatee, int accountId) {
        super();
        this.delegatee = delegatee;
        this.accountId = accountId;
    }

    @Override
    public SchedulingMethod getMethod() {
        return delegatee.getMethod();
    }

    @Override
    public String getUid() {
        return delegatee.getUid();
    }

    @Override
    public Locale getLocale() {
        return delegatee.getLocale();
    }

    @Override
    public AnalyzedChange getMainChange() {
        return mangleAnalyzedChange(delegatee.getMainChange());
    }

    @Override
    public List<AnalyzedChange> getAnalyzedChanges() {
        List<AnalyzedChange> analyzedChanges = delegatee.getAnalyzedChanges();
        if (isNullOrEmpty(analyzedChanges)) {
            return analyzedChanges;
        }
        List<AnalyzedChange> changes = new ArrayList<>(analyzedChanges.size());
        for (AnalyzedChange analyzedChange : analyzedChanges) {
            AnalyzedChange modified = mangleAnalyzedChange(analyzedChange);
            changes.add(modified);
        }
        return changes;
    }

    private AnalyzedChange mangleAnalyzedChange(AnalyzedChange analyzedChange) {
        AnalyzedChange modified = new AnalyzedChange();
        modified.addActions(analyzedChange.getActions());
        modified.setChange(mangleChange(analyzedChange.getChange()));
        modified.setTargetedAttendee(analyzedChange.getTargetedAttendee());
        modified.addAnnotations(mangleAnnotations(analyzedChange.getAnnotations()));
        return modified;
    }

    private ITipChange mangleChange(ITipChange change) {
        DefaultChange modified = new DefaultChange();
        modified.setType(change.getType());
        if (Collections.isNotEmpty(change.getConflicts())) {
            List<EventConflict> conflicts = new ArrayList<>(change.getConflicts().size());
            for (EventConflict conflict : change.getConflicts()) {
                conflicts.add(new IDManglingEventConflict(conflict, accountId));
            }
            modified.setConflicts(conflicts);
        }
        modified.setCurrentEvent(withUniqueID(change.getCurrentEvent(), accountId));
        modified.setDeleted(withUniqueID(change.getDeletedEvent(), accountId));
        modified.setNewEvent(withUniqueID(change.getNewEvent(), accountId));
        return modified;
    }

    public List<ITipAnnotation> mangleAnnotations(Collection<ITipAnnotation> annotations) {
        if (isNullOrEmpty(annotations)) {
            return null;
        }
        List<ITipAnnotation> modified = new ArrayList<>(annotations.size());
        for (ITipAnnotation annotation : annotations) {
            modified.add(ITipAnnotationBuilder.newBuilder() //@formatter:off
                .message(annotation.getMessage())
                .additional(withUniqueID(annotation.getEvent(), accountId))
                .args(annotation.getArgs())
                .additionals(annotation.getAdditionals())
            .build()); //@formatter:on
        }
        return modified;
    }

    @Override
    public CalendarObjectResource getOriginalResource() {
        if (null == delegatee.getOriginalResource()) {
            return null;
        }
        return new IDManglingCalendarObjectResource(delegatee.getOriginalResource(), accountId);
    }

    @Override
    public CalendarObjectResource getResource() {
        if (null == delegatee.getResource()) {
            return null;
        }
        return new IDManglingCalendarObjectResource(delegatee.getResource(), accountId);
    }

}
