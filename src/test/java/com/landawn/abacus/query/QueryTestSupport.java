package com.landawn.abacus.query;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.query.AbstractQueryBuilder.SP;

/** Shared assertions and fixtures for query tests. */
public final class QueryTestSupport {

    private QueryTestSupport() {
    }

    static void assertSql(final String expectedSql, final List<?> expectedParameters, final SP actual) {
        assertAll(() -> assertEquals(expectedSql, actual.query(), "SQL"), () -> assertEquals(expectedParameters, actual.parameters(), "ordered parameters"));
    }

    static List<LogRecord> captureLogs(final Class<?> owner, final Runnable action) {
        final Logger logger = Logger.getLogger(owner.getName());
        final Level previousLevel = logger.getLevel();
        final boolean previousUseParentHandlers = logger.getUseParentHandlers();
        final List<LogRecord> records = new ArrayList<>();
        final Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.FINE);
        try {
            action.run();
        } finally {
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousUseParentHandlers);
            logger.removeHandler(handler);
        }
        return records;
    }

    static Selection selection(final Class<?> entityClass, final String tableAlias, final String classAlias, final Collection<String> includedPropNames,
            final boolean includeSubEntityProperties, final Set<String> excludedPropNames) {
        return Selection.builder(entityClass)
                .tableAlias(tableAlias)
                .classAlias(classAlias)
                .includedPropNames(includedPropNames)
                .includeSubEntityProperties(includeSubEntityProperties)
                .excludedPropNames(excludedPropNames)
                .build();
    }

    @Table(name = "user_order", alias = "o")
    public static class Order {
        private long id;
        private long userId;
        private String orderNumber;
        private BigDecimal amount;
        private Date orderDate;

        // Getters and setters
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }
    }
}
