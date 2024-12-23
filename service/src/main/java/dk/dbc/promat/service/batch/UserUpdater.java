package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.persistence.Address;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.ReviewerDataStash;
import dk.dbc.promat.service.templating.NotificationFactory;
import dk.dbc.promat.service.templating.model.ReviewerDataChanged;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Stateless
public class UserUpdater {
    static final Metadata userUpdateFailureCounterMetadata = Metadata.builder().withName("promat_service_userupdater_update_failures").withDescription("Number of failing update attempts").withUnit("failures").build();
    private static final Logger LOGGER = LoggerFactory.getLogger(UserUpdater.class);
    private static final ObjectMapper MAPPER = new JsonMapperProvider().getObjectMapper();

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    MetricRegistry metricRegistry;

    @Inject
    NotificationFactory notificationFactory;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deactivateEditor(Editor editor) {

        try {
            editor.setEmail("");
            editor.setPhone("");
            editor.setDeactivated(Date.from(ZonedDateTime.now().toInstant()));
        } catch (Exception e) {
            LOGGER.error("Unable to update editor with id {}: {}", editor.getId(), e.getMessage());
            metricRegistry.counter(userUpdateFailureCounterMetadata).inc();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deactivateReviewer(Reviewer reviewer) {

        try {
            reviewer.setEmail("");
            reviewer.setPhone("");
            reviewer.setPrivateEmail("");
            reviewer.setPrivatePhone("");
            reviewer.setAddress(new Address());
            reviewer.setPrivateAddress(new Address());
            reviewer.setDeactivated(Date.from(ZonedDateTime.now().toInstant()));
        } catch (Exception e) {
            LOGGER.error("Unable to update reviewer with id {}: {}", reviewer.getId(), e.getMessage());
            metricRegistry.counter(userUpdateFailureCounterMetadata).inc();
        }
    }

    public boolean isReviewerEditingFinished(int id, int checkInterval) {
        Reviewer reviewer = entityManager.find(Reviewer.class, id);
        boolean finished = reviewer.getLastChanged().plusSeconds(checkInterval).isBefore(LocalDateTime.now());

        LOGGER.info("Checking if notification needs to be made now. reviewer id {}: Result:{}", id, finished);
        return finished;
    }

    public void makeReviewerChangedNotification(Reviewer stashedReviewer) throws NotificationFactory.ValidateException {
        Reviewer changedReviewer = entityManager.find(Reviewer.class, stashedReviewer.getId());
        ReviewerDataChanged reviewerDataChanged =new ReviewerDataChanged().withNewReviewer(changedReviewer).withReviewer(stashedReviewer);
        Notification notification = notificationFactory.notificationOf(reviewerDataChanged);
        if (notification != null) {
            entityManager.persist(notification);
        }

    }

    public void processUserDataChanges() {
        try {
            List<ReviewerDataStash> stashes = entityManager.createQuery("select r from ReviewerDataStash r", ReviewerDataStash.class).getResultList();
            for (ReviewerDataStash stash : stashes) {
                LOGGER.info("Processing stash: {}", stash);
                Reviewer stashedReviewer = MAPPER.readValue(stash.getReviewer(), Reviewer.class);
                if (isReviewerEditingFinished(stashedReviewer.getId(), UserEditConfig.getUserEditTimeOut())) {
                    makeReviewerChangedNotification(stashedReviewer);
                    entityManager.remove(stash);
                }
            }
        } catch (JsonProcessingException | NotificationFactory.ValidateException e) {
            LOGGER.error("Unable to process stashes: ", e);
            throw new RuntimeException(e);
        }
    }
}
