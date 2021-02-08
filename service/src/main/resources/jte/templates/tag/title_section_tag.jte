@param  java.util.List<dk.dbc.promat.service.api.BibliographicInformation> bibliographicInformationList

<table>
    @for (var bibliographicInformation : bibliographicInformationList)
        <table>
            @if(bibliographicInformation.getTitle() != null)
                <tr>
                    <td><b>Titel:</b></td>
                    <td>${bibliographicInformation.getTitle()}</td>
                </tr>
            @endif

            @if(bibliographicInformation.getFaust() != null)
                <tr>
                    <td><b>Faust:</b></td>
                    <td>${bibliographicInformation.getFaust()}</td>
                </tr>
            @endif
            @if(bibliographicInformation.getIsbn() != null)
                <tr>
                    <td><b>Isbn:</b></td>
                    <td>${String.join(",",bibliographicInformation.getIsbn())}</td>
                </tr>
            @endif
            @if(bibliographicInformation.getMaterialtypes() != null)
                <tr>
                    <td><b>Materialetype:</b></td>
                    <td>${String.join(", ", bibliographicInformation.getMaterialtypes())}</td>
                </tr>
            @endif
            <tr><td><br/></td></tr>
        </table>


    @endfor

</table>

