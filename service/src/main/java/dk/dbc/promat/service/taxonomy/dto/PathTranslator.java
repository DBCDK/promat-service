package dk.dbc.promat.service.taxonomy.dto;

import java.util.Arrays;
import java.util.List;

public enum PathTranslator {
    I0("ramme", "handlingens tid udtrykt i ord"),
    I1("ramme", "handlingens tid udtrykt i tal"),
    q("ramme", "geografisk sted"),
    p("ramme", "fiktivt sted"),
    m("ramme", "miljø"),
    g("ramme", "genre"),
    u("ramme", "univers"),
    e("handling", "handler om"),
    v("handling", "navngivet hovedperson"),
    h("handling", "hovedperson(er) - beskrivelse", "om hovedpersonen"),
    k("handling", "hovedperson(er) - beskrivelse", "hovedpersonens karaktertræk"),
    l("handling", "hovedperson(er) - beskrivelse", "hovedpersonens konflikt"),
    s("fortælleteknik", "skrivestil og struktur"),
    r("fortælleteknik", "fortællerstemme"),
    t("fortælleteknik", "tempo"),
    na("stemning", "positiv"),
    nb("stemning", "humoristisk"),
    nc("stemning", "romantisk"),
    nd("stemning", "erotisk"),
    ne("stemning", "dramatisk"),
    nf("stemning", "trist"),
    ng("stemning", "uhyggelig"),
    nh("stemning", "fantasifuld"),
    ni("stemning", "tankevækkende");

    private final List<String> pathValue;
    PathTranslator(String... path) {
        this.pathValue = Arrays.asList(path);
    }

    public List<String> getPathValue() {
        return pathValue;
    }
}
