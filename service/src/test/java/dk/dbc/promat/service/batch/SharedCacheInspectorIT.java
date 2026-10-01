package dk.dbc.promat.service.batch;

import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.persistence.Reviewer;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.identitymaps.IdentityMap;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.jpa.JpaEntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

public class SharedCacheInspectorIT extends ContainerTest {

    @Test
    public void reportsLockedEntryAndItsOwner() {
        SharedCacheInspector inspector = new SharedCacheInspector();
        inspector.entityManagerFactory = entityManager.getEntityManagerFactory();

        // Loading the reviewer puts it in the shared cache
        entityManager.find(Reviewer.class, 1);
        CacheKey key = findCacheKey(1);

        assertThat("not locked before", inspector.findLockedEntries(), not(hasItem(containsString("Reviewer id 1,"))));

        key.acquire();
        try {
            List<String> locked = inspector.findLockedEntries();
            assertThat("locked entry is reported", locked, hasItem(containsString("Reviewer id 1, write lock depth 1")));
            assertThat("owner is reported", locked, hasItem(containsString(
                    "owned by thread '" + Thread.currentThread().getName() + "' (alive: true)")));
        } finally {
            key.release();
        }

        assertThat("not locked after", inspector.findLockedEntries(), not(hasItem(containsString("Reviewer id 1,"))));
    }

    // The entity manager hands out working copies, so look the cached original up by primary key
    private CacheKey findCacheKey(int reviewerId) {
        AbstractSession session = entityManager.getEntityManagerFactory()
                .unwrap(JpaEntityManagerFactory.class).getServerSession();
        IdentityMap identityMap = session.getIdentityMapAccessorInstance()
                .getIdentityMap(session.getDescriptor(Reviewer.class));
        Enumeration<CacheKey> keys = identityMap.keys();
        while (keys.hasMoreElements()) {
            CacheKey key = keys.nextElement();
            if (Integer.valueOf(reviewerId).equals(key.getKey())) {
                return key;
            }
        }
        throw new IllegalStateException("Reviewer " + reviewerId + " is not in the shared cache");
    }
}
