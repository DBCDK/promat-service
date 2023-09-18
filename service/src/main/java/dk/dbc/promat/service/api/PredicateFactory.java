/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.CriteriaOperator;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public class PredicateFactory {
    private PredicateFactory() {}

    public static <Y extends Comparable<? super Y>>  Predicate fromBinaryOperator(CriteriaOperator criteriaOperator,
                                                                                  Expression<? extends Y> expression,
                                                                                  Y object,
                                                                                  CriteriaBuilder builder) {
        switch (criteriaOperator) {
            case EQUAL:
                return builder.equal(expression, object);
            case GREATER_THAN:
                return builder.greaterThan(expression, object);
            case GREATER_THAN_OR_EQUAL_TO:
                return builder.greaterThanOrEqualTo(expression, object);
            case LESS_THAN:
                return builder.lessThan(expression, object);
            case LESS_THAN_OR_EQUAL_TO:
                return builder.lessThanOrEqualTo(expression, object);
            case NOT_EQUAL:
                return builder.notEqual(expression, object);
            default:
                throw new IllegalStateException("Unknown binary operator: " + criteriaOperator);
        }
    }
}
