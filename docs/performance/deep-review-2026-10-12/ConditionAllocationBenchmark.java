package com.landawn.abacus.query.condition;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class ConditionAllocationBenchmark {
    private static volatile Object sink;
    private static final Object[] SCALARS = { null, "literal", 123, Boolean.TRUE };
    private static final Object[] NESTED = { new byte[] { 1, 2 }, new Object[] { "nested" } };

    public static void main(final String[] args) {
        final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        final long threadId = Thread.currentThread().threadId();
        final List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            values.add(i);
        }
        final Binary membership = new Binary("id", Operator.IN, values);
        for (int i = 0; i < 100_000; i++) {
            sink = AbstractCondition.snapshotMutableValue((i & 15) == 0 ? NESTED : SCALARS[i & 3]);
        }
        for (int i = 0; i < 10_000; i++) {
            sink = membership.propValue();
        }

        final int scalarIterations = 500_000;
        final long scalarBefore = bean.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < scalarIterations; i++) {
            sink = AbstractCondition.snapshotMutableValue(SCALARS[i & 3]);
        }
        final long scalarAfter = bean.getThreadAllocatedBytes(threadId);
        final int membershipIterations = 20_000;
        final long membershipBefore = bean.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < membershipIterations; i++) {
            sink = membership.propValue();
        }
        final long membershipAfter = bean.getThreadAllocatedBytes(threadId);
        System.out.printf("scalar snapshot: %.1f bytes/call%n", (scalarAfter - scalarBefore) / (double) scalarIterations);
        System.out.printf("32-value membership exposure: %.1f bytes/call%n", (membershipAfter - membershipBefore) / (double) membershipIterations);
    }
}
