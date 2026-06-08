package com.landawn.abacus.query;

import com.landawn.abacus.query.AbstractQueryBuilder.SQLPolicy;
import com.landawn.abacus.util.NamingPolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Configuration that defines a SQL "dialect" for a {@link SqlBuilder.Factory}: the {@link NamingPolicy}
 * used to translate property names into column names, and the {@link SQLPolicy} that controls the
 * parameter style (raw literals, positional {@code ?}, named {@code :name}, or iBATIS {@code #{name}}).
 *
 * <p>Each predefined {@link SqlBuilder} constant (e.g. {@link SqlBuilder#PSC}, {@link SqlBuilder#NSC})
 * is backed by a distinct {@code SqlDialect}. Instances are typically created with the generated
 * {@code builder()}, for example
 * {@code SqlDialect.builder().namingPolicy(NamingPolicy.SNAKE_CASE).sqlPolicy(SQLPolicy.PARAMETERIZED_SQL).build()}.</p>
 */
@Builder
@Data
@AllArgsConstructor
@Accessors(fluent = true)
public class SqlDialect {
    private NamingPolicy namingPolicy;

    private SQLPolicy sqlPolicy;
}
