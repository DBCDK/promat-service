package dk.dbc.promat.service.service;

import dk.dbc.promat.service.api.PredicateFactory;
import dk.dbc.promat.service.api.RecordsResolver;
import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dk.dbc.promat.service.persistence.CaseStatus.CLOSED;
import static dk.dbc.promat.service.persistence.CaseStatus.DELETED;
import static dk.dbc.promat.service.persistence.CaseStatus.EXPORTED;
import static dk.dbc.promat.service.persistence.CaseStatus.valueOf;

@ApplicationScoped
public class CaseSearch {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseSearch.class);
    // Default number of results when getting cases
    private static final int DEFAULT_CASES_LIMIT = 100;
    @ConfigProperty
    private String solrUrl;

//    private HttpSolrClient solrClient;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    RecordsResolver recordsResolver;

    @SuppressWarnings("unused")
    public CaseSearch() {
    }

//    public CaseSearch(String solrUrl) {
//        this.solrUrl = solrUrl;
//        solrClient = new HttpSolrClient.Builder(solrUrl).withConnectionTimeout(500).withSocketTimeout(2000).build();
//    }

//    @PostConstruct
//    public void init() {
//        solrClient = new HttpSolrClient.Builder(solrUrl).withConnectionTimeout(500).withSocketTimeout(2000).build();
//    }

    public CaseSummaryList listCases(ListCasesParams params) throws ServiceErrorException {
        // Initialize query and criteriabuilder
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PromatCase> criteriaQuery = builder.createQuery(PromatCase.class);

        // Create query root
        Root<PromatCase> root = criteriaQuery.from(PromatCase.class);

        criteriaQuery.distinct(true);
        criteriaQuery.select(root);

        // List of all predicates to be AND'ed together on the final query
        List<Predicate> allPredicates = new ArrayList<>();

        // Get case with given primary or related
        callConditionally(params::getFaust, f ->  makeFaustPredicate(f, builder, root)).ifPresent(allPredicates::add);

        // Search by faust, ean (barcode) or isbn.
        callConditionally(params::getId, id -> makeIdPredicates(id, builder, root)).ifPresent(allPredicates::add);

        // Get cases with given set of statuses
        Predicate statusPredicate = callConditionally(params::getStatus, s -> makeStatusPredicate(s, builder, root))
                .orElse(builder.notEqual(root.get("status"), DELETED));
        allPredicates.add(statusPredicate);

        // Get cases with given reviewer
        callConditionally(params::getReviewer, r -> builder.equal(root.get("reviewer").get("id"), r)).ifPresent(allPredicates::add);

        // Get cases with given editor
        callConditionally(params::getEditor, e -> builder.equal(root.get("editor").get("id"), e)).ifPresent(allPredicates::add);

        // Get cases with given creator
        callConditionally(params::getCreator, creator -> builder.equal(root.get("creator").get("id"), creator));

        // Get cases with a title that matches (entire, or part of) the given title
        callConditionally(params::getTitle, title -> makeTitlePredicate(title, builder, root)).ifPresent(allPredicates::add);

        // Get cases with an author that matches (entire, or part of) the given author
        callConditionally(params::getAuthor, author -> builder.like(builder.lower(root.get("author")), builder.literal("%" + author.toLowerCase() + "%"))).ifPresent(allPredicates::add);

        callConditionally(params::getTrimmedWeekcode, trimmedWeekcode ->
                PredicateFactory.fromBinaryOperator(params.getTrimmedWeekcodeOperator(), root.get("trimmedWeekCode"), trimmedWeekcode, builder))
                .ifPresent(allPredicates::add);

        callConditionally(params::getWeekCode, w -> makeWeekCodePredicate(w, builder, root)).ifPresent(allPredicates::add);

        // Get cases with these (comma separated) materials
        callConditionally(params::getMaterials, materials -> makeMaterialsPredicate(materials, builder, root)).ifPresent(allPredicates::add);

        // If a starting id has been given, add this
        callConditionally(params::getFrom, from -> builder.gt(root.get("id"), builder.literal(from))).ifPresent(allPredicates::add);

        // If an ending id has been given, add this
        callConditionally(params::getTo, to -> builder.lt(root.get("id"), builder.literal(to))).ifPresent(allPredicates::add);

        // Publisher parameter
        callConditionally(params::getPublisher, publisher -> builder.like(root.get("publisher"), builder.literal("%" + publisher + "%"))).ifPresent(allPredicates::add);

        // Combine all where clauses together with AND and add them to the query
        if (!allPredicates.isEmpty()) {
            Predicate finalPredicate = builder.and(allPredicates.toArray(Predicate[]::new));
            criteriaQuery.where(finalPredicate);
        }


        // Add ordering
        ListCasesParams.Order order = params.getOrder();
        if (order == ListCasesParams.Order.DESCENDING) {
            criteriaQuery.orderBy(builder.desc(root.get("id")));
        } else {
            criteriaQuery.orderBy(builder.asc(root.get("id")));
        }

        // Add limits
        TypedQuery<PromatCase> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(params.getLimit() == null ? DEFAULT_CASES_LIMIT : params.getLimit());

        // Execute the query
        // TODO: 12/01/2021 Rename CaseSummaryList to CaseList
        CaseSummaryList caseList = new CaseSummaryList();
        caseList.getCases().addAll(query.getResultList());

        // If requested format is EXPORT, then it is not allowed to return cases without a faustnumber.
        // We cannot check for this in the query directly, so instead remove offending results
        if (params.getFormat() == ListCasesParams.Format.EXPORT) {
            List<PromatCase> exports = caseList.getCases().stream().filter(PromatCase::isValidForExport).collect(Collectors.toList());
            caseList.setCases(exports);
            LOGGER.info("Caselist now has {} cases", caseList.getCases().size());
        }

        // Set final number of cases and return the list
        caseList.setNumFound(caseList.getCases().size());
        return caseList;
    }

    private Predicate makeIdPredicates(String id, CriteriaBuilder builder, Root<PromatCase> root) throws ServiceErrorException {
        try {
            Set<String> fausts;
            Set<Integer> caseIds = new HashSet<>();

            // Is this a faust?
            if (id.length() < 10) {
                fausts = Set.of(id);
            } else {

                // This is EAN (barcode) or ISBN.
                RecordsListDto faustList = (RecordsListDto) recordsResolver.resolveId(id);
                fausts = faustList.getRecords().stream().map(RecordDto::getFaust).collect(Collectors.toSet());
            }
            // Fetch all caseids
            for (String f : fausts) {
                TypedQuery<PromatCase> query = entityManager.createNamedQuery(PromatCase.LIST_CASE_BY_FAUST_NAME, PromatCase.class);
                query.setParameter("faust", f);
                caseIds.addAll(query.getResultList().stream().map(PromatCase::getId).collect(Collectors.toList()));
            }

            if (!caseIds.isEmpty()) {
                CriteriaBuilder.In<Integer> inIdsClause = builder.in(root.get("id"));

                // Now set caseid, one by one.
                for (Integer cid : caseIds) {
                    inIdsClause.value(cid);
                }
                return builder.and(inIdsClause);
            }
            throw new ServiceErrorException("Found no match for id " + id);
        } catch (Exception e) {
            throw new ServiceErrorException("Failed to lookup id " + id + ", " + e.getMessage());
        }
    }

    private Predicate makeTitlePredicate(String title, CriteriaBuilder builder, Root<PromatCase> root) {
        return builder.like(builder.lower(root.get("title")), builder.literal("%" + title.toLowerCase() + "%"));

//        try {
//            List<String> fausts = faustSearch(title);
//            return makeFaustListPredicate(fausts, builder, root);
//        } catch (SolrServerException e) {
//            throw new ServiceErrorException("Solr got angry while searching for " + title + ", message: " + e.getMessage());
//        } catch (IOException e) {
//            throw new ServiceErrorException("Threw an io exception while searching for " + title + ", message: " + e.getMessage());
//        }
    }

