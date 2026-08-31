package dk.dbc.promat.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// The "many" side of BuggiOptionGroup's one-to-many - see that class for the relationship
// explanation, and TaxonomyCategory.java for @Entity/@Table/@Id basics.
@Entity
@Table(name = "buggi_option")
public class BuggiOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // This is the OWNING side of the relationship: @JoinColumn here is what actually creates
    // and manages the group_id foreign key column on buggi_option. Setting this field (via
    // setGroup(...)) and persisting is what links an option to a group - the @OneToMany back
    // on BuggiOptionGroup is read-only from JPA's perspective, purely a convenience for
    // navigating "group -> its options" without writing a query.
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private BuggiOptionGroup group;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BuggiOptionGroup getGroup() {
        return group;
    }

    public void setGroup(BuggiOptionGroup group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
