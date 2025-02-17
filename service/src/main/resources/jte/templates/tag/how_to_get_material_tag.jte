@import dk.dbc.promat.service.templating.model.AssignReviewer
@import dk.dbc.promat.service.persistence.PromatTask
@import dk.dbc.promat.service.persistence.PromatCase
@param AssignReviewer model

!{PromatCase promatCase = model.getPromatCase(); }

!{ String material = "Book"; }

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
        Den trykte bog er på vej med posten, og der er et link til åbning af e-bog i Promat.
    @else
        Link til åbning af e-bog findes i Promat.
    @endif
@elseif (material.equals("Ebog"))
    Link til åbning af e-bog findes i Promat.
@else
    Materialet er på vej med posten.
@endif
