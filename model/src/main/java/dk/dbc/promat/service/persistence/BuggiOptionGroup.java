package dk.dbc.promat.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

// A simpler, flat sibling of TaxonomyCategory/TaxonomySubject: one group (e.g. "Stemning")
// has many options (e.g. "sjov", "trist") - a plain one-to-many, not a self-referencing tree,
// since Buggi's tag vocabulary only ever needs this one level of grouping. See
// TaxonomyCategory.java for detailed notes on @Entity/@Table/@Id/@Column basics, which apply
// here identically.
@Entity
@Table(name = "buggi_option_group")
public class BuggiOptionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "subfield_code", nullable = false)
    private String subfieldCode;

    @Column(name = "requires_nonzero_value", nullable = false)
    private boolean requiresNonzeroValue = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    // @OneToMany is the "inverse" (or "non-owning") side of the relationship whose "owning"
    // side is BuggiOption.group (a @ManyToOne with an actual @JoinColumn - see that class).
    // In JPA, a foreign key column only ever lives on ONE side of a relationship (here, on
    // buggi_option.group_id) - the entity on the "many" side always owns it. This @OneToMany
    // doesn't create or reference any column of its own; `mappedBy = "group"` just tells JPA
    // "populate this list by looking up BuggiOption rows whose `group` field points back at
    // me" whenever this field is fetched. Change options here and DON'T touch the matching
    // BuggiOption.group field, and nothing gets persisted - the owning side is what's saved.
    @OneToMany(mappedBy = "group")
    private List<BuggiOption> options = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubfieldCode() {
        return subfieldCode;
    }

    public void setSubfieldCode(String subfieldCode) {
        this.subfieldCode = subfieldCode;
    }

    public boolean isRequiresNonzeroValue() {
        return requiresNonzeroValue;
    }

    public void setRequiresNonzeroValue(boolean requiresNonzeroValue) {
        this.requiresNonzeroValue = requiresNonzeroValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<BuggiOption> getOptions() {
        return options;
    }

    public void setOptions(List<BuggiOption> options) {
        this.options = options;
    }
}
