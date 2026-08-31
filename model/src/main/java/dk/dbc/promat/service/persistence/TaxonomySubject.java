package dk.dbc.promat.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

// See TaxonomyCategory.java for a walkthrough of @Entity/@Table/@Column basics - this file
// focuses on the things that are different/new here: a natural (not database-generated)
// primary key, and a custom type converter for an array field.
@Entity
@Table(name = "taxonomy_subject")
public class TaxonomySubject {

    // Fields
    // No @GeneratedValue here, unlike TaxonomyCategory's id - this primary key is a "natural
    // key": it comes from upstream data (the taxonomy subject's own id in the source Kafka
    // messages/rawrepo records), not something Postgres invents for us. That means callers
    // MUST explicitly set id (e.g. via withId(...)) before persisting - if you forget, JPA
    // will try to insert a row with a null primary key and the database will reject it.
    @Id
    private Integer id;

    @Column(nullable = false)
    private String title;

    // note is a Postgres `text[]` (native array column, see the "text[]" columnDefinition
    // below) but Java has no built-in mapping from a String[] field to a SQL array type that
    // works reliably across every JPA provider/driver combination. @Convert tells JPA
    // "don't try to map this field yourself - hand it to StringArrayConverter instead, both
    // when reading a row (bytes/array from the database -> String[] for this field) and when
    // writing one (String[] -> whatever the database driver needs)". See
    // StringArrayConverter.java for what actually happens in that translation, and why its
    // declared types matter more than you'd expect.
    @Column(nullable = false, columnDefinition = "text[]")
    @Convert(converter = StringArrayConverter.class)
    private String[] note = new String[0];

    @Column(name = "often_used", nullable = false)
    private boolean oftenUsed = false;

    // No @Column annotation at all is valid too - JPA falls back to "map this field to a
    // column with the same name as the field" (ref -> ref), nullable by default. Annotations
    // here are only needed when you want to override that default behaviour somehow.
    private String ref;

    // @ManyToOne + @JoinColumn: many TaxonomySubject rows can point at one TaxonomyCategory.
    // Unlike TaxonomyCategory's self-referencing parent, this points at a *different* entity
    // class - the more common/typical shape of a @ManyToOne relationship. nullable = false
    // here (and NOT NULL in the migration) means every subject must belong to a category;
    // there's also a database trigger (see migration V51) that additionally checks the
    // referenced category is a leaf, not a group node - JPA/@Column can express "must not be
    // null", but "must satisfy this other business rule" needs a real database constraint or
    // application-level check, which is why the trigger exists alongside this annotation.
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TaxonomyCategory category;

    @Column(name = "source_record_id")
    private String sourceRecordId;

    // `= LocalDateTime.now()` is a field initializer - it runs once, when a `new
    // TaxonomySubject()` is constructed, giving every new instance a sensible default before
    // any setter is called. It gets overwritten immediately by ScheduledTaxonomyKafkaSync's
    // upsert logic anyway, but having a non-null default avoids surprises for any other code
    // that might construct one without explicitly setting this field.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String[] getNote() {
        return note;
    }

    public void setNote(String[] note) {
        this.note = note;
    }

    public boolean isOftenUsed() {
        return oftenUsed;
    }

    public void setOftenUsed(boolean oftenUsed) {
        this.oftenUsed = oftenUsed;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public TaxonomyCategory getCategory() {
        return category;
    }

    public void setCategory(TaxonomyCategory category) {
        this.category = category;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Fluent setters - see TaxonomyCategory.java for what this pattern is and why it exists.
    public TaxonomySubject withId(Integer id) {
        this.id = id;
        return this;
    }

    public TaxonomySubject withTitle(String title) {
        this.title = title;
        return this;
    }

    public TaxonomySubject withNote(String[] note) {
        this.note = note;
        return this;
    }

    public TaxonomySubject withOftenUsed(boolean oftenUsed) {
        this.oftenUsed = oftenUsed;
        return this;
    }

    public TaxonomySubject withRef(String ref) {
        this.ref = ref;
        return this;
    }

    public TaxonomySubject withCategory(TaxonomyCategory category) {
        this.category = category;
        return this;
    }

    public TaxonomySubject withSourceRecordId(String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
        return this;
    }

    public TaxonomySubject withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    // equals, hashCode, toString - see TaxonomyCategory.java for the general explanation of
    // why these exist and the equals()/hashCode() contract. Two things specific to this class:
    //
    // 1. `note` is a String[], and arrays in Java do NOT override equals()/hashCode() in a
    //    useful way by default - Arrays.equals(a, b) compares elements, but a.equals(b) (or
    //    Objects.equals(a, b)) would compare object identity instead, which is almost never
    //    what you want for an array field. Arrays.equals/Arrays.hashCode are the standard
    //    fix, which is why `note` is handled separately from everything else below rather
    //    than folded into the Objects.equals/Objects.hash calls.
    // 2. `category` is compared by its id (category.getId()), same reasoning as
    //    TaxonomyCategory's `parent` field - avoids recursively comparing the whole category
    //    object, and null-safely handles a subject that (in theory) hasn't had its category
    //    set yet.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaxonomySubject that = (TaxonomySubject) o;

        return oftenUsed == that.oftenUsed
                && Objects.equals(id, that.id)
                && Objects.equals(title, that.title)
                && Arrays.equals(note, that.note)
                && Objects.equals(ref, that.ref)
                && Objects.equals(category == null ? null : category.getId(), that.category == null ? null : that.category.getId())
                && Objects.equals(sourceRecordId, that.sourceRecordId)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        // Objects.hash(...) handles everything except `note`; Arrays.hashCode(note) is mixed
        // in afterwards using the same "31 * result + ..." technique Java's own generated
        // hashCode()s conventionally use (31 is prime, which spreads hash values out well and
        // is cheap for the JIT to compute via a bit-shift instead of true multiplication).
        int result = Objects.hash(id, title, oftenUsed, ref, category == null ? null : category.getId(), sourceRecordId, updatedAt);
        result = 31 * result + Arrays.hashCode(note);
        return result;
    }

    @Override
    public String toString() {
        return "TaxonomySubject{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", note=" + java.util.Arrays.toString(note) +
                ", oftenUsed=" + oftenUsed +
                ", ref='" + ref + '\'' +
                ", category=" + (category != null ? category.getId() : null) +
                ", sourceRecordId='" + sourceRecordId + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
