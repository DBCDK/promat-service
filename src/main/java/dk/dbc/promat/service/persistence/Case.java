/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "promat_case")
@NamedQuery(
        name = "getExistingCases",
        query = "SELECT c FROM Case c WHERE FUNCTION('jsonb_contains', c.relatedFausts, CAST(:faust AS JSONB))"
)
public class Case {
    @Id
    Integer id;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonStringArrayConverter.class)
    List<String> relatedFausts;

    public Integer getId() {
        return id;
    }

    public List<String> getRelatedFausts() {
        return relatedFausts;
    }
}
