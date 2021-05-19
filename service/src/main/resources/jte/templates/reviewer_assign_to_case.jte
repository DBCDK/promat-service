@import dk.dbc.promat.service.templating.model.AssignReviewer
@import dk.dbc.promat.service.templating.Formatting
@param AssignReviewer model

!{var promatCase = model.getPromatCase(); var note = model.getNote(); }


<p>Kære ${promatCase.getReviewer().getFirstName()} ${promatCase.getReviewer().getLastName()}</p>
<p>Du er blevet bedt om at lave en anmeldelse af følgende materiale:</p>
<p>@tag.title_section_tag(bibliographicInformationList = model.getTitleSections())</p>
<p>Anmeldelsen bedes udarbejdet senest: ${Formatting.format(promatCase.getDeadline())}</p>
@if(note != null)
    <p>Note: $unsafe{note}</p>
@endif

<p>@tag.metakompas_section_tag(promatCase = promatCase)</p>

@tag.how_to_get_material_tag(model = model)

<p>God fornøjelse</p>
<p>Med venlig hilsen,<br/>
ProMat redaktionen
</p>
