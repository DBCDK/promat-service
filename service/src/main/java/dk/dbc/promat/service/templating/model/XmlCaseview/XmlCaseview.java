/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.Formatting;

import javax.xml.bind.annotation.XmlElement;
import java.util.Arrays;
import java.util.List;

@JsonPropertyOrder({"response", "case"})
@JacksonXmlRootElement(localName = "promat_xml_collection")
public class XmlCaseview {

    @XmlElement(name = "response")
    private XmlCaseviewResponse caseviewResponse;

    @XmlElement(name = "case")
    private XmlCaseviewCase caseviewCase;

    public XmlCaseviewResponse getCaseviewResponse() {
        return caseviewResponse;
    }

    public void setCaseviewResponse(XmlCaseviewResponse caseviewResponse) {
        this.caseviewResponse = caseviewResponse;
    }

    public XmlCaseview withResponse(XmlCaseviewResponse response) {
        this.caseviewResponse = response;
        return this;
    }

    public XmlCaseviewCase getCase() {
        return caseviewCase;
    }

    public void setCase(XmlCaseviewCase promatcase) {
        this.caseviewCase = promatcase;
    }

    public XmlCaseview withCase(XmlCaseviewCase promatcase) {
        this.caseviewCase = promatcase;
        return this;
    }

    private String GetTaskDataForFaust(String requestedFaust, TaskFieldType fieldType, List<PromatTask> tasks) {
        // Todo: Return task data - make sure to html encode content so that ampersands etc. is correctly formatted
        return "data";
    }

    public XmlCaseview from(String hostname, String requestedFaust, PromatCase promatCase) {

        String bkmeval = GetTaskDataForFaust(requestedFaust, TaskFieldType.BKM, promatCase.getTasks());
        String brief = GetTaskDataForFaust(requestedFaust, TaskFieldType.BRIEF, promatCase.getTasks());
        String description = GetTaskDataForFaust(requestedFaust, TaskFieldType.DESCRIPTION, promatCase.getTasks());
        String evaluation = GetTaskDataForFaust(requestedFaust, TaskFieldType.EVALUATION, promatCase.getTasks());
        String comparison = GetTaskDataForFaust(requestedFaust, TaskFieldType.COMPARISON, promatCase.getTasks());
        String recommendation = GetTaskDataForFaust(requestedFaust, TaskFieldType.RECOMMENDATION, promatCase.getTasks());
        String genre = GetTaskDataForFaust(requestedFaust, TaskFieldType.GENRE, promatCase.getTasks());
        String age = GetTaskDataForFaust(requestedFaust, TaskFieldType.AGE, promatCase.getTasks());
        String matlevel = GetTaskDataForFaust(requestedFaust, TaskFieldType.MATLEVEL, promatCase.getTasks());
        List<String> subjterms = Arrays.asList("term1", "term2", "term3"); // Todo: This comes as a comma separated list.. handle that

        caseviewResponse = new XmlCaseviewResponse()
                .withServer(hostname)
                .withRequestArg(new XmlCaseviewRequestArg()
                .withFaustno(requestedFaust));
        caseviewCase = new XmlCaseviewCase()
                .withCaseId(promatCase.getId())
                .withCaseMetadata(new XmlCaseviewCaseMetadata()
                        .withDeadline(promatCase.getDeadline())
                        .withCasestate(Formatting.format(promatCase.getStatus()))
                        .withUser(Formatting.format(promatCase.getReviewer()))
                        .withNote(promatCase.getDetails()))
                .withBibMetadata(new XmlCaseviewBibMetadata()
                        .withTitle(promatCase.getTitle())
                        .withAuthor(promatCase.getAuthor())
                        .withFaust(requestedFaust)
                        .withWeekcode(new XmlCaseviewWeekcode(promatCase.getWeekCode()))
                        .withPublisher(promatCase.getPublisher()))
                .withData(new XmlCaseviewData()
                        .withBkmeval(bkmeval)
                        .withBrief(new XmlCaseviewBrief(requestedFaust, brief))
                        .withDescription(description)
                        .withEvaluation(evaluation)
                        .withComparison(comparison)
                        .withRecommendation(recommendation)
                        .withGenre(genre)
                        .withAge(age)
                        .withMatlevel(matlevel)
                        .withSubjterm(new XmlCaseviewSubjterms(requestedFaust, subjterms)));

        return this;
    }
}
