package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.templating.Formatting;

import javax.xml.bind.annotation.XmlElement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private String GetTaskDataForFaust(String requestedFaust, TaskFieldType fieldType, List<PromatTask> tasks) throws ServiceErrorException {

        // Get the task that matches faust and fieldtype
        Optional<PromatTask> task;

        // Try to get a task that has targetfaust = requested faust
        task = tasks.stream()
                .filter(t -> t.getTaskFieldType() == fieldType &&
                        t.getTargetFausts() != null && t.getTargetFausts().contains(requestedFaust))
                .findFirst();

        // If no task was found, try to find a task without targetFaust (relates to the primary, or faustnumbers
        // related to the primary faust)
        if(!task.isPresent()) {
            task = tasks.stream()
                    .filter(t -> t.getTaskFieldType() == fieldType &&
                            (t.getTargetFausts() == null || t.getTargetFausts().size() == 0))
                    .findFirst();
        }

        // Return task data, if a task was found
        if(task.isPresent()) {
            return task.get().getData();
        }

        // No data was found. This is not an error - it is totally plausible that fields does not exist
        return "";
    }

    public XmlCaseview from(String hostname, String requestedFaust, PromatCase promatCase) throws ServiceErrorException {

        String bkmeval = GetTaskDataForFaust(requestedFaust, TaskFieldType.BKM, promatCase.getTasks());
        String brief = GetTaskDataForFaust(requestedFaust, TaskFieldType.BRIEF, promatCase.getTasks());
        String description = GetTaskDataForFaust(requestedFaust, TaskFieldType.DESCRIPTION, promatCase.getTasks());
        String evaluation = GetTaskDataForFaust(requestedFaust, TaskFieldType.EVALUATION, promatCase.getTasks());
        String comparison = GetTaskDataForFaust(requestedFaust, TaskFieldType.COMPARISON, promatCase.getTasks());
        String recommendation = GetTaskDataForFaust(requestedFaust, TaskFieldType.RECOMMENDATION, promatCase.getTasks());
        String age = GetTaskDataForFaust(requestedFaust, TaskFieldType.AGE, promatCase.getTasks());
        List<String> matlevels = Arrays.asList(GetTaskDataForFaust(requestedFaust, TaskFieldType.MATLEVEL, promatCase.getTasks())
                .split("[;|,]"))
                .stream()
                .map(t -> t.trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
        List<String> subjterms = Arrays.asList(GetTaskDataForFaust(requestedFaust, TaskFieldType.TOPICS, promatCase.getTasks())
                .split("[;|,]"))
                .stream()
                .map(t -> t.trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());

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
                        .withAge(age)
                        .withMatlevel(new XmlCaseviewMatlevels(requestedFaust, matlevels))
                        .withSubjterm(new XmlCaseviewSubjterms(requestedFaust, subjterms)));

        return this;
    }
}
