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

package com.openexchange.chronos.scheduling.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.scheduling.ITipAnnotation;
import com.openexchange.i18n.tools.StringHelper;

/**
 * 
 * {@link ITipAnnotationBuilder}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.0
 */
public class ITipAnnotationBuilder {

    /**
     * Creates a new builder
     *
     * @return A builder
     */
    public static ITipAnnotationBuilder newBuilder() {
        return new ITipAnnotationBuilder();
    }

    String message;
    Locale locale;
    List<Object> args = new ArrayList<>();
    Event additional;
    Map<String, Object> additionals;

    protected ITipAnnotationBuilder() {}

    /**
     * Set the message
     *
     * @param message The message
     * @return This instance for chaining
     */
    public ITipAnnotationBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * 
     * Set the locale for the user
     * 
     * @param locale The local
     * @return This instance for chaining
     */
    public ITipAnnotationBuilder locale(Locale locale) {
        this.locale = locale;
        return this;
    }

    /**
     * Thats additional arguments for the message
     *
     * @param args The arguments
     * @return This instance for chaining
     */
    public ITipAnnotationBuilder args(List<Object> args) {
        this.args.addAll(args);
        return this;
    }

    /**
     * Adds one arguments for the message
     *
     * @param args The argument
     * @return This instance for chaining
     */
    public ITipAnnotationBuilder addArgs(Object args) {
        this.args.add(args);
        return this;
    }

    /**
     * Set the additional event
     *
     * @param additional The event
     * @return This instance for chaining
     */
    public ITipAnnotationBuilder additional(Event additional) {
        this.additional = additional;
        return this;
    }

    /**
     * Sets a map containing additional parameters of the annotation.
     *
     * @param additionals The additional parameters
     * @return This instance for chaining
     */
    public ITipAnnotationBuilder additionals(Map<String, Object> additionals) {
        this.additionals = additionals;
        return this;
    }

    /**
     * Builds the object
     *
     * @return The annotation
     */
    public ITipAnnotation build() {
        return new ITipAnnotationImpl(this);
    }
}

class ITipAnnotationImpl implements ITipAnnotation {

    private final String message;
    private final List<Object> args;
    private final Event additional;
    private final Map<String, Object> additionals;

    /**
     * 
     * Initializes a new {@link ITipAnnotationBuilder}.
     *
     * @param builder The builder
     */
    public ITipAnnotationImpl(ITipAnnotationBuilder builder) {
        if (builder.locale != null) {
            this.message = StringHelper.valueOf(builder.locale).getString(builder.message);
        } else {
            this.message = builder.message;
        }
        this.additional = builder.additional;
        this.args = new ArrayList<Object>(builder.args);
        this.additionals = builder.additionals;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public List<Object> getArgs() {
        return args;
    }

    @Override
    public Event getEvent() {
        return additional;
    }

    @Override
    public Map<String, Object> getAdditionals() {
        return additionals;
    }

    @Override
    public String toString() {
        return "ITipAnnotation [message=" + message + ", args=" + args + ", additional=" + additional + "]";
    }
}
