package dk.dbc.promat.service.persistence;

public enum PayCategory {

    BRIEF("1960"),

    METAKOMPAS("1987"),

    BKM("1962"),

    GROUP_1_LESS_THAN_100_PAGES("1956"),

    GROUP_2_100_UPTO_199_PAGES("1957"),

    GROUP_3_200_UPTO_499_PAGES("1958"),

    GROUP_4_500_OR_MORE_PAGES("1959"),

    MOVIES_GR_1("1980"),

    MOVIES_GR_2("1981"),

    MOVIES_GR_3("1982"),

    MULTIMEDIA_FEE("1954"),

    MULTIMEDIA_FEE_GR2("1985"),

    MOVIE_NON_FICTION_GR1("1979"),

    MOVIE_NON_FICTION_GR2("1983"),

    MOVIE_NON_FICTION_GR3("1984");

    private String payCategory;

    PayCategory(String category) {
        this.payCategory = category;
    }

    public String value() {
        return payCategory;
    }
}
