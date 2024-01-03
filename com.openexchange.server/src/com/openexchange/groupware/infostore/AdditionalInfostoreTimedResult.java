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

package com.openexchange.groupware.infostore;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.SearchIterator;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.l;

/**
 * {@link AdditionalInfostoreTimedResult} is a {@link TimedResult} holding an additional time stamp which is included to compute the final sequence number.
 * <br>
 * <br>
 * The sequence number returned by {@link #sequenceNumber()} is the maximum sequence number from all results and the additional sequence number.
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class AdditionalInfostoreTimedResult implements TimedResult<DocumentMetadata> {

    private final TimedResult<DocumentMetadata> result;
    private final List<Long> sequenceNumbers;
    private Long sequenceNumber = null;

    /**
     * Initializes a new {@link AdditionalInfostoreTimedResult}.
     *
     * @param results The results
     * @param additionalSequenceNumber The additional time stamp to include in the computation of the max. sequence number being returned by {@link #sequenceNumber()}
     */
    public AdditionalInfostoreTimedResult(SearchIterator<DocumentMetadata> results, long additionalSequenceNumber) {
        this(new InfostoreTimedResult(results), additionalSequenceNumber);
    }

    /**
     * Initializes a new {@link AdditionalInfostoreTimedResult}.
     *
     * @param result The result
     * @param additionalSequenceNumber The additional time stamp to include in the computation of the max. sequence number being returned by {@link #sequenceNumber()}
     */
    public AdditionalInfostoreTimedResult(TimedResult<DocumentMetadata> result, long additionalSequenceNumber) {
        this.result = result;
        this.sequenceNumbers = new ArrayList<>();
        this.sequenceNumbers.add(L(additionalSequenceNumber));
    }

    @Override
    public SearchIterator<DocumentMetadata> results() throws OXException {
        return result.results();
    }

    @Override
    public long sequenceNumber() throws OXException {
        if (sequenceNumber == null) {
            sequenceNumbers.add(L(result.sequenceNumber()));
            sequenceNumber = L(sequenceNumbers.stream().mapToLong(number -> l(number)).max().orElse(0));
        }
        return l(sequenceNumber);
    }
}
