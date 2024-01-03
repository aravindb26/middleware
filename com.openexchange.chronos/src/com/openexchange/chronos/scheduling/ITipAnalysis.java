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

import java.util.List;
import java.util.Locale;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.EventField;

/**
 * 
 * {@link ITipAnalysis}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class ITipAnalysis {

    private SchedulingMethod method;
    private CalendarObjectResource originalResource;
    private CalendarObjectResource storedRelatedResource;
    private CalendarObjectResource resource;
    private String uid;
    private Locale locale;
    private List<AnalyzedChange> analyzedChanges;
    private AnalyzedChange mainChange;

    /**
     * Initializes a new {@link ITipAnalysis}.
     */
    public ITipAnalysis() {
        super();
    }

    /**
     * Get the scheduling method the analysis is for
     * 
     * @return The method
     */
    public SchedulingMethod getMethod() {
        return method;
    }

    /**
     * Get the changes the analysis is about
     *
     * @return The changes, can be empty
     */
    public List<AnalyzedChange> getAnalyzedChanges() {
        return analyzedChanges;
    }

    /**
     * Gets the change that best describes the overall changes.
     *
     * @return The main change, or <code>null</code> if there aren't changes at all
     */
    public AnalyzedChange getMainChange() {
        return mainChange;
    }

    /**
     * Get the event UID
     *
     * @return The event UID
     */
    public String getUid() {
        return uid;
    }

    /**
     * Gets the originalResource
     *
     * @return The originalResource
     */
    public CalendarObjectResource getOriginalResource() {
        return originalResource;
    }

    /**
     * Gets the stored resource that is related to the incoming scheduling object resource via its {@link EventField#RELATED_TO}.
     *
     * @return The stored related resource
     */
    public CalendarObjectResource getStoredRelatedResource() {
        return storedRelatedResource;
    }

    /**
     * Gets the resource
     *
     * @return The resource
     */
    public CalendarObjectResource getResource() {
        return resource;
    }

    /**
     * Gets the locale
     *
     * @return The locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the method
     *
     * @param method The method to set
     * @return This instance for chaining
     */
    public ITipAnalysis setMethod(SchedulingMethod method) {
        this.method = method;
        return this;
    }

    /**
     * Set the original resource
     *
     * @param originalResource The original resource
     * @return This instance for chaining
     */
    public ITipAnalysis setOriginalResource(CalendarObjectResource originalResource) {
        this.originalResource = originalResource;
        return this;
    }

    /**
     * Set the resource of the incoming scheduling message
     *
     * @param resource The resource
     * @return This instance for chaining
     */
    public ITipAnalysis setResource(CalendarObjectResource resource) {
        this.resource = resource;
        return this;
    }

    /**
     * Set the stored resource that is related to the incoming scheduling object resource via its {@link EventField#RELATED_TO}.
     *
     * @param storedRelatedResource The stored related resource
     * @return This instance for chaining
     */
    public ITipAnalysis setStoredRelatedResource(CalendarObjectResource storedRelatedResource) {
        this.storedRelatedResource = storedRelatedResource;
        return this;
    }

    /**
     * Set the changes the analysis is about
     *
     * @param analyzedChanges The changes, can be empty
     * @return This instance for chaining
     */
    public ITipAnalysis setChanges(List<AnalyzedChange> analyzedChanges) {
        this.analyzedChanges = analyzedChanges;
        return this;
    }

    /**
     * Set the mainChange changes the analysis is about
     *
     * @param mainChange The mainChange change, can be <code>null</code>
     * @return This instance for chaining
     */
    public ITipAnalysis setMainChange(AnalyzedChange mainChange) {
        this.mainChange = mainChange;
        return this;
    }

    /**
     * Set the UID of the event
     *
     * @param uid The event UID
     * @return This instance for chaining
     */
    public ITipAnalysis setUid(String uid) {
        this.uid = uid;
        return this;
    }

    /**
     * Sets the locale
     *
     * @param locale The locale to set
     * @return This instance for chaining
     */
    public ITipAnalysis setLocale(Locale locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public String toString() {
        return "ITipAnalysis [method=" + method + ", uid=" + uid + ", locale=" + locale + "]";
    }

}
