package dk.dbc.promat.service.dto;

// One selectable Buggi option exposed by GET /buggi/options.
public class BuggiOption {
    private Integer id;
    private String name;

    public BuggiOption() {
    }

    public BuggiOption(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

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
}
