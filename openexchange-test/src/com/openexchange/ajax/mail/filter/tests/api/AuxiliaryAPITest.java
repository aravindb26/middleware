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

package com.openexchange.ajax.mail.filter.tests.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.mail.filter.api.dao.Rule;
import com.openexchange.ajax.mail.filter.api.dao.action.Keep;
import com.openexchange.ajax.mail.filter.api.dao.action.Stop;
import com.openexchange.ajax.mail.filter.api.dao.test.TrueTest;
import com.openexchange.ajax.mail.filter.tests.AbstractMailFilterTest;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AuxiliaryAPITest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class AuxiliaryAPITest extends AbstractMailFilterTest {

    /**
     * Initialises a new {@link AuxiliaryAPITest}.
     *
     * @param name test case's name
     */
    public AuxiliaryAPITest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        // Create 10 rules
        List<Rule> expectedRules = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Rule rule = new Rule();
            rule.setName("testDeleteScript" + i);
            rule.setActive(true);
            rule.addAction(new Keep());
            rule.addAction(new Stop());
            rule.setTest(new TrueTest());

            int id = mailFilterAPI.createRule(rule);
            rememberRule(id);
            rule.setId(id);
            rule.setPosition(i);
            expectedRules.add(rule);
        }

        // Get and assert
        getAndAssert(expectedRules);
    }

    /**
     * Tests the auxiliary API call 'deletescript'
     */
    @Test
    public void testDeleteScript() throws Exception {
        // Delete the entire script
        mailFilterAPI.deleteScript();

        // Assert that the rules were deleted
        List<Rule> rules = mailFilterAPI.listRules();
        assertTrue(rules.isEmpty(), "The list of rules is not empty");
        forgetRules();
    }

    /**
     * Tests the auxiliary API call 'getscript'
     */
    @Test
    public void testGetScript() throws Exception {
        String script = mailFilterAPI.getScript();
        assertFalse(script.isEmpty(), "The script is empty");
    }
}
