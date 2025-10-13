/*
 * Copyright (C) 2025 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Clause2025Test extends TestBase {

    private static class TestClause extends Clause {
        public TestClause(Operator operator, Condition condition) {
            super(operator, condition);
        }
    }

    @Test
    public void testConstructor() {
        Condition condition = ConditionFactory.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);
        assertNotNull(clause);
        assertNotNull(clause.getOperator());
        assertNotNull(clause.getCondition());
    }

    @Test
    public void testAnd_ThrowsException() {
        Condition condition = ConditionFactory.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);
        Condition otherCondition = ConditionFactory.eq("other", "value2");

        assertThrows(UnsupportedOperationException.class, () -> {
            clause.and(otherCondition);
        });
    }

    @Test
    public void testOr_ThrowsException() {
        Condition condition = ConditionFactory.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);
        Condition otherCondition = ConditionFactory.eq("other", "value2");

        assertThrows(UnsupportedOperationException.class, () -> {
            clause.or(otherCondition);
        });
    }

    @Test
    public void testNot_ThrowsException() {
        Condition condition = ConditionFactory.eq("test", "value");
        TestClause clause = new TestClause(Operator.WHERE, condition);

        assertThrows(UnsupportedOperationException.class, () -> {
            clause.not();
        });
    }
}
