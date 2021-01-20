@import dk.dbc.promat.service.persistence.PromatCase
@import dk.dbc.promat.service.templating.Formatting
@param PromatCase assignedCase
<p><b>Kære ${assignedCase.getReviewer().getFirstName()} ${assignedCase.getReviewer().getLastName()}</b></p>

<p>
Du er blevet bedt om at lave anmeldelse af følgende materiale:
</p>
<p><i>"${assignedCase.getTitle()}"</i></p>
<p>Anmeldelsen bedes udarbejdet senest: ${Formatting.format(assignedCase.getDeadline())}
<br/>
Materialet er på vej til dig i posten.</p>
<br/>
<p>Med venlig hilsen,<br/>
ProMat redaktionen
</p>