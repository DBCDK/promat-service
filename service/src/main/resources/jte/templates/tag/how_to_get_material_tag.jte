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
    @if (promatCase.getDetails() != null && promatCase.getDetails().contains("VENTER PÅ ACT POST"))
        Link til åbning af e-bog findes i promat, og den trykte bog er på vej med posten.
    @else
        Link til åbning af e-bog findes i promat.
    @endif
@elseif (material.equals("Ebog"))
    Link til åbning af e-bog findes i promat.
@else
    Materialet er på vej med posten.
@endif
