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
    BUGGI(PayCategory.BUGGI, false, false),
    // Reading experience tasks done in Promat instead of Metakompasset: ADULT is the Metakompas selection
    // (MARC 665), CHILD the Buggi selection (664). Unlike internal tasks, they aren't approved when the case
    // is approved - each is approved on its own.
    // METAKOMPAS/BUGGI above are the same tasks done in Metakompasset, kept while such tasks exist, and will
    // be deprecated later.
    READING_EXPERIENCE_ADULT(PayCategory.METAKOMPAS, false, false),
    READING_EXPERIENCE_CHILD(PayCategory.BUGGI, false, false);

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
