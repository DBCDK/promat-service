package dk.dbc.promat.service.persistence;

public enum PayCategory {
    NONE("0000"),

    BRIEF("3216"),

    METAKOMPAS("3229"),

    BUGGI("3230"),

    BKM("3218"),

    GROUP_1_LESS_THAN_100_PAGES("3212"),

    GROUP_2_100_UPTO_199_PAGES("3213"),

    GROUP_3_200_UPTO_499_PAGES("3214"),

    GROUP_4_500_OR_MORE_PAGES("3215"),

    MOVIES_GR_1("3222"),

    MOVIES_GR_2("3223"),

    MOVIES_GR_3("3224"),

    MULTIMEDIA_FEE("3210"),

    MULTIMEDIA_FEE_GR2("3227"),

    MOVIE_NON_FICTION_GR1("3221"),

    MOVIE_NON_FICTION_GR2("3225"),

    MOVIE_NON_FICTION_GR3("3226"),

    // To be ignored in payments, task with this category does not add extra payments
    EXPRESS("0000");

    private String payCategory;

    PayCategory(String category) {
        this.payCategory = category;
    }

    public String value() {
        return payCategory;
    }
}
