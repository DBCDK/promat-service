@import dk.dbc.promat.service.templating.model.AssignReviewer
@import dk.dbc.promat.service.persistence.PromatTask
@import dk.dbc.promat.service.persistence.PromatCase
@param AssignReviewer model

!{PromatCase promatCase = model.getPromatCase(); }

!{ String material = "Book" }

@if (promatCase.getFulltextLink() != null)
    !{material = "Printfile"; }
@else
    @for ( var title : model.getTitleSections())
        @if (title.getMaterialtypes().contains("Ebog"))
            !{ material = "Ebog"; }
        @endif
    @endfor
@endif

@if (material.equals("Printfile"))
    Link til åbning af printfil findes i promat.
@elseif (material.equals("Ebog"))
    Link til åbning af Ebog findes i promat.
@else
    Materialet er på vej til dig i posten.
@endif
