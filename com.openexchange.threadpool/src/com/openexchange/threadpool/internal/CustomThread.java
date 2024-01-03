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

package com.openexchange.threadpool.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.java.Strings;
import com.openexchange.marker.OXThreadMarker;
import com.openexchange.threadpool.ThreadRenamer;

/**
 * {@link CustomThread} - Enhances {@link Thread} class by a setter/getter method for a thread's original name.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CustomThread extends Thread implements ThreadRenamer, OXThreadMarker {

    private volatile String originalName;
    private volatile String appendix;
    private volatile boolean changed;
    private volatile boolean httpProcessing;
    private Map<String, Object> threadValues; // Only accessed using this thread

    /**
     * Initializes a new {@link CustomThread}.
     *
     * @param target The object whose run method is called
     * @param name The name of the new thread which is also used as original name
     */
    public CustomThread(final Runnable target, final String name) {
        super(target, name);
        applyName(name);
    }

    private Map<String, Object> requireValues() {
        Map<String, Object> values = threadValues;
        if (values == null) {
            values = new HashMap<>();
            this.threadValues = values;
        }
        return values;
    }

    private void applyName(final String name) {
        originalName = name;
        int pos = originalName.indexOf('-');
        appendix = pos > 0 ? name.substring(pos) : null;
    }

    @Override
    public boolean isHttpRequestProcessing() {
        return httpProcessing;
    }

    @Override
    public void setHttpRequestProcessing(boolean httpProcessing) {
        this.httpProcessing = httpProcessing;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getThreadLocalValue(String key) {
        if (Thread.currentThread() != this) {
            throw new IllegalAccessError("Thread-local value must not be accessed from another thread");
        }

        if (key == null) {
            return null;
        }

        Map<String, Object> values = threadValues;
        return values == null ? null : (V) values.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V putThreadLocalValue(String key, V value) {
        if (Thread.currentThread() != this) {
            throw new IllegalAccessError("Thread-local value must not be accessed from another thread");
        }

        return key == null || value == null ? null : (V) requireValues().put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V removeThreadLocalValue(String key) {
        if (Thread.currentThread() != this) {
            throw new IllegalAccessError("Thread-local value must not be accessed from another thread");
        }

        if (key == null) {
            return null;
        }

        Map<String, Object> values = threadValues;
        return values == null ? null : (V) values.remove(key);
    }

    @Override
    public void removeThreadLocalValues(Collection<String> keys) {
        if (Thread.currentThread() != this) {
            throw new IllegalAccessError("Thread-local value must not be accessed from another thread");
        }

        if (keys == null) {
            return;
        }

        Map<String, Object> values = threadValues;
        if (values != null) {
            for (String key : keys) {
                values.remove(key);
            }
        }
    }

    @Override
    public void removeThreadLocalValuesByPrefix(String prefix) {
        if (Thread.currentThread() != this) {
            throw new IllegalAccessError("Thread-local value must not be accessed from another thread");
        }

        if (Strings.isEmpty(prefix)) {
            return;
        }

        Map<String, Object> values = threadValues;
        if (values != null) {
            for (Iterator<Entry<String, Object>> it = values.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Object> entry = it.next();
                if (entry.getKey().startsWith(prefix)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Clears the thread-local values.
     */
    public void clearThreadLocalValues() {
        Map<String, Object> values = threadValues;
        if (values != null) {
            values.clear();
        }
    }

    /**
     * Gets the original name.
     *
     * @return The original name
     */
    public String getOriginalName() {
        return originalName;
    }

    @Override
    public void restoreName() {
        if (!changed) {
            return;
        }
        setName(originalName);
        changed = false;
    }

    @Override
    public void rename(final String newName) {
        setName(newName);
        changed = true;
    }

    @Override
    public void renamePrefix(final String newPrefix) {
        if (null == appendix) {
            setName(newPrefix);
        } else {
            setName(new StringBuilder(16).append(newPrefix).append(appendix).toString());
        }
        changed = true;
    }

}
