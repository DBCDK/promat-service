package dk.dbc.promat.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

// Single-row table: the last full read of the taxonomy Kafka topic, kept around purely so a
// service restart (or a temporarily unreachable Kafka) still has a taxonomy to serve, instead
// of starting from empty. See ScheduledTaxonomyKafkaSync (writer) and DbTaxonomyBuilder
// (reader) for how this is used - `data` holds the subjects as a plain JSON array (the same
// shape the Kafka topic itself uses), read back and deserialized by the application rather
// than queried at the SQL level, so a plain `text` column is used instead of `jsonb`.
@Entity
@Table(name = "taxonomy_snapshot")
public class TaxonomySnapshot {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String data;

    @Column(name = "subject_count", nullable = false)
    private int subjectCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getSubjectCount() {
        return subjectCount;
    }

    public void setSubjectCount(int subjectCount) {
        this.subjectCount = subjectCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TaxonomySnapshot withId(Integer id) {
        this.id = id;
        return this;
    }

    public TaxonomySnapshot withData(String data) {
        this.data = data;
        return this;
    }

    public TaxonomySnapshot withSubjectCount(int subjectCount) {
        this.subjectCount = subjectCount;
        return this;
    }

    public TaxonomySnapshot withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaxonomySnapshot that = (TaxonomySnapshot) o;

        return subjectCount == that.subjectCount
                && Objects.equals(id, that.id)
                && Objects.equals(data, that.data)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data, subjectCount, updatedAt);
    }

    @Override
    public String toString() {
        return "TaxonomySnapshot{" +
                "id=" + id +
                ", subjectCount=" + subjectCount +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
