package dk.dbc.promat.service.batch;

import dk.dbc.connector.openformat.OpenFormatConnectorException;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.templating.NotificationFactory;
import dk.dbc.promat.service.templating.model.DeadlinePassedMail;
import dk.dbc.promat.service.templating.model.EarlyReminderMail;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class Reminders {
    private static final Logger LOGGER = LoggerFactory.getLogger(Reminders.class);

    // Little helper when testing.
    // Allows mocking LocalDate.now()
    protected static class Today {
        protected LocalDate toDay() {
            return LocalDate.now();
        }
    }

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @EJB
    NotificationFactory notificationFactory;

    protected Today todayProvider = new Today();

    private boolean outsideInterval(LocalDate reminder, LocalDate deadline) {

        // Last reminder was sent precisely three days before deadline.
        LocalDate supposedReminderDate = deadline.minusDays(3L);
        if (reminder.isEqual(supposedReminderDate)) {
            return false;
        }

        // Reminder dates to an era earlier than three days before deadline OR
        // reminder is later than deadline, in which case this must be handled in
        // "You're" late section.
        return reminder.isBefore(supposedReminderDate) || reminder.isAfter(deadline);
    }

    /**
     * Takes care of producing a reminder mail to reviewer on each case, when:
     * - Deadline is less than three days away
     * - Everyday when deadline is passed.
     */
    public void processReminders() {
        LOGGER.info("Looking for cases where reminders to reviewers needs to sent.");
        TypedQuery<PromatCase> query = entityManager
                .createNamedQuery(PromatCase.GET_CASES_FOR_REMINDERS_CHECK_NAME, PromatCase.class);
        List<PromatCase> cases = query.getResultList();
        LocalDate today = todayProvider.toDay();
        for (PromatCase pc : cases) {
            processReminder(pc, today);
        }
        entityManager.flush();

    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processReminder(PromatCase pc, LocalDate today) {

        // Only cases with deadline
        LocalDate deadline = pc.getDeadline();
        if (deadline != null) {
            try {

                // Case 1: Today is within three days of deadline, and reminder is not sent.
                long daysToDeadline = ChronoUnit.DAYS.between(today, deadline);

                if (daysToDeadline > 0 && daysToDeadline <= 3) {

                    // There is no reminder sent on this case OR the reminder is a relict
                    // from another lifecycle of the case. (That is: Deadline has been moved).
                    LocalDate reminder = pc.getReminderSent();
                    if (reminder == null || outsideInterval(reminder, deadline)) {
                        Notification notification = notificationFactory
                                .notificationOf(new EarlyReminderMail().withPromatCase(pc));
                        entityManager.persist(notification);
                        pc.setReminderSent(today);
                        return;
                    }
                }

                // Case 2: Today is AFTER the deadline. A "You're late!" reminder has to be sent.
                if (today.isAfter(deadline)) {

                    // Has mail already been sent today?
                    LocalDate reminder = pc.getReminderSent();
                    if (reminder == null || !reminder.equals(today)) {
                        Notification notification = notificationFactory
                                .notificationOf(new DeadlinePassedMail().withPromatCase(pc));
                        entityManager.persist(notification);
                        pc.setReminderSent(today);
                    }
                }

            } catch (OpenFormatConnectorException | NotificationFactory.ValidateException e) {
                LOGGER.error("Caught exception during 'processReminder': {}", e.getMessage());
            }
        }
    }
}
