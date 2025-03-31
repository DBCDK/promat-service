package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import java.util.Date;

@SqlResultSetMapping(
        name = "PromatUser.UserRoleMapping",
        classes = {
                @ConstructorResult(
                        targetClass = dk.dbc.promat.service.dto.UserRole.class,
                        columns = {
                                @ColumnResult(name = "id"),
                                @ColumnResult(name = "role"),
                                @ColumnResult(name = "localid", type=String.class)})})
@NamedNativeQuery(
        name = PromatUser.GET_USER_ROLE,
        query = PromatUser.GET_USER_ROLE_QUERY,
        resultSetMapping = "PromatUser.UserRoleMapping")
@NamedNativeQuery(
        name = PromatUser.GET_USER_ROLE_BY_AGENCY_AND_USERID,
        query = PromatUser.GET_USER_ROLE_BY_AGENCY_AND_USERID_QUERY,
        resultSetMapping = "PromatUser.UserRoleMapping")
@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="role")
public abstract class PromatUser {
    public static final String GET_USER_ROLE =
            "PromatUser.getUserRole";
    public static final String GET_USER_ROLE_QUERY =
            "SELECT id,role,CAST(paycode AS TEXT) AS localid FROM promatuser WHERE culrId=?1";
    public static final String GET_USER_ROLE_BY_AGENCY_AND_USERID =
            "PromatUser.getUserRoleByAuthToken";
    public static final String GET_USER_ROLE_BY_AGENCY_AND_USERID_QUERY =
            "SELECT id,role,CAST(paycode AS TEXT) AS localid FROM promatuser WHERE userId=?1 AND agency=?2";

    public enum Role {
        EDITOR, REVIEWER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({CaseView.Summary.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class, EditorView.Summary.class, EditorView.Editor.class})
    protected Integer id;

    // update/insert is managed by discriminator mechanics
    @Column(nullable = false, insertable = false, updatable = false)
    @Convert(converter = RoleConverter.class)
    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class, EditorView.Summary.class, EditorView.Editor.class})
    protected Role role;

    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class, EditorView.Summary.class, EditorView.Editor.class})
    protected boolean active;

    @JsonView({CaseView.Summary.class, CaseView.Case.class, ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected String culrId;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class, EditorView.Summary.class, EditorView.Editor.class})
    protected String firstName;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class, EditorView.Summary.class, EditorView.Editor.class})
    protected String lastName;

    @JsonView({ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected String email;

    @JsonView({ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected String phone;

    @JsonView({ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected Date activeChanged;

    @JsonView({ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected Date deactivated;

    @JsonView({ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected String agency;

    @JsonView({ReviewerView.Reviewer.class, EditorView.Editor.class})
    protected String userId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCulrId() {
        return culrId;
    }

    public void setCulrId(String culrId) {
        this.culrId = culrId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getActiveChanged()  {
        return activeChanged;
    }

    public void setActiveChanged(Date activeChanged) {
        this.activeChanged = activeChanged;
    }

    public Date getDeactivated()  {
        return deactivated;
    }

    public void setDeactivated(Date deactivated) {
        this.deactivated = deactivated;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "PromatUser{" +
                "id=" + id +
                ", role=" + role +
                ", active=" + active +
                ", culrId='" + culrId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", activeChanged=" + activeChanged +
                ", deactivated=" + deactivated +
                ", agency='" + agency + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
