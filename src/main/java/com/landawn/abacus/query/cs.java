/*
 * Copyright (c) 2026, Haiyang Li.
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

package com.landawn.abacus.query;

/**
 * Internal constants container providing the standardized parameter-name string literals
 * used by argument-validation calls (e.g., {@code N.checkArgNotNull(condition, cs.condition)})
 * throughout the Abacus Query framework.
 *
 * <p>This utility class centralizes the textual identifiers that appear in error messages
 * and diagnostics, ensuring that the parameter name reported when a validation fails
 * matches the actual method-parameter name in the source code, and reducing the risk
 * of typos when the same name is referenced across many call sites.</p>
 *
 * <p><b>Contract:</b> each constant's name and its string value are identical, and both must equal
 * the declared name of the method parameter it validates. Renaming a method parameter therefore
 * requires renaming the corresponding constant (and its value) in the same change, and vice versa,
 * so the three names never drift apart.</p>
 *
 * <p>The constants are primarily used in:</p>
 * <ul>
 *   <li>{@code N.checkArgNotNull / checkArgNotEmpty / checkArgNotBlank / checkArgNotNegative} validation
 *       calls inside framework methods</li>
 *   <li>Argument labels for exception messages thrown by framework methods</li>
 * </ul>
 *
 * <p><b>Usage Example (framework-internal):</b></p>
 * <pre>{@code
 * public static Not not(final Condition condition) {
 *     N.checkArgNotNull(condition, cs.condition);
 *     // ...
 * }
 * }</pre>
 */
public final class cs { // NOSONAR
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private cs() {
        // utility class - prevent instantiation
    }

    /**
     * Parameter name for an additional SQL mapper file path to load beyond the first one.
     */
    public static final String additionalFilePath = "additionalFilePath";

    /**
     * Parameter name for the array of additional SQL mapper file paths to load beyond the first one.
     */
    public static final String additionalFilePaths = "additionalFilePaths";

    /**
     * Parameter name for the collection of database column names.
     */
    public static final String columnNames = "columnNames";

    /**
     * Parameter name for a condition object used in query filtering.
     */
    public static final String condition = "condition";

    /**
     * Parameter name for the collection or array of condition objects to be combined.
     */
    public static final String conditions = "conditions";

    /**
     * Parameter name for a Consumer callback applied to the builder.
     */
    public static final String consumer = "consumer";

    /**
     * Parameter name for the maximum number of rows to return.
     */
    public static final String count = "count";

    /**
     * Parameter name for the sort direction applied to an ORDER BY or GROUP BY element.
     */
    public static final String direction = "direction";

    /**
     * Parameter name for the collection of entity objects to be persisted.
     */
    public static final String entities = "entities";

    /**
     * Parameter name for the collection of entity objects or property maps to be persisted.
     */
    public static final String entitiesOrPropMaps = "entitiesOrPropMaps";

    /**
     * Parameter name for a single entity object or property map.
     */
    public static final String entity = "entity";

    /**
     * Parameter name for the entity class whose mapping metadata is used.
     */
    public static final String entityClass = "entityClass";

    /**
     * Parameter name for a single entity identifier.
     */
    public static final String entityId = "entityId";

    /**
     * Parameter name for the collection of entity identifiers.
     */
    public static final String entityIds = "entityIds";

    /**
     * Parameter name for a raw SQL expression fragment.
     */
    public static final String expr = "expr";

    /**
     * Parameter name for a file to read from or write to.
     */
    public static final String file = "file";

    /**
     * Parameter name for a single SQL mapper file path.
     */
    public static final String filePath = "filePath";

    /**
     * Parameter name for the collection of SQL mapper file paths.
     */
    public static final String filePaths = "filePaths";

    /**
     * Parameter name for the collection of files to read from.
     */
    public static final String files = "files";

    /**
     * Parameter name for the first SQL mapper file path in a multi-file load.
     */
    public static final String firstFilePath = "firstFilePath";

    /**
     * Parameter name for a Function callback applied to the builder.
     */
    public static final String function = "function";

    /**
     * Parameter name for the array of property or column names to group by.
     */
    public static final String groupings = "groupings";

    /**
     * Parameter name for the SQL identifier registered in a SQL mapper.
     */
    public static final String id = "id";

    /**
     * Parameter name for the collection of property names to include.
     */
    public static final String includedPropNames = "includedPropNames";

    /**
     * Parameter name for an input stream to read from.
     */
    public static final String inputStream = "inputStream";

    /**
     * Parameter name for the collection of entity classes or names to join.
     */
    public static final String joinEntities = "joinEntities";

    /**
     * Parameter name for a LIMIT condition.
     */
    public static final String limit = "limit";

    /**
     * Parameter name for the upper bound of a BETWEEN range.
     */
    public static final String maxValue = "maxValue";

    /**
     * Parameter name for the lower bound of a BETWEEN range.
     */
    public static final String minValue = "minValue";

    /**
     * Parameter name for the list of Selection descriptors for a multi-entity select.
     */
    public static final String multiSelects = "multiSelects";

    /**
     * Parameter name for the number of rows to skip before returning results.
     */
    public static final String offset = "offset";

    /**
     * Parameter name for the array of property or column names to order by.
     */
    public static final String orders = "orders";

    /**
     * Parameter name for an output stream to write to.
     */
    public static final String outputStream = "outputStream";

    /**
     * Parameter name for the collection or array of SQL parameter values.
     */
    public static final String parameters = "parameters";

    /**
     * Parameter name for the number of parameter placeholders to append.
     */
    public static final String placeholderCount = "placeholderCount";

    /**
     * Parameter name for the text appended after the generated SQL fragment.
     */
    public static final String postfix = "postfix";

    /**
     * Parameter name for the text prepended before the generated SQL fragment.
     */
    public static final String prefix = "prefix";

    /**
     * Parameter name for the property metadata descriptor.
     */
    public static final String propInfo = "propInfo";

    /**
     * Parameter name for the collection or array of property names.
     */
    public static final String propNames = "propNames";

    /**
     * Parameter name for the value compared against a property.
     */
    public static final String propValue = "propValue";

    /**
     * Parameter name for the map of property names to values.
     */
    public static final String props = "props";

    /**
     * Parameter name for a SQL query string.
     */
    public static final String query = "query";

    /**
     * Parameter name for a single Selection descriptor.
     */
    public static final String selection = "selection";

    /**
     * Parameter name for the collection of Selection descriptors.
     */
    public static final String selections = "selections";

    /**
     * Parameter name for the separator character or string.
     */
    public static final String separator = "separator";

    /**
     * Parameter name for a SQL statement string.
     */
    public static final String sql = "sql";

    /**
     * Parameter name for another SQL builder whose generated SQL is embedded.
     */
    public static final String sqlBuilder = "sqlBuilder";

    /**
     * Parameter name for the SQL dialect that controls identifier quoting and pagination syntax.
     */
    public static final String sqlDialect = "sqlDialect";

    /**
     * Parameter name for a sub-query embedded in the generated SQL.
     */
    public static final String subQuery = "subQuery";

    /**
     * Parameter name for the collection or array of table names.
     */
    public static final String tableNames = "tableNames";

    /**
     * Parameter name for the tokenizer configuration used while splitting SQL.
     */
    public static final String tokenizerConfig = "tokenizerConfig";

    /**
     * Parameter name for the collection of value rows for a multi-column IN clause.
     */
    public static final String valueRows = "valueRows";

    /**
     * Parameter name for the collection or array of values.
     */
    public static final String values = "values";

    /**
     * Parameter name for the target type values are converted to.
     */
    public static final String valueType = "valueType";
}
