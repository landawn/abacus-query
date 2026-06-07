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

import com.landawn.abacus.util.NamingPolicy;

/**
 * Immutable holder pairing a {@link NamingPolicy} with the {@code toString} rendering produced for it.
 *
 * <p>Used as a single-slot memoization cell by the {@code toString(NamingPolicy)} methods throughout the
 * condition hierarchy. Capturing the policy and its rendered value together in one immutable object that is
 * published through a single {@code volatile} reference makes the cache thread-safe: a concurrent reader can
 * only ever observe either {@code null} (and recompute) or a fully-initialized holder whose value matches its
 * policy. With the previous two-field design, two threads rendering the same shared instance under different
 * naming policies could interleave the two writes so that a reader saw one field's policy paired with the
 * other field's value, returning a string rendered for the wrong policy.</p>
 *
 * <p>This matters in practice because {@link Expression} instances are interned and shared across threads via
 * {@link Expression#of(String)}, so the same instance is routinely rendered with different naming policies
 * concurrently.</p>
 */
final class CachedToString {

    final NamingPolicy namingPolicy;

    final String value;

    CachedToString(final NamingPolicy namingPolicy, final String value) {
        this.namingPolicy = namingPolicy;
        this.value = value;
    }
}
