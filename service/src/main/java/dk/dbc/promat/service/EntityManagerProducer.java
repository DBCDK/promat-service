package dk.dbc.promat.service;

import dk.dbc.promat.service.persistence.PromatEntityManager;

import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * To obtain an {@link EntityManager} for the Promat database simply say
 * <pre>
 * {@literal @}Inject {@literal @}PromatEntityManager EntityManager em
 * </pre>
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class EntityManagerProducer {
    @Produces @PromatEntityManager
    @PersistenceContext(unitName="promatPU")
    private EntityManager promatEntityManager;
}
