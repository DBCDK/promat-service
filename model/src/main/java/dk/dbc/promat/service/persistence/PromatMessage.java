package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class PromatMessage {
    public enum Direction {
        REVIEWER_TO_EDITOR,
        EDITOR_TO_REVIEWER
    }

    public static final String TABLE_NAME = "promatmessage";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JsonFilter("idAndName")
    private Reviewer reviewer;

    @OneToOne
    @JsonFilter("idAndName")
    private Editor editor;

    @OneToOne
    @JsonFilter("idAndTitle")
    private PromatCase promatCase;

    private String messageText;

    private LocalDate created;

    private Boolean isRead;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Reviewer getReviewer() {
        return reviewer;
    }

    public void setReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
    }

    public Editor getEditor() {
        return editor;
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    public PromatCase getPromatCase() {
        return promatCase;
    }

    public void setPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public PromatMessage withReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public PromatMessage withEditor(Editor editor) {
        this.editor = editor;
        return this;
    }

    public PromatMessage withPromatCase(PromatCase promatCase) {
        this.promatCase = promatCase;
        return this;
    }

    public PromatMessage withMessageText(String messageText) {
        this.messageText = messageText;
        return this;
    }

    public PromatMessage withCreated(LocalDate created) {
        this.created = created;
        return this;
    }

    public PromatMessage withIsRead(Boolean isRead) {
        this.isRead = isRead;
        return this;
    }
    public PromatMessage withDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PromatMessage)) {
            return false;
        }
        PromatMessage aMessage = (PromatMessage) o;
        return Objects.equals(id, aMessage.id) &&
                Objects.equals(reviewer, aMessage.reviewer) &&
                Objects.equals(editor, aMessage.editor) &&
                Objects.equals(promatCase, aMessage.promatCase) &&
                Objects.equals(messageText, aMessage.messageText) &&
                Objects.equals(created, aMessage.created) &&
                Objects.equals(isRead, aMessage.isRead) &&
                direction == aMessage.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reviewer, editor, promatCase, messageText, created, isRead, direction);
    }

    @Override
    public String toString() {
        return "PromatMessage{" +
                "id=" + id +
                ", reviewer=" + reviewer +
                ", editor=" + editor +
                ", promatCase=" + promatCase +
                ", messageText='" + messageText + '\'' +
                ", created=" + created +
                ", isRead=" + isRead +
                ", direction=" + direction +
                '}';
    }
}
