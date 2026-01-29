@import dk.dbc.promat.service.templating.model.DeadlinePassedMail
@import dk.dbc.promat.service.templating.Formatting
@param DeadlinePassedMail model

!{var promatCase = model.getPromatCase();}


<p>Kære ${promatCase.getReviewer().getFirstName()} ${promatCase.getReviewer().getLastName()}</p>
<p>Du er blevet bedt om at lave en anmeldelse af følgende materiale:</p>
<p>@template.tag.title_section_tag(bibliographicInformationList = model.getTitleSections())</p>
<p>Afleveringsfristen var ${Formatting.format(promatCase.getDeadline())}, men vi har endnu ikke modtaget udtalelsen fra dig.
Du bedes derfor færdiggøre den hurtigst muligt.</p>
<p>Kontakt evt. redaktionen for nærmere aftale</p>
<p>MVH. redaktionen</p>