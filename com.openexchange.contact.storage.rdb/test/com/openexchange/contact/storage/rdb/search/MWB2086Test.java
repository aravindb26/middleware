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

package com.openexchange.contact.storage.rdb.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import com.openexchange.java.SimpleTokenizer;
import com.openexchange.junit.ParallelParameterized;

/**
 * {@link MWB2086Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
@RunWith(ParallelParameterized.class)
public class MWB2086Test {

    @Parameters(name = "{index}: {0}")
    public static Iterable<? extends Object> data() {
        return Arrays.asList("xxx'", "x'xx", "'xxx", "xxx''", "'''xxx", "x'x'x''x");
    }

    private String prefix;

    /**
     * Initializes a new {@link MWB2086Test}.
     *
     * @param prefix The prefix to use in the test
     */
    public MWB2086Test(String prefix) {
        super();
        this.prefix = prefix;
    }

    @Test
    public void testEscapeSingleQuotes() {
        List<String> patterns = FulltextAutocompleteAdapter.preparePatterns(SimpleTokenizer.tokenize(prefix));
        assertEquals("unexpected length of tokenized patterns", 1, patterns.size());
        String preparedPattern = patterns.get(0);
        for (int i = 0; i < preparedPattern.length(); i++) {
            char c = preparedPattern.charAt(i);
            if ('\'' == c) {
                assertTrue(0 < i);
                assertEquals('\\', preparedPattern.charAt(i - 1));

            }
        }
    }

}
