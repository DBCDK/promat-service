package dk.dbc.promat.service.batch;

import dk.dbc.mail.Headers;
import dk.dbc.mail.MailManager;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.NotificationStatus;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class NotificationSender {
    @Inject
    @ConfigProperty(name = "MAIL_FROM", defaultValue = "promat@dbc.dk")
    String mailFrom;

    @Inject
    @ConfigProperty(name = "LU_MAILADDRESS", defaultValue = "lu@dbc.dk")
    String replyToAddress;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSender.class);

    static final Metadata mailCounterMetadata = Metadata.builder()
            .withName("promat_service_mailsender_counter")
            .withDescription("Number of mails sent.")
            .withType(MetricType.COUNTER)
            .withUnit("mails")
            .build();

    static final Metadata mailFailureGaugeMetadata = Metadata.builder()
            .withName("promat_service_mailsender_failures")
            .withDescription("Number of mails that promat backend service is currently unable to send.")
            .withType(MetricType.CONCURRENT_GAUGE)
            .withUnit("failures")
            .build();

    @Inject
    MailManager mailManager;


    @Inject
    @PromatEntityManager
    EntityManager entityManager;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void notifyMailRecipient(Notification notification) {
        try {
            mailManager.newMail()
                    .withRecipients(notification.getToAddress())
                    .withSubject(notification.getSubject())
                    .withBodyText(notification.getBodyText())
                    .withFromAddress(mailFrom)
                    .withReplyToAddress(replyToAddress)
                    .withHeaders(
                            new Headers()
                                    .withHeader("Content-type", "text/HTML; charset=UTF-8").build()
                    )
                    .build().send();
            notification.setStatus(NotificationStatus.DONE);
            metricRegistry.counter(mailCounterMetadata).inc();
        } catch (jakarta.mail.MessagingException e) {
            LOGGER.error("Unable to send mail due to MessagingException. Notification:{}",notification.toString());
            notification.setStatus(NotificationStatus.ERROR);
            metricRegistry.concurrentGauge(mailFailureGaugeMetadata).inc();
        } catch (Exception e) {
            LOGGER.error("Unable to send mail due to unexpected exception. Notification:{}",notification.toString());
            notification.setStatus(NotificationStatus.ERROR);
            metricRegistry.concurrentGauge(mailFailureGaugeMetadata).inc();
        }
    }

    public void resetMailFailuresGauge() {
        ConcurrentGauge gauge = metricRegistry.concurrentGauge(mailFailureGaugeMetadata);
        while (gauge.getCount() > 0) {
            gauge.dec();
        }
    }
}
