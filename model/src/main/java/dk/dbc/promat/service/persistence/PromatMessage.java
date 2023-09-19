package dk.dbc.promat.service.persistence;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@NamedQueries({
        @NamedQuery(
                name = PromatMessage.GET_MESSAGES_FOR_CASE,
                query = PromatMessage.GET_MESSAGES_FOR_CASE_QUERY
        ),
        @NamedQuery(
                name = PromatMessage.UPDATE_READ_STATE,
                query = PromatMessage.UPDATE_READ_STATE_QUERY
        ),
        @NamedQuery(
                name = PromatMessage.GET_NEWS_FOR_CASE,
                query = PromatMessage.GET_NEWS_FOR_CASE_QUERY
        )})
@Entity
public class PromatMessage {
    public enum Direction {
        REVIEWER_TO_EDITOR,
        EDITOR_TO_REVIEWER
    }

    @Embeddable
    public static class Author {
        @Column(name = "authorid")
        Integer id;

        @Column(name = "authorfirstname")
        String firstname;

        @Column(name = "authorlastname")
        String lastname;

        public Integer getId() {
            return id;
        }

        public void setId(Integer authorId) {
            this.id = authorId;
        }

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String authorFirstname) {
            this.firstname = authorFirstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String authorLastname) {
            this.lastname = authorLastname;
        }

        public Author withAuthorId(Integer authorId) {
            this.id = authorId;
            return this;
        }

        public Author withAuthorFirstname(String authorFirstname) {
            this.firstname = authorFirstname;
            return this;
        }

        public Author withAuthorLastname(String authorLastname) {
            this.lastname = authorLastname;
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
            return Objects.equals(id, author.id) &&
                    Objects.equals(firstname, author.firstname) &&
                    Objects.equals(lastname, author.lastname);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, firstname, lastname);
        }

        @Override
        public String toString() {
            return "Author{" +
                    "id=" + id +
                    ", firstname='" + firstname + '\'' +
                    ", lastname='" + lastname + '\'' +
                    '}';
        }
    }

    public static final String TABLE_NAME = "promatmessage";
    public static final String GET_MESSAGES_FOR_CASE = "PromatMessage.getMessagesForCase";
    public static final String GET_MESSAGES_FOR_CASE_QUERY =
            "SELECT promatmessage FROM PromatMessage promatmessage " +
                    "WHERE promatmessage.caseId = :caseId " +
                    "AND NOT promatmessage.isDeleted " +
                    "ORDER BY promatmessage.id DESC";
    public static final String UPDATE_READ_STATE = "PromatMessage.updateReadState";
    public static final String UPDATE_READ_STATE_QUERY =
            "UPDATE PromatMessage promatMessage SET promatMessage.isRead = :isRead " +
                    "WHERE promatMessage.caseId = :caseId AND " +
                    "promatMessage.direction = :direction";
    public static final String GET_NEWS_FOR_CASE = "PromatMessage.getNewsForCase";
    public static final String GET_NEWS_FOR_CASE_QUERY =
            "SELECT promatmessage FROM PromatMessage promatmessage " +
                    "WHERE promatmessage.caseId = :caseId AND promatmessage.direction = :direction " +
                    "AND NOT promatmessage.isRead " +
                    "AND NOT promatmessage.isDeleted";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    protected Author author;

    private Integer caseId;

    private String messageText;

    private LocalDateTime created;

    private Boolean isRead;

    private Boolean isDeleted = false;

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

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
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

    public PromatMessage withCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public PromatMessage withIsRead(Boolean isRead) {
        this.isRead = isRead;
        return this;
    }

    // Deliberately missing 'withIsDeleted()' since it makes no sense
    // to create a new message that is never to be shown!

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
                Objects.equals(isDeleted, aMessage.isDeleted) &&
                direction == aMessage.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, author, caseId, messageText, created, isRead, isDeleted, direction);
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
                ", isRead=" + isDeleted +
                ", direction=" + direction +
                '}';
    }
}
