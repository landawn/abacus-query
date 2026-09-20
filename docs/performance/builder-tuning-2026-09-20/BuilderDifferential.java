package com.landawn.abacus.query;

import com.landawn.abacus.query.AbstractQueryBuilder.SP;
import com.landawn.abacus.query.condition.*;
import com.landawn.abacus.util.NamingPolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Deterministic behavior corpus compared against the untouched starting implementation. */
public final class BuilderDifferential {
    private static final String[] NAMES = { "id", "firstName", "first_name", "id_2", "_firstName_", "trailing__", "u.id", "COUNT(*)",
            "a-b", "2faCode", "éclair", "猫名", "[a.b]", "\"a.b\"", "N'Case'", "COALESCE(firstName, 'N.A')", "a -- bad", "a/*bad*/", " ", "" };
    private static final Object[] VALUES = { 1, -1, Long.MIN_VALUE, Short.MAX_VALUE, (byte) -128, 1.25d, true, false, null,
            "O'Brien", "back\\slash", new java.math.BigDecimal("1.2300"), Filters.QME, SqlExpression.of("CURRENT_DATE") };
    private static String query(Dsl d, int index, String name, Object value) {
        SqlBuilder b = null;
        try {
            switch (index % 10) {
                case 0: b = d.select(name, "id"); b.from("users u").where(Filters.eq("id", value)); break;
                case 1: b = d.update("users"); b.set(name, value).where(Filters.eq("id", 2)); break;
                case 2: {
                    Map<String, Object> m = new LinkedHashMap<>(); m.put(name, value); m.put("otherName", 2);
                    b = d.insert(m); b.into("users"); break;
                }
                case 3: b = d.select("id").from("users"); b.where(Filters.between(name, value, 9)); break;
                case 4: {
                    List<Object> values = new ArrayList<>();
                    for (int i = 0; i < 17 + index % 19; i++) values.add(i);
                    values.set(index % values.size(), value);
                    b = d.select("id").from("users"); b.where(Filters.in(name, values)); break;
                }
                case 5: b = d.select("id").from("users"); b.where(Filters.or(Filters.eq(name, value), Filters.eq("id_2", 2))).orderBy("id").limit(4); break;
                case 6: {
                    b = d.update("users").set("id", 1);
                    Map<String, Object> invalid = new LinkedHashMap<>(); invalid.put("id", value); invalid.put("bad--column", 2);
                    try { b.set(invalid); } catch (RuntimeException expected) { }
                    b.set(name, value); break;
                }
                case 7: b = d.select("id").from("users"); b.where(Filters.in("id", new SubQuery("archive", "id", Filters.where(Filters.eq(name, value))))); break;
                case 8: b = d.renderCondition(Filters.and(Filters.eq(name, value), Filters.not(Filters.isNull("id")))); break;
                default: {
                    Map<String, Object> m = new LinkedHashMap<>(); m.put(name, value); m.put("id", 1);
                    b = d.batchInsert(Collections.nCopies(3, m)); b.into("users"); break;
                }
            }
            boolean placeholders = b._hasGeneratedParameterPlaceholder;
            SP result = b.build();
            return result.query() + "\n" + result.parameters() + "\n" + placeholders;
        } catch (RuntimeException e) {
            return e.getClass().getName() + ":" + e.getMessage();
        } finally {
            if (b != null && b._sb != null) {
                try { b.build(); } catch (RuntimeException ignored) { }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        java.util.logging.LogManager.getLogManager().reset();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int count = 0;
        for (NamingPolicy naming : NamingPolicy.values()) {
            for (SqlDialect.SqlPolicy policy : SqlDialect.SqlPolicy.values()) {
                Dsl d = Dsl.forDialect(SqlDialect.builder().namingPolicy(naming).sqlPolicy(policy).build());
                for (int i = 0; i < 1200; i++) {
                    String result = query(d, i, NAMES[(i / 10) % NAMES.length], VALUES[(i / 7) % VALUES.length]);
                    String line = (count++) + "\t" + Base64.getEncoder().encodeToString(result.getBytes(StandardCharsets.UTF_8));
                    digest.update((line + "\n").getBytes(StandardCharsets.UTF_8));
                    if (args.length > 0 && args[0].equals("verbose")) System.out.println(line);
                }
            }
        }
        Random random = new Random(9656);
        String alphabet = "abcXYZ012_ .()!é猫\t\n'-";
        for (int i = 0; i < 20000; i++) {
            StringBuilder text = new StringBuilder();
            for (int n = random.nextInt(40); n > 0; n--) text.append(alphabet.charAt(random.nextInt(alphabet.length())));
            String input = text.toString(), result = AbstractQueryBuilder.sanitizeNamedParameterName(input);
            String line = (count++) + "\t" + Base64.getEncoder().encodeToString((input + "\n" + result).getBytes(StandardCharsets.UTF_8));
            digest.update((line + "\n").getBytes(StandardCharsets.UTF_8));
            if (args.length > 0 && args[0].equals("verbose")) System.out.println(line);
        }
        String camelAlphabet = "abcyzABCXYZ012";
        for (NamingPolicy naming : NamingPolicy.values()) {
            for (int i = 0; i < 10000; i++) {
                StringBuilder word = new StringBuilder();
                for (int n = random.nextInt(25); n > 0; n--) word.append(camelAlphabet.charAt(random.nextInt(camelAlphabet.length())));
                String input = word.toString(), result = AbstractQueryBuilder.normalizeColumnName(input, naming);
                String line = (count++) + "\t" + Base64.getEncoder().encodeToString((input + "\n" + result).getBytes(StandardCharsets.UTF_8));
                digest.update((line + "\n").getBytes(StandardCharsets.UTF_8));
                if (args.length > 0 && args[0].equals("verbose")) System.out.println(line);
            }
        }
        if (AbstractQueryBuilder.activeStringBuilderCounter.get() != 0) throw new AssertionError("Builder resource leak: " + AbstractQueryBuilder.activeStringBuilderCounter);
        System.out.println("cases=" + count + " sha256=" + HexFormat.of().formatHex(digest.digest()));
        System.exit(0);
    }
}
