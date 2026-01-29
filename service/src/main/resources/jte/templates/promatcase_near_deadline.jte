@import dk.dbc.promat.service.templating.model.EarlyReminderMail
@import dk.dbc.promat.service.templating.Formatting
@param EarlyReminderMail model

!{var promatCase = model.getPromatCase();}


<p>Kære ${promatCase.getReviewer().getFirstName()} ${promatCase.getReviewer().getLastName()}</p>
<p>Du er blevet bedt om at lave en anmeldelse af følgende materiale:</p>
<p>@template.tag.title_section_tag(bibliographicInformationList = model.getTitleSections())</p>
<p>Afleveringsfristen er ${Formatting.format(promatCase.getDeadline())}. Du har derfor mindre end 3 dage til at
    gøre arbejdet færdigt.</p>
<p>MVH. redaktionen</p>