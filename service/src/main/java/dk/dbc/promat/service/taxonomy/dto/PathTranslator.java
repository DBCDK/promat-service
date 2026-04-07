package dk.dbc.promat.service.taxonomy.dto;

import java.util.Arrays;
import java.util.List;

/**
 * Enumeration for translating subject taxonomy codes into hierarchical path structures.
 *
 * This translator maps single-character or short codes to categorized path descriptions
 * used in the literary subject taxonomy system. Each enum constant represents a specific
 * aspect of literary analysis and categorization, organized into four main categories:
 * setting/frame (ramme), plot/action (handling), narrative technique (fortælleteknik),
 * and mood/atmosphere (stemning).
 *
 * The path values are stored as a list of strings, where the first element typically
 * represents the main category and subsequent elements provide more specific
 * subcategories or descriptions.
 *
 * Categories include:
 * - Setting elements: time expressions (numeric (I1) and textual (I0)), geographic locations,
 *   fictional places, environment, genre, and universe
 * - Plot elements: subject matter, named protagonists, protagonist descriptions
 *   including characteristics and conflicts
 * - Narrative technique: writing style and structure, narrator voice, and pacing
 * - Mood: various atmospheric qualities ranging from positive to thought-provoking,
 *   including humorous, romantic, erotic, dramatic, sad, spooky, imaginative, and
 *   thought-provoking moods
 *
 * This enumeration is designed to work with subject taxonomy system as they are packed in the
 * marc records, where codes need to be translated from marc-record oriented shortcuts into human-readable
 * categorical paths.
 */
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
