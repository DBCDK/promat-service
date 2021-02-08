@import dk.dbc.promat.service.templating.model.AssignReviewerNotification
@import dk.dbc.promat.service.templating.Formatting
@param AssignReviewerNotification model

!{var promatCase = model.getPromatCase(); var note = model.getNote(); }


<p><b>Kære ${promatCase.getReviewer().getFirstName()} ${promatCase.getReviewer().getLastName()}</b></p>
<p>
Du er blevet bedt om at lave anmeldelse af følgende materiale:
</p>
<p>@tag.title_section_tag(bibliographicInformationList = model.getTitleSections())</p>
@if(note != null)
    <p><b>Note:</b>${note}</p>
@endif
<p>Anmeldelsen bedes udarbejdet senest: ${Formatting.format(promatCase.getDeadline())}
<br/>

Materialet er på vej til dig i posten.</p>
<br/>
<p>Med venlig hilsen,<br/>
ProMat redaktionen
</p>
<%-- todo:
* Add "metakompas" (0 or 1)
* Add "Link for opening ..."
* Add note layout (0 or 1)
* Fix "Materialet er på vej..." (How to check for this?)
--%>