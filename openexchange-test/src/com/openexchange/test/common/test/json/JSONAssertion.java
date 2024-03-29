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

package com.openexchange.test.common.test.json;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;

/**
 * {@link JSONAssertion}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class JSONAssertion implements JSONCondition {

    public static final void assertValidates(final JSONAssertion assertion, final Object o) {
        assertNotNull(o, "Object was null");
        if (!assertion.validate(o)) {
            fail(assertion.getComplaint());
        }
    }

    /**
     * Checks for equality of specified JSON values.
     * 
     * @param jsonValue1 The first JSON value
     * @param jsonValue2 The second JSON value
     * @return <code>true</code> if equal; otherwise <code>false</code>
     */
    public static boolean equals(final JSONValue jsonValue1, final JSONValue jsonValue2) {
        if (jsonValue1 == jsonValue2) {
            return true;
        }
        if (null == jsonValue1) {
            if (null != jsonValue2) {
                return false;
            }
            return true; // Both null
        }
        if (null == jsonValue2) {
            return false;
        }
        if (jsonValue1.isArray()) {
            if (!jsonValue2.isArray()) {
                return false;
            }
            return getListFrom(jsonValue1.toArray()).equals(getListFrom(jsonValue2.toArray()));
        }
        if (jsonValue1.isObject()) {
            if (!jsonValue2.isObject()) {
                return false;
            }
            return getMapFrom(jsonValue1.toObject()).equals(getMapFrom(jsonValue2.toObject()));
        }
        return false;
    }

    /**
     * Checks for equality of specified JSON datas.
     * 
     * @param jsonData1 The first JSON data
     * @param jsonData2 The second JSON data
     * @return <code>true</code> if equal; otherwise <code>false</code>
     */
    public static boolean equals(final Object jsonData1, final Object jsonData2) {
        if (jsonData1 == jsonData2) {
            return true;
        }
        if (null == jsonData1) {
            if (null != jsonData2) {
                return false;
            }
            return true; // Both null
        }
        if (null == jsonData2) {
            return false;
        }
        if (!jsonData1.getClass().equals(jsonData2.getClass())) {
            return false;
        }
        return getFrom(jsonData2).equals(getFrom(jsonData2));
    }

    private static List<Object> getListFrom(final JSONArray jsonArray) {
        final int length = jsonArray.length();
        final List<Object> list = new ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            try {
                list.add(getFrom(jsonArray.get(i)));
            } catch (JSONException e) {
                // Ignore
            }
        }
        return list;
    }

    private static Map<String, Object> getMapFrom(final JSONObject jsonObject) {
        final int length = jsonObject.length();
        final Map<String, Object> map = new HashMap<String, Object>(length);
        for (final Entry<String, Object> entry : jsonObject.entrySet()) {
            map.put(entry.getKey(), getFrom(entry.getValue()));
        }
        return map;
    }

    private static Object getFrom(final Object object) {
        if (object instanceof JSONArray) {
            return getListFrom((JSONArray) object);
        }
        if (object instanceof JSONObject) {
            return getMapFrom((JSONObject) object);
        }
        return object;
    }

    private final Stack<JSONAssertion> stack = new Stack<JSONAssertion>();

    private final List<JSONCondition> conditions = new LinkedList<JSONCondition>();
    private String key;

    private String complaint;

    public JSONAssertion isObject() {
        if (!stack.isEmpty()) {
            stack.peek().isObject();
        } else {
            conditions.add(new IsOfType(JSONObject.class));
        }
        return this;
    }

    public JSONAssertion hasKey(final String key) {
        if (!stack.isEmpty()) {
            stack.peek().hasKey(key);
        } else {
            conditions.add(new HasKey(key));
            this.key = key;
        }
        return this;
    }

    public JSONAssertion lacksKey(final String key) {
        if (!stack.isEmpty()) {
            stack.peek().lacksKey(key);
        } else {
            conditions.add(new LacksKey(key));
            this.key = key;
        }
        return this;
    }

    public JSONAssertion withValue(final Object value) {
        if (!stack.isEmpty()) {
            stack.peek().withValue(value);
        } else {
            conditions.add(new KeyValuePair(key, value));
        }
        return this;
    }

    public JSONAssertion withValueObject() {
        final JSONAssertion stackElement = new JSONAssertion();
        conditions.add(new ValueObject(key, stackElement));
        stackElement.isObject();
        stack.push(stackElement);
        return this;
    }

    public JSONAssertion withValueArray() {
        final JSONAssertion stackElement = new JSONAssertion();
        conditions.add(new ValueArray(key, stackElement));
        stackElement.isArray();
        stack.push(stackElement);
        return this;
    }

    public JSONAssertion atIndex(final int i) {
        if (!stack.isEmpty()) {
            stack.peek().atIndex(i);
            return this;
        }
        conditions.add(new HasIndex(i));
        return null;
    }

    public JSONAssertion hasNoMoreKeys() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
        return this;
    }

    public JSONAssertion isArray() {
        conditions.add(new IsOfType(JSONArray.class));
        return this;
    }

    public JSONAssertion withValues(final Object... values) {
        conditions.add(new WithValues(values));
        return this;
    }

    public JSONAssertion objectEnds() {
        return hasNoMoreKeys();
    }

    @Override
    public boolean validate(final Object o) {
        for (final JSONCondition condition : conditions) {
            if (!condition.validate(o)) {
                complaint = condition.getComplaint();
                return false;
            }
        }
        return true;
    }

    @Override
    public String getComplaint() {
        return complaint;
    }

    private static final class IsOfType implements JSONCondition {

        private String complaint;
        private final Class<?> type;

        public IsOfType(final Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean validate(final Object o) {
            final boolean isCorrectType = type.isInstance(o);
            if (!isCorrectType) {
                complaint = "Expected " + type.getName() + " was: " + o.getClass().getName();
            }
            return isCorrectType;
        }

        @Override
        public String getComplaint() {
            return complaint;
        }
    }

    private static final class HasKey implements JSONCondition {

        private final String key;

        public HasKey(final String key) {
            this.key = key;
        }

        @Override
        public boolean validate(final Object o) {
            return ((JSONObject) o).has(key);
        }

        @Override
        public String getComplaint() {
            return "Missing key: " + key;
        }

    }

    private static final class LacksKey implements JSONCondition {

        private final String key;

        public LacksKey(final String key) {
            this.key = key;
        }

        @Override
        public boolean validate(final Object o) {
            return !((JSONObject) o).has(key);
        }

        @Override
        public String getComplaint() {
            return "Key should be missing: " + key;
        }

    }

    private static final class HasIndex implements JSONCondition {

        private final int index;

        public HasIndex(final int index) {
            this.index = index;
        }

        @Override
        public boolean validate(final Object o) {
            return ((JSONArray) o).length() > index;
        }

        @Override
        public String getComplaint() {
            return "Missing index: " + index;
        }

    }

    private static final class KeyValuePair implements JSONCondition {

        private final String key;
        private final Object value;
        private String complaint;

        public KeyValuePair(final String key, final Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean validate(final Object o) {
            try {
                final Object object = ((JSONObject) o).get(key);
                if (!object.equals(value)) {
                    complaint = "Expected value " + value + " for key " + key + " but got " + object;
                    return false;
                }
                return true;
            } catch (JSONException e) {
                return false;
            }
        }

        @Override
        public String getComplaint() {
            return complaint;
        }
    }

    private static final class ValueObject implements JSONCondition {

        private final String key;
        private final JSONAssertion assertion;

        public ValueObject(final String key, final JSONAssertion assertion) {
            this.key = key;
            this.assertion = assertion;
        }

        @Override
        public String getComplaint() {
            return assertion.getComplaint();
        }

        @Override
        public boolean validate(final Object o) {
            try {
                final Object subObject = ((JSONObject) o).get(key);
                return assertion.validate(subObject);
            } catch (JSONException x) {
                return false;
            }
        }

    }

    private static final class ValueArray implements JSONCondition {

        private final String key;
        private final JSONAssertion assertion;

        public ValueArray(final String key, final JSONAssertion assertion) {
            this.key = key;
            this.assertion = assertion;
        }

        @Override
        public String getComplaint() {
            return assertion.getComplaint();
        }

        @Override
        public boolean validate(final Object o) {
            try {
                final Object subObject = ((JSONObject) o).get(key);
                return assertion.validate(subObject);
            } catch (JSONException x) {
                return false;
            }
        }

    }

    private static final class WithValues implements JSONCondition {

        private final Object[] values;
        private String complaint;

        public WithValues(final Object[] values) {
            this.values = values;
        }

        @Override
        public boolean validate(final Object o) {
            final JSONArray arr = (JSONArray) o;
            if (arr.length() != values.length) {
                complaint = "Lengths differ: expected " + values.length + " was: " + arr.length();
                return false;
            }
            for (int i = 0; i < values.length; i++) {
                final Object expected = values[i];
                Object actual;
                try {
                    actual = arr.get(i);
                } catch (JSONException e) {
                    complaint = e.toString();
                    return false;
                }
                if (!expected.equals(actual)) {
                    complaint = "Expected " + expected + " got: " + actual + " at index " + i;
                    return false;
                }
            }
            return true;
        }

        @Override
        public String getComplaint() {
            return complaint;
        }
    }

}
