package com.landawn.abacus.query;

import java.lang.management.ManagementFactory;
import com.landawn.abacus.util.NamingPolicy;

public class QueryUtilityAllocationCheck {
    private static volatile Object sink;

    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();
        for (int segments : new int[] {4, 32, 256, 1024}) {
            String identifier = "\"part.with.dot\".firstName.".repeat(segments) + "[last.part]";
            for (int i = 0; i < 50; i++) {
                sink = QueryUtil.convertIdentifier(identifier, NamingPolicy.SNAKE_CASE);
            }
            long before = bean.getThreadAllocatedBytes(threadId);
            int iterations = 50;
            for (int i = 0; i < iterations; i++) {
                sink = QueryUtil.convertIdentifier(identifier, NamingPolicy.SNAKE_CASE);
            }
            long allocated = bean.getThreadAllocatedBytes(threadId) - before;
            System.out.println(segments + " pairs: " + allocated / iterations + " bytes/op");
        }
    }
}
