/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import java.util.function.Function;

@SuppressWarnings("SpellCheckingInspection")
public enum TaskFieldType {
    BRIEF(PayCategory.BRIEF),
    DESCRIPTION,
    EVALUATION,
    COMPARISON,
    RECOMMENDATION,
    @Deprecated
    BIBLIOGRAPHIC, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    TOPICS,
    BKM(PayCategory.BKM),
    EXPRESS(PayCategory.EXPRESS),
    METAKOMPAS(PayCategory.METAKOMPAS),
    @Deprecated
    GENRE, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    AGE,
    MATLEVEL,
    BUGGI(PayCategory.BUGGI);

    private final Function<TaskType, PayCategory> payment;

    TaskFieldType(PayCategory paymentCategory) {
        this.payment = t -> paymentCategory;
    }

    TaskFieldType() {
        payment = t -> t.payCategory;
    }

    public PayCategory getPaymentCategory(TaskType taskType) {
        return payment.apply(taskType);
    }
}
