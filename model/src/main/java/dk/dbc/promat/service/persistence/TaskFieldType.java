/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import java.util.function.Function;

@SuppressWarnings("SpellCheckingInspection")
public enum TaskFieldType {
    BRIEF(PayCategory.BRIEF, false, false),
    DESCRIPTION,
    EVALUATION,
    COMPARISON,
    RECOMMENDATION,
    @Deprecated
    BIBLIOGRAPHIC, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    TOPICS(false),
    BKM(PayCategory.BKM, true, false),
    EXPRESS(PayCategory.EXPRESS, false, false),
    METAKOMPAS(PayCategory.METAKOMPAS, false, true),
    @Deprecated
    GENRE, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    AGE(true),
    MATLEVEL(true),
    BUGGI(PayCategory.BUGGI, false, true);

    private final Function<TaskType, PayCategory> payment;
    public final boolean onceOnlyPerCase;
    public final boolean externalTask;

    TaskFieldType(PayCategory paymentCategory, boolean onceOnlyPerCase, boolean externalTask) {
        this.payment = t -> paymentCategory;
        this.onceOnlyPerCase = onceOnlyPerCase;
        this.externalTask = externalTask;
    }

    TaskFieldType() {
        this(false);
    }

    TaskFieldType(boolean onceOnlyPerCase) {
        payment = t -> t.payCategory;
        this.onceOnlyPerCase = onceOnlyPerCase;
        this.externalTask = false;
    }

    public PayCategory getPaymentCategory(TaskType taskType) {
        return payment.apply(taskType);
    }
}
