@import dk.dbc.promat.service.templating.model.AssignReviewerNotification
@import dk.dbc.promat.service.templating.Formatting
@param AssignReviewerNotification assignedCase

!{var promatCase = assignedCase.getPromatCase(); var note = assignedCase.getNote(); }


<p><b>Kære ${promatCase.getReviewer().getFirstName()} ${promatCase.getReviewer().getLastName()}</b></p>
<p>
Du er blevet bedt om at lave anmeldelse af følgende materiale:
</p>
<p><i>"${promatCase.getTitle()}"</i></p>
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
* Add repeatable (1 or more) section:
*   title, faust, isbn and material
* Add "metakompas" (0 or 1)
* Add "Link for opening ..." (How to check for this?)
* Add note layout (0 or 1)
* Fix "Materialet er på vej..." (How to check for this?)
--%>
