@import dk.dbc.promat.service.persistence.Reviewer
@import dk.dbc.promat.service.templating.model.ReviewerDataChanged
@import java.util.List
@param ReviewerDataChanged reviewerDataChanged
!{ List<String> fieldNames = List.of("firstName", "lastName", "email", "phone",
 "institution","address1", "address2", "zip", "city", "hiatus_begin", "hiatus_end", "selected",
 "capacity", "privateAddress1", "privateAddress2", "privateZip", "privateCity", "privateSelected", "privateEmail");
 Reviewer reviewer = reviewerDataChanged.getReviewer(); }

<p>Anmelderprofilen '${reviewer.getId()}' (${reviewer.getFirstName()} ${reviewer.getLastName()}) er ændret,
    her er ændringerne:</p>

<table>

    @for(var field : fieldNames)
        @if(reviewerDataChanged.getDiffMap().get(field) != null)
            <tr>
                <td>[${field}]</td>
            </tr>
            @if(field.equals("email"))
                <tr>
                    <td><table>
                            <tr>
                                <td>
                                    Fra:
                                </td>
                                <td>
                                    <a href="mailto:${reviewerDataChanged.getDiffMap().get(field).getFromValue()}">
                                        ${reviewerDataChanged.getDiffMap().get(field).getFromValue()}
                                    </a>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Til:
                                </td>
                                <td>
                                    <a href="mailto:${reviewerDataChanged.getDiffMap().get(field).getToValue()}">
                                        ${reviewerDataChanged.getDiffMap().get(field).getToValue()}
                                    </a>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            @elseif(field.equals("privateEmail"))
                <tr>
                    <td><table>
                            <tr>
                                <td>
                                    Fra:
                                </td>
                                <td>
                                    <a href="mailto:${reviewerDataChanged.getDiffMap().get(field).getFromValue()}">
                                        ${reviewerDataChanged.getDiffMap().get(field).getFromValue()}
                                    </a>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Til:
                                </td>
                                <td>
                                    <a href="mailto:${reviewerDataChanged.getDiffMap().get(field).getToValue()}">
                                        ${reviewerDataChanged.getDiffMap().get(field).getToValue()}
                                    </a>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            @else
                <tr>
                    <td><table>
                            <tr>
                                <td>
                                    Fra:
                                </td>
                                <td>
                                    ${reviewerDataChanged.getDiffMap().get(field).getFromValue()}
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    Til:
                                </td>
                                <td>
                                    ${reviewerDataChanged.getDiffMap().get(field).getToValue()}
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            @endif
            <tr>
                <td><br/></td>
            </tr>
        @endif
    @endfor
</table>