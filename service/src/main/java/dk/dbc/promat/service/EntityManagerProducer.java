/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import dk.dbc.promat.service.persistence.PromatEntityManager;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
