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

package com.openexchange.groupware.infostore.search.impl;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import com.openexchange.java.Autoboxing;

/**
 * {@link ListPartitioningTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 8.0.0
 */
public class ListPartitioningTest {
    
    @Test
    public void testListPartitioning() {
        List<Character> alphabet = Arrays.asList(Autoboxing.c2C("abcdefghijklmnopqrstuvwxyz".toCharArray()));
        for (int i = 0; i <= alphabet.size(); i++) {
            List<Character> firstList = alphabet.subList(0, i);
            List<Character> secondList = alphabet.subList(firstList.size(), alphabet.size());
            for (int maxPartitionSize = 1; maxPartitionSize < alphabet.size() + 2; maxPartitionSize++) {
                testListPartitioning(firstList, secondList, maxPartitionSize);
            }
        }
    }

    private void testListPartitioning(List<Character> firstList, List<Character> secondList, int maxPartitionSize) {
        List<Character> referenceList = new LinkedList<Character>(firstList);
        referenceList.addAll(secondList);
        List<Character> repartitionedList = new LinkedList<Character>();
        List<List<Character>> partitionedLists;
        int partitionIndex = 0;
        while (null != (partitionedLists = SearchEngineImpl.getPartitionedLists(firstList, secondList, maxPartitionSize, partitionIndex++))) {
            repartitionedList.addAll(partitionedLists.get(0));
            repartitionedList.addAll(partitionedLists.get(1));
        }
        assertEquals(referenceList, repartitionedList);
    }
    
}
