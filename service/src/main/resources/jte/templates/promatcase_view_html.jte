@import dk.dbc.promat.service.persistence.PromatCase
@import dk.dbc.promat.service.persistence.PromatTask
@import dk.dbc.promat.service.persistence.TaskFieldType
@import dk.dbc.promat.service.templating.Formatting
@import java.util.List
@import java.util.Optional
@import java.util.stream.Collectors

@param PromatCase promatCase
@param String requestedFaustnumber
@param List<String> relatedFaustnumbers

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>Anmeldelse for ${requestedFaustnumber}@if(relatedFaustnumbers.size() > 0) + (${relatedFaustnumbers.stream().sorted().collect(Collectors.joining(","))})@endif</title>
    <style type="text/css">
        body {font: 10pt arial, helvetica, sans-serif;}
        div.metadata {}
        div.metadata div {font-weight: bold}
        div.metadata div.data {position: relative; display: block; font-weight: normal; left: 4cm; margin-top: -20px; min-height: 20px;}
        div.fields {padding-top: 0.6cm;}
        div.field {padding-top: 0.2cm;}
        div.field .type {font-weight: bold}
    </style>
</head>
<body>
<div class="main">
    <div class="firstrecord">
        <div class="metadata">
            <div>Titel:<div class="data">${promatCase.getTitle()}</div></div>
            <div>Forfatter:<div class="data">${promatCase.getAuthor()}</div></div>
            <div>Forlag:<div class="data">${promatCase.getPublisher()}</div></div>
            <div>Faustnr:<div class="data">${promatCase.getPrimaryFaust()}</div></div>
            <div>Anmelder:<div class="data">${Formatting.format(promatCase.getReviewer())}</div></div>
            <div>Frist:<div class="data">${Formatting.format(promatCase.getDeadline())}</div></div>
            <div>Status:<div class="data">${Formatting.format(promatCase.getStatus())}</div></div>
            <div>Redakt&oslash;r:<div class="data">${Formatting.format(promatCase.getEditor())}</div></div>
        </div>

        <div class="fields">

            <!-- TaskFieldType = BKM -->
            !{var bkm = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.BKM).findFirst(); }
            @if(bkm.isPresent())
            <div class="field">
                <div class="type">BKM-vurdering:</div>
                <div>
                    ${bkm.get().getData()}
                </div>
            </div>
            @endif

            <!-- TaskFieldType = BRIEF -->
            @for (PromatTask task : promatCase.getTasks().stream()
                    .filter(t -> t.getTaskFieldType() == TaskFieldType.BRIEF)
                    .collect(Collectors.toList()))
                <div class="field">
                    <div class="type">Kort om:</div>
                    <div>
                        ${task.getTargetFausts() != null && task.getTargetFausts().size() > 0
                                ? task.getTargetFausts().stream()
                                        .collect(Collectors.joining(", "))
                                : promatCase.getPrimaryFaust()
                        }: ${task.getData()}
                    </div>
                </div>
            @endfor

            <!-- TaskFieldType = DESCRIPTION -->
            <div class="field">
                <div class="type">Beskrivelse:</div>
                <div>
                    !{var description = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.DESCRIPTION).findFirst(); }
                    ${description.isPresent() && description.get().getData() != null ? description.get().getData() : ""}
                </div>
            </div>

            <!-- TaskFieldType = EVALUATION -->
            <div class="field">
                <div class="type">Vurdering:</div>
                <div>
                    !{var evaluation = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.EVALUATION).findFirst(); }
                    ${evaluation.isPresent() && evaluation.get().getData() != null ? evaluation.get().getData() : ""}
                </div>
            </div>

            <!-- TaskFieldType = COMPARISON -->
            <div class="field">
                <div class="type">Andre om samme emne/genre:</div>
                <div>
                    !{var comparison = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.COMPARISON).findFirst(); }
                    !{var comparisonvalue = comparison.isPresent() ? (comparison.get().getData() != null ? comparison.get().getData() : "") : ""; }
                    $unsafe{comparisonvalue.replace("<t>", "<span style=\"font-style: italic;\">").replace("</t>", "</span>")}
                </div>
            </div>

            <!-- TaskFieldType = RECOMMENDATION -->
            <div class="field">
                <div class="type">Til bibliotekaren:</div>
                <div>
                    !{var recommendation = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.RECOMMENDATION).findFirst(); }
                    ${recommendation.isPresent() && recommendation.get().getData() != null ? recommendation.get().getData() : ""}
                </div>
            </div>

            <!-- TaskFieldType = TOPICS -->
            @for (PromatTask task : promatCase.getTasks().stream()
                    .filter(t -> t.getTaskFieldType() == TaskFieldType.TOPICS)
                    .collect(Collectors.toList()))
                <div class="field">
                    <div class="type">Emneord:</div>
                    <div>
                        ${task.getTargetFausts() != null && task.getTargetFausts().size() > 0
                                ? task.getTargetFausts().stream()
                                        .collect(Collectors.joining(", "))
                                : promatCase.getPrimaryFaust()
                        }: ${task.getData()}
                    </div>
                </div>
            @endfor

            <!-- TaskFieldType = AGE -->
            <div class="field">
                <div class="type">Alder (anvendes til fag- og skønlitteratur for børn):</div>
                <div>
                    !{var age = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.AGE).findFirst(); }
                    ${age.isPresent() && age.get().getData() != null ? age.get().getData() : ""}
                </div>
            </div>

            <!-- TaskFieldType = MATLEVEL -->
            <div class="field">
                <div class="type">Niveau/målgruppe (anvendes til skønlitteratur for børn):</div>
                <div>
                    !{var matlevel = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.MATLEVEL).findFirst(); }
                    ${matlevel.isPresent() && matlevel.get().getData() != null ? matlevel.get().getData() : ""}
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>



