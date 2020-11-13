package dk.dbc.promat.service.persistence;

import dk.dbc.promat.service.templating.NotificationFactory;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;

@NamedQueries({
        @NamedQuery(
                name = Notification.SELECT_FROM_NOTIFCATION_QUEUE_NAME,
                query = Notification.SELECT_FROM_NOTIFCATION_QUEUE_QUERY,
                lockMode = LockModeType.PESSIMISTIC_WRITE
        )
})
@Entity
public class Notification {
    public static final String SELECT_FROM_NOTIFCATION_QUEUE_NAME = "select.from.notification.queue";
    public static final String SELECT_FROM_NOTIFCATION_QUEUE_QUERY =
            "SELECT notification FROM  Notification notification " +
                    "WHERE notification.status = :status " +
                    " ORDER BY notification.id asc ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String toAddress;
    String subject;
    String bodyText;

    @Column(updatable = false)
    LocalDateTime created;


    @Enumerated(EnumType.ORDINAL)
    NotificationStatus status;

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Notification withToAddress(String toAddress) {
        this.toAddress = toAddress;
        return this;
    }

    public Notification withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public Notification withBodyText(String bodyText) {
        this.bodyText = bodyText;
        return this;
    }

    public Notification withCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public Notification withStatus(NotificationStatus status) {
        this.status = status;
        return this;
    }

    public static Notification of(NotificationType notifcationType, PromatCase aCase) {
        return NotificationFactory.getInstance().of(notifcationType, aCase);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notification that = (Notification) o;

        if (!id.equals(that.id)) return false;
        if (!toAddress.equals(that.toAddress)) return false;
        if (!subject.equals(that.subject)) return false;
        if (!bodyText.equals(that.bodyText)) return false;
        if (!created.equals(that.created)) return false;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + toAddress.hashCode();
        result = 31 * result + subject.hashCode();
        result = 31 * result + bodyText.hashCode();
        result = 31 * result + created.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", toAddress='" + toAddress + '\'' +
                ", subject='" + subject + '\'' +
                ", bodyText='" + bodyText + '\'' +
                ", created=" + created +
                ", status=" + status +
                '}';
    }
}
