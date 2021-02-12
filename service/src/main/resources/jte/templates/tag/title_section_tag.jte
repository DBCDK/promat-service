@param  java.util.List<dk.dbc.promat.service.api.BibliographicInformation> bibliographicInformationList
<table>
    @for (var bibliographicInformation : bibliographicInformationList)
        <table>
            @if(bibliographicInformation.getFaust() != null)
                <tr>
                    <td>Faust:</td>
                    <td>${bibliographicInformation.getFaust()}</td>
                </tr>
            @endif
            @if(bibliographicInformation.getMaterialtypes() != null)
                <tr>
                    <td>Materialetype:</td>
                    <td>${String.join(", ", bibliographicInformation.getMaterialtypes())}</td>
                </tr>
            @endif
            @if(bibliographicInformation.getTitle() != null)
                <tr>
                    <td>Titel:</td>
                    <td>${bibliographicInformation.getTitle()}</td>
                </tr>
            @endif
            @if(bibliographicInformation.getIsbn() != null)
                <tr>
                    <td>ISBN:</td>
                    <td>${String.join(",",bibliographicInformation.getIsbn())}</td>
                </tr>
            @endif
            <tr><td><br/></td></tr>
        </table>
    @endfor
</table>

