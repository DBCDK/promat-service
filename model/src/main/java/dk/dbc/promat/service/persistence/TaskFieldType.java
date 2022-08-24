/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import java.util.function.Function;

@SuppressWarnings("SpellCheckingInspection")
public enum TaskFieldType {
    BRIEF(PayCategory.BRIEF, false),
    DESCRIPTION,
    EVALUATION,
    COMPARISON,
    RECOMMENDATION,
    @Deprecated
    BIBLIOGRAPHIC, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    TOPICS(true),
    BKM(PayCategory.BKM, true),
    EXPRESS(PayCategory.EXPRESS, false),
    METAKOMPAS(PayCategory.METAKOMPAS, true),
    @Deprecated
    GENRE, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    AGE(true),
    MATLEVEL(true),
    BUGGI(PayCategory.BUGGI, true);

    private final Function<TaskType, PayCategory> payment;
    public final boolean onceOnlyPerCase;

    TaskFieldType(PayCategory paymentCategory, boolean onceOnlyPerCase) {
        this.payment = t -> paymentCategory;
        this.onceOnlyPerCase = onceOnlyPerCase;
    }

    TaskFieldType() {
        this(false);
    }

    TaskFieldType(boolean onceOnlyPerCase) {
        payment = t -> t.payCategory;
        this.onceOnlyPerCase = onceOnlyPerCase;
    }

    public PayCategory getPaymentCategory(TaskType taskType) {
        return payment.apply(taskType);
    }
}
