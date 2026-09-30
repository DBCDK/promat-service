package dk.dbc.promat.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// Where Metakompas and Buggi selections are registered during the switch from Metakompasset to
// Promat. Set per environment by METAKOMPAS_REGISTRATION; removed again once every environment
// runs PROMAT.
@ApplicationScoped
public class MetakompasRegistration {
    public enum Mode {
        // Reviewers register in Metakompasset; CaseInformationUpdater polls the records for it
        METAKOMPASSET,
        // Reviewers register in Promat, through the tasks/{taskId}/metakompas|buggi endpoints
        PROMAT
    }

    @Inject
    @ConfigProperty(name = "METAKOMPAS_REGISTRATION", defaultValue = "METAKOMPASSET")
    Mode mode;

    // For CDI
    public MetakompasRegistration() {}

    public MetakompasRegistration(Mode mode) {
        this.mode = mode;
    }

    public boolean isMetakompasset() {
        return mode == Mode.METAKOMPASSET;
    }

    public boolean isPromat() {
        return mode == Mode.PROMAT;
    }
}