//    private Predicate makeFaustListPredicate(List<String> fausts, CriteriaBuilder builder, Root<PromatCase> root) {
//        TypedQuery<PromatCase> query = entityManager.createNamedQuery(PromatCase.LIST_CASE_BY_FAUST_NAME, PromatCase.class);
//        CriteriaBuilder.In<Integer> inIdsClause = builder.in(root.get("id"));
//        fausts.stream()
//                .map(f -> query.setParameter("faust", f))
//                .map(TypedQuery::getResultList)
//                .flatMap(Collection::stream)
//                .map(PromatCase::getId)
//                .forEach(inIdsClause::value);
//        return inIdsClause;
//    }

    private Predicate makeMaterialsPredicate(String materials, CriteriaBuilder builder, Root<PromatCase> root) throws ServiceErrorException {
        List<Predicate> materialsPredicates = new ArrayList<>();
        for (String oneMaterial : materials.split(",")) {
            try {
                materialsPredicates.add(builder.equal(root.get("materialType"), MaterialType.valueOf(oneMaterial)));
            } catch (IllegalArgumentException ex) {
                ServiceErrorDto error = new ServiceErrorDto().withCode(ServiceErrorCode.INVALID_REQUEST).withCause("Invalid material type").withDetails(String.format("Unknown material: %s", oneMaterial));
                throw new ServiceErrorException(error.getCause()).withHttpStatus(400);
            }
        }
        return builder.or(materialsPredicates.toArray(Predicate[]::new));
    }

    private Predicate makeWeekCodePredicate(String weekCode, CriteriaBuilder builder, Root<PromatCase> root) {
        Predicate weekCodePredicate = builder.equal(builder.lower(root.get("weekCode")), weekCode.toLowerCase());
        Predicate codesPredicate = builder.isTrue(builder.function("JsonbContainsFromString", Boolean.class, root.get("codes"), builder.upper(builder.literal(weekCode))));
        return builder.or(weekCodePredicate, codesPredicate);
    }

    private Predicate makeStatusPredicate(String status, CriteriaBuilder builder, Root<PromatCase> root) throws ServiceErrorException {
        // Allthough jax.rs actually supports having multiple get arguments with the same name
        // "?status=CREATED&status=ASSIGNED" this is not a safe implementation since other
        // frameworks (React/NextJS or others) may have difficulties handling this. So instead
        // a list of statuses is expected to be given as a comma separated list

        List<Predicate> statusPredicates = new ArrayList<>();
        for (String oneStatus : status.split(",")) {
            try {
                statusPredicates.add(builder.equal(root.get("status"), valueOf(oneStatus)));
            } catch (IllegalArgumentException ex) {
                ServiceErrorDto error = new ServiceErrorDto().withCode(ServiceErrorCode.INVALID_REQUEST).withCause("Invalid case status").withDetails(String.format("Unknown case status: %s", oneStatus));
                throw new ServiceErrorException(error.getCause()).withHttpStatus(400);
            }
        }
        return builder.or(statusPredicates.toArray(Predicate[]::new));
    }

    private <P> Optional<Predicate> callConditionally(Supplier<P> supplier, ServiceCall<P, Predicate> service) throws ServiceErrorException {
        P p = supplier.get();
        if(p != null) {
            if((p instanceof String && ((String) p).isBlank())) {
                return Optional.empty();
            } else if (p instanceof Integer && ((Integer) p) > 0) {
                return Optional.empty();
            }
            return Optional.of(service.call(p));
        }
        return Optional.empty();
    }

    public interface ServiceCall<T, R> {
        R call(T t) throws ServiceErrorException;
    }

    private Predicate makeFaustPredicate(String faust, CriteriaBuilder builder, Root<PromatCase> root) {
        Join<PromatCase, PromatTask> tasks = root.join("tasks", JoinType.LEFT);
        Predicate primaryFaustPredicate = builder.equal(root.get("primaryFaust"), builder.literal(faust));
        Predicate relatedFaustsPredicate = builder.isTrue(builder.function("JsonbContainsFromString", Boolean.class, tasks.get("targetFausts"), builder.literal(faust)));
        Predicate faustPredicate = builder.or(primaryFaustPredicate, relatedFaustsPredicate);

        // And status not CLOSED or DONE
        CriteriaBuilder.In<CaseStatus> inClause = builder.in(root.get("status"));
        inClause.value(CLOSED).value(EXPORTED).value(DELETED);
        Predicate statusPredicate = builder.not(inClause);

        return builder.and(faustPredicate, statusPredicate);
    }

//    public List<String> faustSearch(String title) throws SolrServerException, IOException {
//        SolrQuery query = new SolrQuery("term.ti:" + title)
//                .addFilterQuery("marc.001b:870976", "scan.kk:acc*")
//                .setRows(100)
//                .addSort("sort.001d", SolrQuery.ORDER.desc);
//        query.set("fl", "marc.001a");
//        QueryResponse response = solrClient.query(query);
//        return response.getResults().stream().flatMap(s -> s.values().stream()).map(Object::toString).collect(Collectors.toList());
//    }
}
