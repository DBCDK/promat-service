package dk.dbc.promat.service.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.identitymaps.IdentityMap;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.jpa.JpaEntityManagerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds the entries in the EclipseLink shared cache that are locked, and by which thread.
 * Used by {@link BatchJobLivenessCheck} when a batch job is stuck, to find out what orphans the
 * cache locks the jobs hang on (see ADR 0007). A thread dump cannot show this, because EclipseLink
 * tracks lock owners in its own fields, not in Java monitors.
 * Uses EclipseLink internals, and only reads.
 */
@ApplicationScoped
public class SharedCacheInspector {

    @PersistenceUnit(unitName = "promatPU")
    EntityManagerFactory entityManagerFactory;

    public List<String> findLockedEntries() {
        AbstractSession session = entityManagerFactory.unwrap(JpaEntityManagerFactory.class).getServerSession();
        // Subclasses in an inheritance hierarchy share their root's identity map - only visit each map once
        Set<IdentityMap> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        List<String> locked = new ArrayList<>();
        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            IdentityMap identityMap = session.getIdentityMapAccessorInstance().getIdentityMap(descriptor, true);
            if (identityMap == null || !visited.add(identityMap)) {
                continue;
            }
            Enumeration<CacheKey> keys = identityMap.keys();
            while (keys.hasMoreElements()) {
                CacheKey key = keys.nextElement();
                if (key.isAcquired() || key.getNumberOfReaders() > 0) {
                    locked.add(describe(descriptor, key));
                }
            }
        }
        return locked;
    }

    private static String describe(ClassDescriptor descriptor, CacheKey key) {
        Object object = key.getObject();
        String entity = object != null ? object.getClass().getSimpleName() : descriptor.getJavaClass().getSimpleName();
        Thread owner = key.getActiveThread();
        StringBuilder description = new StringBuilder()
                .append(entity).append(" id ").append(key.getKey())
                .append(", write lock depth ").append(key.getDepth())
                .append(", readers ").append(key.getNumberOfReaders());
        if (owner == null) {
            description.append(", no owning thread");
        } else {
            description.append(", owned by thread '").append(owner.getName()).append("'")
                    .append(" (alive: ").append(owner.isAlive()).append(")");
            if (owner.isAlive()) {
                description.append(". Owner stack:\n").append(Arrays.stream(owner.getStackTrace())
                        .map(element -> "\tat " + element)
                        .collect(Collectors.joining("\n")));
            }
        }
        return description.toString();
    }
}
