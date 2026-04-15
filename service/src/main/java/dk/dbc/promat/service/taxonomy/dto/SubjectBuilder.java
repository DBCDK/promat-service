package dk.dbc.promat.service.taxonomy.dto;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import dk.dbc.promat.service.taxonomy.TaxonomyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SubjectBuilder {
    private final Set<Integer> usedIds = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectBuilder.class);
    private static final String TAXONOMY_FIELD = "x09";
    private static final String IN_REVIEW_FIELD = "d09";
    private static final String SUBJECTVALUE_FIELD = "165";
    private static final String NOTES_FIELD = "680";
    public static final Pattern INTERVAL_PATTERN = Pattern.compile("\\d\\s*-\\s*\\d");

    public static <T> T notNullOrThrow(T object, String parameter) throws TaxonomyException {
        if (object == null) {
            throw new TaxonomyException(String.format("Parameter '%s' cannot be null", parameter));
        } else {
            return object;
        }
    }

    public List<PathSubject> build(MarcBinding marcBinding) throws TaxonomyException {
        if (isSubject(marcBinding)) {
                return subject(marcBinding);
        } else {
            return List.of();
        }
    }

    private boolean isSubject(MarcBinding marcBinding) {
        if (marcBinding == null) {
            return false;
        }
        return marcBinding.hasField(SUBJECTVALUE_FIELD) &&
                marcBinding.hasField(TAXONOMY_FIELD) &&
                marcBinding.getSubFieldValue(IN_REVIEW_FIELD, 'z') != null &&
                !marcBinding.getSubFieldValue(IN_REVIEW_FIELD, 'z').startsWith("EMK");
    }

    private List<PathSubject> subject(MarcBinding marcBinding) throws TaxonomyException {
        String faust = marcBinding.getSubFieldValue("001", 'a').trim();
        String title = notNullOrThrow(marcBinding.getSubFieldValue(SUBJECTVALUE_FIELD, 'a'), SUBJECTVALUE_FIELD + "a");
        List<PathSubject> pathSubjects = new ArrayList<>();

        // Multiple taxonomy fields can potentially exist.
        // Each must result in a taxonomy entry.
        marcBinding.getDataFields(TAXONOMY_FIELD).forEach(
                dataField -> {
                    try {
                        PathSubject pathSubject = new PathSubject();
                        pathSubject.withTitle(title);
                        pathSubject.withNote(notes(marcBinding));


                        // o: 'Oftenused' in subfield o
                        dataField.getSubField(DataField.hasSubFieldCode('o'))
                                .ifPresent(subField -> pathSubject.withOftenUsed(true));

                        // p: 'Pathcode' in subfield p
                        dataField.getSubField(DataField.hasSubFieldCode('p'))
                                .ifPresent(subField -> {
                                    String key = subField.getData();

                                    // Special case: Is this a span of years separated by "-"?
                                    //   For instance: 1990 - 2000.
                                    if ("i".equals(key)) {
                                        if (INTERVAL_PATTERN.matcher(title).find()) {
                                            key = "I1";
                                        } else {
                                            key = "I0";
                                        }
                                    }
                                    pathSubject.withPath(PathTranslator.valueOf(key).getPathValue());

                                });

                        // q: 'ID of subject' (not a faust!) in subfield q
                        dataField.getSubField(DataField.hasSubFieldCode('q'))
                                .ifPresentOrElse(
                                        subField -> handleID(pathSubject, subField.getData()),
                                        () -> {throw new IllegalArgumentException("Missing ID ('q' field)");});

                        // r: 'ref'. Internal tree reference for SOME objects ("handling.handler om") in subfield r
                        dataField.getSubField(DataField.hasSubFieldCode('r'))
                                .ifPresent(subField -> pathSubject.withRef(subField.getData()));

                        pathSubjects.add(pathSubject);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Unable to (fully) create:{}. Error:{}", faust, e.getMessage());
                    }
                }
        );
        return pathSubjects;
    }

    /**
     * Notes are a shared between standard dbc subjects, and subjects especially aimed at "Metakompas" use.
     * For each x09 field: Each subfield å might reference a shared note in field 680. From praxis:
     * 165 00 *a sød
     * 680 00 *å 1 *a En naiv, uskyldig og behagelig stemning. Stikord: venlig, elskelig, kær, blid
     * x08 00 *p s
     * x09 00 *p k
     * x09 00 *å 1 *p nc
     * <p>
     * IF x09 (from above example) featured only a *å 2 the 680 *a would not be included.
     * IF another 680 field featured only a *a (and no å subfields) this would be included in any case.
     *
     * @param marcBinding topics marcRecord
     */
    private Set<String> notes(MarcBinding marcBinding) {
        Set<String> notes = new LinkedHashSet<>();

        // Add all notes (680a) that are not indexed based. (No 'å' subfields).
        marcBinding.getDataFields(NOTES_FIELD).forEach(dataField -> {
            if (!dataField.hasSubField(DataField.hasSubFieldCode('å'))) {
                dataField.getSubField(DataField.hasSubFieldCode('a')).ifPresent(note -> notes.add(note.getData()));
            }
        });

        // Add the indexed ones. Index is 1 or 2 in x09å. Index points to 680å. In that very same 680, use the 'a' subfield as note.
        marcBinding.getSubFieldValues(TAXONOMY_FIELD, 'å').forEach(index ->
                marcBinding.getDataFields(NOTES_FIELD).forEach(dataField -> {
                    String noteFieldIndex = dataField.getSubField(DataField.hasSubFieldCode('å')).map(SubField::getData).orElse(null);
                    if (noteFieldIndex != null && noteFieldIndex.equals(index)) {
                        dataField.getSubField(DataField.hasSubFieldCode('a')).ifPresent(note -> notes.add(note.getData()));
                    }
                }));
        return notes;
    }

    private void handleID(PathSubject pathSubject, String idAsString) {
        int id =  Integer.parseInt(idAsString);
        if (id <=0 ) {
            throw new IllegalArgumentException(String.format("ID must be greater than zero: %d", id));
        }
        if (usedIds.contains(id)) {
            throw new IllegalArgumentException(String.format("Taxonomy ID %d is already in use", id));
        } else {
            usedIds.add(id);
            pathSubject.withId(id);
        }
    }
}
