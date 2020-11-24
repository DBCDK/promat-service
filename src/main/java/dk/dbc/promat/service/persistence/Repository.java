/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

@Stateless
public class Repository {
    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    /**
     * Locks a single entity for exclusive access.
     * This is a blocking operation.
     * @param entityClass class of the entity
     * @param primaryKey the primary key of the entity
     * @param <T> the type
     * @return locked entity
     */
    public <T> T getExclusiveAccessToEntity(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Locks a table for exclusive access
     * This is a blocking operation.
     * @param tableName name of the table
     */
    public void getExclusiveAccessToTable(String tableName) {
        final Query lockQuery = entityManager.createNativeQuery(
                String.format("LOCK TABLE %s IN EXCLUSIVE MODE", tableName)
        );
        lockQuery.executeUpdate();
    }
}
