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
    <title>Anmeldelse for ${requestedFaustnumber} + (${relatedFaustnumbers.stream().sorted().collect(Collectors.joining(","))})</title>
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

        <div class="data">

            <!-- TaskFieldType = BKM -->
            <div class="message">
                <div>BKM-vurdering:</div>
                <div>
                    !{var bkm = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.BKM).findFirst(); }
                    ${bkm.isPresent() ? bkm.get().getData() : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = BRIEF -->
            @for (PromatTask task : promatCase.getTasks().stream()
                    .filter(t -> t.getTaskFieldType() == TaskFieldType.BRIEF)
                    .collect(Collectors.toList()))
                <div class="message">
                    <div>Kort om:</div>
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
            <div class="message">
                <div>Beskrivelse:</div>
                <div>
                    !{var description = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.DESCRIPTION).findFirst(); }
                    ${description.isPresent() ? description.get().getData() : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = EVALUATION -->
            <div class="message">
                <div>Vurdering:</div>
                <div>
                    !{var evaluation = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.EVALUATION).findFirst(); }
                    ${evaluation.isPresent() ? evaluation.get().getData() : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = COMPARISON -->
            <div class="message">
                <div>Andre om samme emne/genre:</div>
                <div>
                    !{var comparison = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.COMPARISON).findFirst(); }
                    $unsafe{comparison.isPresent() ? comparison.get().getData().replace("<t>", "<span style=\"font-style: italic;\">").replace("</t>", "</span>") : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = RECOMMENDATION -->
            <div class="message">
                <div>Til bibliotekaren:</div>
                <div>
                    !{var recommendation = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.RECOMMENDATION).findFirst(); }
                    ${recommendation.isPresent() ? recommendation.get().getData() : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = TOPICS -->
            @for (PromatTask task : promatCase.getTasks().stream()
                    .filter(t -> t.getTaskFieldType() == TaskFieldType.TOPICS)
                    .collect(Collectors.toList()))
                <div class="message">
                    <div>Emneord:</div>
                    <div>
                        ${task.getTargetFausts() != null && task.getTargetFausts().size() > 0
                                ? task.getTargetFausts().stream()
                                        .collect(Collectors.joining(", "))
                                : promatCase.getPrimaryFaust()
                        }: ${task.getData()}
                    </div>
                </div>
            @endfor

            <!-- TaskFieldType = GENRE -->
            <div class="message">
                <div>Genre- og formbetegnelser (anvendes til skønlitteratur for børn og voksne):</div>
                <div>
                    !{var genre = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.GENRE).findFirst(); }
                    ${genre.isPresent() ? genre.get().getData() : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = AGE -->
            <div class="message">
                <div>Alder (anvendes til fag- og skønlitteratur for børn):</div>
                <div>
                    !{var age = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.AGE).findFirst(); }
                    ${age.isPresent() ? age.get().getData() : "&nbsp;"}
                </div>
            </div>

            <!-- TaskFieldType = MATLEVEL -->
            <div class="message">
                <div>Niveau/målgruppe (anvendes til skønlitteratur for børn):</div>
                <div>
                    !{var matlevel = promatCase.getTasks().stream().filter(t -> t.getTaskFieldType() == TaskFieldType.MATLEVEL).findFirst(); }
                    ${matlevel.isPresent() ? matlevel.get().getData() : "&nbsp;"}
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>



