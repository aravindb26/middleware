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

package com.openexchange.caching.internal;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import com.openexchange.caching.ElementAttributes;
import com.openexchange.caching.ElementEventHandler;

/**
 * {@link ElementAttributesImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ElementAttributesImpl implements ElementAttributes, Cloneable {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 5870240777519850914L;

    /**
     * Can this item be flushed to disk
     */
    private boolean isSpool = true;

    /**
     * Is this item laterally distributable
     */
    private boolean isLateral = true;

    /**
     * Can this item be sent to the remote cache
     */
    private boolean isRemote = true;

    /**
     * You can turn off expiration by setting this to true. This causes the cache to bypass both max life and idle time expiration.
     */
    private boolean isEternal = true;

    /**
     * The object version. This is currently not used.
     */
    /* private long version; */

    /**
     * Max life seconds
     */
    private long maxLifeSeconds = -1;

    /**
     * The maximum time an entry can be idle. Setting this to -1 causes the idle time check to be ignored.
     */
    private long maxIdleTimeSeconds = -1;

    /**
     * The byte size of the field. Must be manually set.
     */
    private int size;

    /**
     * The creation time. This is used to enforce the max life.
     */
    private long createTime;

    /**
     * The last access time. This is used to enforce the max idle time.
     */
    private long lastAccessTime;

    /**
     * The list of Event handlers to use. This is transient, since the event handlers cannot usually be serialized. This means that you
     * cannot attach a post serialization event to an item.
     * <p>
     * TODO we need to check that when an item is passed to a non-local cache that if the local cache had a copy with event handlers, that
     * those handlers are used.
     */
    private transient ArrayList<ElementEventHandler> eventHandlers;

    /**
     * Initializes a new {@link ElementAttributesImpl}
     */
    public ElementAttributesImpl() {
        super();
        createTime = System.currentTimeMillis();
        lastAccessTime = createTime;
    }

    /**
     * The readObject method is responsible for reading from the stream and restoring the classes fields. It may call in.defaultReadObject
     * to invoke the default mechanism for restoring the object's non-static and non-transient fields. The
     * {@link ObjectInputStream#defaultReadObject()} method uses information in the stream to assign the fields of the object saved in the
     * stream with the correspondingly named fields in the current object. This handles the case when the class has evolved to add new
     * fields. The method does not need to concern itself with the state belonging to its super classes or subclasses. State is saved by
     * writing the individual fields to the ObjectOutputStream using the writeObject method or by using the methods for primitive data types
     * supported by {@link DataOutput}.
     *
     * @param in The object input stream
     * @throws IOException If an I/O error occurs
     * @throws ClassNotFoundException If a casting fails
     */
    private void readObject(final java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        /*
         * Restore common fields
         */
        in.defaultReadObject();
        eventHandlers = null;
    }

    /**
     * Constructor for the element attributes object
     *
     * @param attr The element attributes object
     */
    protected ElementAttributesImpl(final ElementAttributesImpl attr) {
        isEternal = attr.isEternal;
        // Waterfall onto disk, for pure disk set memory to 0
        isSpool = attr.isSpool;
        // lateral
        isLateral = attr.isLateral;
        // central RMI store
        isRemote = attr.isRemote;
        maxLifeSeconds = attr.maxLifeSeconds;
        // time-to-live
        maxIdleTimeSeconds = attr.maxIdleTimeSeconds;
        size = attr.size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            final ElementAttributesImpl attr = (ElementAttributesImpl) super.clone();
            /*
             * Set create/last-access time to now and do not copy from this attributes
             */
            attr.createTime = System.currentTimeMillis();
            attr.lastAccessTime = attr.createTime;
            attr.eventHandlers = (ArrayList<ElementEventHandler>) eventHandlers.clone();
            return attr;
        } catch (CloneNotSupportedException e) {
            /*
             * Cannot occur since we are cloneable
             */
            throw new InternalError("Clone failed even though java.lang.Cloneable interface is implemented", e);
        }
    }

    @Override
    public void addElementEventHandler(final ElementEventHandler eventHandler) {
        // lazy here, no concurrency problems expected
        if (eventHandlers == null) {
            eventHandlers = new ArrayList<ElementEventHandler>();
        }
        eventHandlers.add(eventHandler);
    }

    @Override
    public void addElementEventHandlers(final ArrayList<ElementEventHandler> eventHandlers) {
        if (eventHandlers == null || eventHandlers.size() == 0) {
            return;
        }
        if (this.eventHandlers == null) {
            this.eventHandlers = new ArrayList<ElementEventHandler>();
        }
        eventHandlers.addAll(eventHandlers);
    }

    @Override
    public ElementAttributes copy() {
        try {
            return (ElementAttributes) clone();
        } catch (@SuppressWarnings("unused") final Exception e) {
            return new ElementAttributesImpl();
        }
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Queue<ElementEventHandler> getElementEventHandlers() {
        return (LinkedList<ElementEventHandler>) eventHandlers.clone();
    }

    @Override
    public long getIdleTime() {
        return maxIdleTimeSeconds;
    }

    @Override
    public boolean getIsEternal() {
        return isEternal;
    }

    @Override
    public boolean getIsLateral() {
        return isLateral;
    }

    @Override
    public boolean getIsRemote() {
        return isRemote;
    }

    @Override
    public boolean getIsSpool() {
        return isSpool;
    }

    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public long getMaxLifeSeconds() {
        return maxLifeSeconds;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public long getTimeToLiveSeconds() {
        final long now = System.currentTimeMillis();
        return ((getCreateTime() + (getMaxLifeSeconds() * 1000)) - now) / 1000;
    }

    @Override
    public void setIdleTime(final long idle) {
        maxIdleTimeSeconds = idle;
    }

    @Override
    public void setIsEternal(final boolean val) {
        isEternal = val;

    }

    @Override
    public void setIsLateral(final boolean val) {
        isLateral = val;
    }

    @Override
    public void setIsRemote(final boolean val) {
        isRemote = val;
    }

    @Override
    public void setIsSpool(final boolean val) {
        isSpool = val;
    }

    @Override
    public void setLastAccessTimeNow() {
        lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public void setMaxLifeSeconds(final long mls) {
        maxLifeSeconds = mls;
    }

    @Override
    public void setSize(final int size) {
        this.size = size;
    }

}
