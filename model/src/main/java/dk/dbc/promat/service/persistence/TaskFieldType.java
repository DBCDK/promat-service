package dk.dbc.promat.service.persistence;

import java.util.function.Function;

@SuppressWarnings("SpellCheckingInspection")
public enum TaskFieldType {
    BRIEF(PayCategory.BRIEF, false, true),
    DESCRIPTION,
    EVALUATION,
    COMPARISON,
    RECOMMENDATION,
    @Deprecated
    BIBLIOGRAPHIC, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    TOPICS(false),
    BKM(PayCategory.BKM, true, true),
    EXPRESS(PayCategory.EXPRESS, false, true),
    METAKOMPAS(PayCategory.METAKOMPAS, false, false),
    @Deprecated
    GENRE, // Todo: Obsolete, remove when no tasks exists in the db with this taskfieldtype
    AGE(true),
    MATLEVEL(true),
    BUGGI(PayCategory.BUGGI, false, false);

    private final Function<TaskType, PayCategory> payment;
    public final boolean onceOnlyPerCase;
    public final boolean internalTask;

    TaskFieldType(PayCategory paymentCategory, boolean onceOnlyPerCase, boolean internalTask) {
        this.payment = t -> paymentCategory;
        this.onceOnlyPerCase = onceOnlyPerCase;
        this.internalTask = internalTask;
    }

    TaskFieldType() {
        this(false);
    }

    TaskFieldType(boolean onceOnlyPerCase) {
        payment = t -> t.payCategory;
        this.onceOnlyPerCase = onceOnlyPerCase;
        this.internalTask = true;
    }

    public PayCategory getPaymentCategory(TaskType taskType) {
        return payment.apply(taskType);
    }
}
