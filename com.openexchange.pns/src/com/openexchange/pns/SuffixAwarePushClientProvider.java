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
package com.openexchange.pns;

import java.util.Optional;
import com.openexchange.push.clients.PushClientProvider;

/**
 * {@link SuffixAwarePushClientProvider} is a delegating {@link PushClientProvider} which adds a suffix to the client id before delegating it
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class SuffixAwarePushClientProvider<S> implements PushClientProvider<S> {

    private final PushClientProvider<S> delegate;
    private final String suffix;

    /**
     * Initializes a new {@link SuffixAwarePushClientProvider}.
     *
     * @param delegate The {@link PushClientProvider} to delegate to
     * @param suffix The suffix to append to every client id
     */
    public SuffixAwarePushClientProvider(PushClientProvider<S> delegate, String suffix) {
        super();
        this.delegate = delegate;
        this.suffix = suffix;
    }

    @Override
    public Optional<S> optClient(String id) {
        return delegate.optClient(id + suffix);
    }

}
