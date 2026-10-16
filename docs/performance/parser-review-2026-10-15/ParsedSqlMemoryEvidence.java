package com.landawn.abacus.query;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

/** Small structural evidence probe; not a benchmark or production change. */
public final class ParsedSqlMemoryEvidence {
    public static void main(String[] args) {
        try {
            inspect();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        // ParsedSql initializes a shared pool whose background threads can outlive this probe.
        System.exit(0);
    }

    private static void inspect() throws Exception {
        final Field value = String.class.getDeclaredField("value");
        value.setAccessible(true);
        for (String sql : List.of("SELECT * FROM t WHERE id = ?", "SELECT " + "x".repeat(4000),
                "CREATE TABLE t (id INT)", "SELECT * FROM t WHERE id = :id")) {
            ParsedSql parsed = ParsedSql.parse(sql, null);
            System.out.printf("text length=%d operation=%s count=%d equal=%s sameString=%s sameBytes=%s originalBytes=%d renderedBytes=%d%n",
                    sql.length(), parsed.isDataOperation(), parsed.parameterCount(), parsed.originalSql().equals(parsed.parameterizedSql()),
                    parsed.originalSql() == parsed.parameterizedSql(), value.get(parsed.originalSql()) == value.get(parsed.parameterizedSql()),
                    ((byte[]) value.get(parsed.originalSql())).length, ((byte[]) value.get(parsed.parameterizedSql())).length);
        }

        for (int occurrences : new int[] { 1, 2, 9, 10, 11, 1001 }) {
            String sql = "SELECT " + ":id,".repeat(occurrences - 1) + ":id";
            ParsedSql parsed = ParsedSql.parse(sql, null);
            if (parsed.parameterCount() != occurrences) {
                throw new AssertionError("Unexpected binding count");
            }
            IdentityHashMap<String, Boolean> names = new IdentityHashMap<>();
            IdentityHashMap<Object, Boolean> payloads = new IdentityHashMap<>();
            for (String name : parsed.namedParameters()) {
                if (!name.equals("id")) {
                    throw new AssertionError("Unexpected name");
                }
                names.put(name, true);
                payloads.put(value.get(name), true);
            }
            ArrayList<?> backing = findArrayListBacking(parsed.namedParameters());
            Field elementData = ArrayList.class.getDeclaredField("elementData");
            elementData.setAccessible(true);
            System.out.printf("names sqlLength=%d count=%d distinctStringIdentities=%d distinctByteArrays=%d backingCapacity=%d%n",
                    sql.length(), occurrences, names.size(), payloads.size(), ((Object[]) elementData.get(backing)).length);
        }
    }

    private static ArrayList<?> findArrayListBacking(Object backing) throws ReflectiveOperationException {
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        for (int depth = 0; depth < 16; depth++) {
            if (backing instanceof ArrayList<?> list) {
                return list;
            }
            if (backing == null || seen.put(backing, true) != null) {
                throw new AssertionError("Null or cyclic collection backing at depth " + depth);
            }
            Field field = findCollectionField(backing.getClass());
            field.setAccessible(true);
            backing = field.get(backing);
        }
        throw new AssertionError("Collection backing exceeds 16 wrapper levels");
    }

    private static Field findCollectionField(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                // ImmutableList.EMPTY is static and itself implements List; it is not this instance's backing.
                // ImmutableList.list and inherited ImmutableCollection.coll are instance backing references.
                if (!Modifier.isStatic(field.getModifiers()) && Collection.class.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
        }
        throw new AssertionError("Cannot find collection backing for " + type);
    }
}
