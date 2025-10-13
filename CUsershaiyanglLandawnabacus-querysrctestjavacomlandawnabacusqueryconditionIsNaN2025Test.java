/*
 * Copyright (c) 2025, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.query.condition;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NamingPolicy;

@Tag("2025")
public class IsNaN2025Test extends TestBase {
    @Test public void testConstructor() { IsNaN c = new IsNaN("score"); assertEquals("score", c.getPropName()); }
    @Test public void testToString() { assertTrue(new IsNaN("score").toString(NamingPolicy.NO_CHANGE).contains("IS NAN")); }
    @Test public void testEquals() { assertEquals(new IsNaN("score"), new IsNaN("score")); }
    @Test public void testHashCode() { assertEquals(new IsNaN("score").hashCode(), new IsNaN("score").hashCode()); }
    @Test public void testCopy() { IsNaN orig = new IsNaN("score"); assertNotNull(orig.copy()); }
    @Test public void testOperator() { assertEquals(Operator.IS, new IsNaN("score").getOperator()); }
}
