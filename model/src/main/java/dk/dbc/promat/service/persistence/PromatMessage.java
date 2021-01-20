package dk.dbc.promat.service.persistence;

import java.time.LocalDate;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@NamedQueries({
        @NamedQuery(
                name = PromatMessage.GET_MESSAGES_FOR_CASE,
                query = PromatMessage.GET_MESSAGES_FOR_CASE_QUERY
        ),
        @NamedQuery(
                name = PromatMessage.UPDATE_READ_STATE,
                query = PromatMessage.UPDATE_READ_STATE_QUERY
        )})
@Entity
public class PromatMessage {
    public enum Direction {
        REVIEWER_TO_EDITOR,
        EDITOR_TO_REVIEWER
    }

    @Embeddable
    public static class Author {
        Integer authorId;
        String authorFirstname;
        String authorLastname;

        public Integer getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Integer authorId) {
            this.authorId = authorId;
        }

        public String getAuthorFirstname() {
            return authorFirstname;
        }

        public void setAuthorFirstname(String authorFirstname) {
            this.authorFirstname = authorFirstname;
        }

        public String getAuthorLastname() {
            return authorLastname;
        }

        public void setAuthorLastname(String authorLastname) {
            this.authorLastname = authorLastname;
        }

        public Author withAuthorId(Integer authorId) {
            this.authorId = authorId;
            return this;
        }

        public Author withAuthorFirstname(String authorFirstname) {
            this.authorFirstname = authorFirstname;
            return this;
        }

        public Author withAuthorLastname(String authorLastname) {
            this.authorLastname = authorLastname;
            return this;
        }

        public static Author fromPromatUser(PromatUser user) {
            if (user == null) {
                return null;
            }
            return new Author()
                    .withAuthorId(user.getId())
                    .withAuthorFirstname(user.getFirstName())
                    .withAuthorLastname(user.getLastName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Author)) {
                return false;
            }
            Author author = (Author) o;
            return Objects.equals(authorId, author.authorId) &&
                    Objects.equals(authorFirstname, author.authorFirstname) &&
                    Objects.equals(authorLastname, author.authorLastname);
        }

        @Override
        public int hashCode() {
            return Objects.hash(authorId, authorFirstname, authorLastname);
        }

        @Override
        public String toString() {
            return "Author{" +
                    "authorId=" + authorId +
                    ", authorFirstname='" + authorFirstname + '\'' +
                    ", authorLastname='" + authorLastname + '\'' +
                    '}';
        }
    }

    public static final String TABLE_NAME = "promatmessage";
    public static final String GET_MESSAGES_FOR_CASE = "PromatMessage.getMessagesForCase";
    public static final String GET_MESSAGES_FOR_CASE_QUERY =
            "SELECT promatmessage FROM PromatMessage promatmessage WHERE promatmessage.caseId = :caseId";
    public static final String UPDATE_READ_STATE = "PromatMessage.updateReadState";
    public static final String UPDATE_READ_STATE_QUERY =
            "UPDATE PromatMessage promatMessage SET promatMessage.isRead = :isRead " +
                    "WHERE promatMessage.caseId = :caseId AND " +
                    "promatMessage.direction = :direction";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    protected Author author;

    private Integer caseId;

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

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
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

    public PromatMessage withAuthor(Author author) {
        this.author = author;
        return this;
    }

    public PromatMessage withCaseId(Integer caseId) {
        this.caseId = caseId;
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
                Objects.equals(author, aMessage.author) &&
                Objects.equals(caseId, aMessage.caseId) &&
                Objects.equals(messageText, aMessage.messageText) &&
                Objects.equals(created, aMessage.created) &&
                Objects.equals(isRead, aMessage.isRead) &&
                direction == aMessage.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, author, caseId, messageText, created, isRead, direction);
    }

    @Override
    public String toString() {
        return "PromatMessage{" +
                "id=" + id +
                ", author=" + author +
                ", caseId=" + caseId +
                ", messageText='" + messageText + '\'' +
                ", created=" + created +
                ", isRead=" + isRead +
                ", direction=" + direction +
                '}';
    }
}
