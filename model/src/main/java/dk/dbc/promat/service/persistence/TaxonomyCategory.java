package dk.dbc.promat.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

// @Entity is JPA's (Jakarta Persistence API) marker that says "this plain Java class also
// represents a database row". The JPA provider (this project uses EclipseLink, configured
// in persistence.xml) uses reflection over the annotated fields below to generate SQL for
// you - you never hand-write INSERT/UPDATE/SELECT for this class.
//
// @Table tells JPA which physical table this entity maps to (taxonomy_category, created in
// migration V51). Without @Table, JPA would default to using the class name itself as the
// table name - being explicit here also lets us declare a composite uniqueConstraint that
// can't be expressed via a single field's annotations alone: no two categories may share
// the same (parent_id, name) pair, i.e. a parent can't have two children with the same name.
@Entity
@Table(name = "taxonomy_category", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_id", "name"})
})
public class TaxonomyCategory {

    // Fields
    // @Id marks this field as the entity's primary key - JPA requires every @Entity to have
    // exactly one (or a composite key, which is a more advanced topic not used here).
    // @GeneratedValue(strategy = IDENTITY) means "let the database assign the value" - this
    // maps to Postgres's `serial`/identity column (see the migration), which auto-increments
    // on INSERT. Because of this, `id` is null on a freshly-constructed object and only gets
    // a real value after entityManager.persist(...) actually writes the row.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // @ManyToOne declares a relationship to another entity - "many TaxonomyCategory rows can
    // point at one parent TaxonomyCategory row". This is a *self-referencing* relationship:
    // the class relates to itself, which is exactly how you model an arbitrary-depth tree
    // (category -> parent -> parent's parent -> ... -> a root category with parent == null)
    // in a relational database using a single table, instead of one table per tree level.
    // @JoinColumn says which physical column on THIS table stores the foreign key
    // (parent_id, referencing this same table's id column) - without it JPA would guess a
    // default column name that might not match the migration.
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TaxonomyCategory parent;

    // A plain @Column with no `name` attribute maps to a column with the same name as the
    // Java field, lowercased (Postgres folds unquoted identifiers to lowercase) - so `name`
    // here maps to the `name` column. nullable = false is a *hint* JPA can use for schema
    // generation, but since this project turns schema generation off (see persistence.xml's
    // jakarta.persistence.schema-generation.database.action=none) the migration's own
    // `NOT NULL` is what actually enforces this at the database level; the annotation here is
    // mostly documentation for anyone reading the entity.
    @Column(nullable = false)
    private String name;

    // Java field names conventionally use camelCase (isLeaf), but SQL/Postgres convention is
    // snake_case (is_leaf) - @Column(name = "...") is how you bridge that mismatch instead of
    // renaming the Java field to something unnatural.
    @Column(name = "is_leaf", nullable = false)
    private boolean isLeaf = false;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Getters and setters
    // JPA (and most Java frameworks/tools built on the JavaBeans convention) expects plain
    // get/set accessor pairs like these - EclipseLink calls them via reflection to read and
    // write field values when loading/saving rows, even though the fields above are private.
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TaxonomyCategory getParent() {
        return parent;
    }

    public void setParent(TaxonomyCategory parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
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

    // Fluent setters
    // These "with*" methods aren't a JPA/Jakarta concept at all - they're a plain Java
    // convention (sometimes called the "fluent builder" or "wither" pattern) that lets you
    // construct and configure an object in one chained expression instead of several
    // statements, e.g.:
    //   new TaxonomyCategory().withName("ramme").withIsLeaf(false).withActive(true)
    // instead of:
    //   TaxonomyCategory c = new TaxonomyCategory();
    //   c.setName("ramme"); c.setIsLeaf(false); c.setActive(true);
    // Each method mutates `this` and then returns it, which is what makes chaining possible.
    public TaxonomyCategory withId(Integer id) {
        this.id = id;
        return this;
    }

    public TaxonomyCategory withParent(TaxonomyCategory parent) {
        this.parent = parent;
        return this;
    }

    public TaxonomyCategory withName(String name) {
        this.name = name;
        return this;
    }

    public TaxonomyCategory withIsLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
        return this;
    }

    public TaxonomyCategory withActive(boolean active) {
        this.active = active;
        return this;
    }

    public TaxonomyCategory withDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        return this;
    }

    // equals, hashCode, toString
    // These three overrides aren't JPA-specific either - they're standard java.lang.Object
    // methods every Java class inherits, and it's idiomatic to override them for classes
    // whose *values* matter for comparison (as opposed to just their identity/memory address,
    // which is what the default Object.equals/hashCode use).
    //
    // A subtlety worth understanding: `parent` is itself a TaxonomyCategory, so a naive
    // `Objects.equals(parent, that.parent)` would recursively call THIS SAME equals() method
    // on the parent, which would in turn look at *its* parent, and so on up the tree - not
    // wrong, but unnecessarily expensive and easy to get into trouble with if a cycle ever
    // existed. Comparing by `parent.getId()` instead treats "same parent id" as "same parent"
    // for equality purposes, without walking the whole ancestor chain. The `parent == null ?
    // null : parent.getId()` ternary exists purely to avoid a NullPointerException for
    // root categories, which have no parent at all.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaxonomyCategory that = (TaxonomyCategory) o;

        return isLeaf == that.isLeaf
                && active == that.active
                && Objects.equals(id, that.id)
                && Objects.equals(parent == null ? null : parent.getId(), that.parent == null ? null : that.parent.getId())
                && Objects.equals(name, that.name)
                && Objects.equals(displayOrder, that.displayOrder);
    }

    // The contract between equals() and hashCode() is strict: if two objects are equal(),
    // they MUST return the same hashCode() (the reverse isn't required - unequal objects are
    // still allowed to collide). This matters in practice because hash-based collections
    // (HashSet, HashMap) use hashCode() to pick a "bucket" first and equals() only to
    // disambiguate within that bucket - get this contract wrong and objects can vanish from
    // a HashSet or never be found by a HashMap.get() even though an "equal" one was put in.
    // Objects.hash(...) is a convenience that combines several values into one hash the same
    // way you'd otherwise do by hand with `31 * result + ...` (see TaxonomySubject for that
    // manual style, done here because `note` is an array and needs Arrays.hashCode).
    @Override
    public int hashCode() {
        return Objects.hash(id, parent == null ? null : parent.getId(), name, isLeaf, active, displayOrder);
    }

    // toString() is what gets printed by LOGGER.info("{}", category), in a debugger's
    // "watch" view, or by default if you forget to override it (which prints an unhelpful
    // ClassName@hexHashcode). Overriding it with the field values is purely a developer
    // convenience for logging/debugging - it has no effect on persistence or equality.
    @Override
    public String toString() {
        return "TaxonomyCategory{" +
                "id=" + id +
                ", parent=" + (parent != null ? parent.getId() : null) +
                ", name='" + name + '\'' +
                ", isLeaf=" + isLeaf +
                ", active=" + active +
                ", displayOrder=" + displayOrder +
                '}';
    }
}
