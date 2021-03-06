/*
 * Copyright 2008-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.ebean.repository.query;

import io.ebean.Query;
import io.ebean.SqlQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.*;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * {@link ParameterBinder} is used to bind method parameters to a {@link Query}. This is usually done whenever an
 * {@link AbstractEbeanQuery} is executed.
 *
 * @author Xuegui Yuan
 */
public class ParameterBinder {

    private final DefaultParameters parameters;
    private final ParameterAccessor accessor;
    private final Object[] values;

    /**
     * Creates a new {@link ParameterBinder}.
     *
     * @param parameters must not be {@literal null}.
     * @param values     must not be {@literal null}.
     */
    public ParameterBinder(DefaultParameters parameters, Object[] values) {

        Assert.notNull(parameters, "Parameters must not be null!");
        Assert.notNull(values, "Values must not be null!");

        Assert.isTrue(parameters.getNumberOfParameters() == values.length, "Invalid number of parameters given!");

        this.parameters = parameters;
        this.values = values.clone();
        this.accessor = new ParametersParameterAccessor(parameters, this.values);
    }

    ParameterBinder(DefaultParameters parameters) {
        this(parameters, new Object[0]);
    }

    /**
     * Returns the {@link Pageable} of the parameters, if available. Returns {@code null} otherwise.
     *
     * @return
     */
    public Pageable getPageable() {
        return accessor.getPageable();
    }

    /**
     * Returns the sort instance to be used for query creation. Will use a {@link Sort} parameter if available or the
     * {@link Sort} contained in a {@link Pageable} if available. Returns {@code null} if no {@link Sort} can be found.
     *
     * @return
     */
    public Sort getSort() {
        return accessor.getSort();
    }

    /**
     * Binds the parameters to the given {@link Query}.
     *
     * @param query must not be {@literal null}.
     * @return
     */
    public Object bind(Object query) {

        Assert.notNull(query, "EbeanQuery must not be null!");

        int bindableParameterIndex = 0;
        int queryParameterPosition = 1;

        for (Parameter parameter : parameters) {

            if (canBindParameter(parameter)) {

                Object value = accessor.getBindableValue(bindableParameterIndex);
                bind(query, parameter, value, queryParameterPosition++);
                bindableParameterIndex++;
            }
        }

        return query;
    }

    /**
     * Returns {@literal true} if the given parameter can be bound.
     *
     * @param parameter
     * @return
     */
    protected boolean canBindParameter(Parameter parameter) {
        return parameter.isBindable();
    }

    /**
     * Perform the actual query parameter binding.
     *
     * @param query
     * @param parameter
     * @param value
     * @param position
     */
    protected void bind(Object query, Parameter parameter, Object value, int position) {
        if (parameter.isNamedParameter()) {
            if (query instanceof Query) {
                Query ormQuery = (Query) query;
                ormQuery.setParameter(
                        Optional.ofNullable(parameter.getName()).orElseThrow(() -> new IllegalArgumentException("o_O paraneter needs to have a name!")),
                        value);
            } else if (query instanceof SqlQuery) {
                SqlQuery sqlQuery = (SqlQuery) query;
                sqlQuery.setParameter(
                        Optional.ofNullable(parameter.getName()).orElseThrow(() -> new IllegalArgumentException("o_O paraneter needs to have a name!")),
                        value);
            } else {
                throw new IllegalArgumentException("query must be Query or SqlQuery!");
            }
        } else {
            if (query instanceof Query) {
                Query ormQuery = (Query) query;
                ormQuery.setParameter(position, value);
            } else if (query instanceof SqlQuery) {
                SqlQuery sqlQuery = (SqlQuery) query;
                sqlQuery.setParameter(position, value);
            } else {
                throw new IllegalArgumentException("query must be Query or SqlQuery!");
            }
        }
    }

    /**
     * Binds the parameters to the given query and applies special parameter types (e.g. pagination).
     *
     * @param query must not be {@literal null}.
     * @return
     */
    public Object bindAndPrepare(Object query) {
        Assert.notNull(query, "query must not be null!");
        return bindAndPrepare(query, parameters);
    }

    private Object bindAndPrepare(Object query, Parameters<?, ?> parameters) {

        Object result = bind(query);

        if (!parameters.hasPageableParameter()) {
            return result;
        }
        if (query instanceof Query) {
            Query ormQuery = (Query) query;
            ormQuery.setFirstRow((int) getPageable().getOffset());
            ormQuery.setMaxRows(getPageable().getPageSize());
        } else if (query instanceof SqlQuery) {
            SqlQuery sqlQuery = (SqlQuery) query;
            sqlQuery.setFirstRow((int) getPageable().getOffset());
            sqlQuery.setMaxRows(getPageable().getPageSize());
        }

        return result;
    }

    /**
     * Returns the parameters.
     *
     * @return
     */
    Parameters getParameters() {
        return parameters;
    }

    protected Object[] getValues() {
        return values;
    }
}
