package dk.dbc.promat.service.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A JPA "AttributeConverter" is a small, reusable translator between a Java type your entity
 * wants to use (here: String[]) and whatever type actually gets sent to/read from the JDBC
 * driver for that column (the "Y" type parameter below). You write one when the two types
 * don't line up automatically - Java has no built-in concept of "this array maps to a SQL
 * array column", so without a converter, JPA wouldn't know what to do with a String[] field.
 *
 * @Converter registers this class with the JPA provider. It's also explicitly listed in
 * persistence.xml's <class> entries (see the persistence.xml itself for why that's needed
 * in this project specifically) - a converter is metadata JPA needs to know about up front,
 * the same way it needs to know about every @Entity class.
 *
 * The AttributeConverter<X, Y> interface has two directions:
 *   - convertToDatabaseColumn(X): called when SAVING, X (String[], the Java-side type) -> Y
 *   - convertToEntityAttribute(Y): called when LOADING, Y (the database-side type) -> X
 *
 * The real lesson in this file is what "Y" should actually be, which took real debugging to
 * find rather than being obvious from documentation. This class originally declared
 * AttributeConverter<String[], PGobject> (PGobject being the PostgreSQL JDBC driver's generic
 * "opaque database value" wrapper type) on the theory that a Postgres text[] column arrives
 * as a PGobject and needs to be manually parsed. That compiled fine and even worked for
 * WRITES - but every READ threw an exception, because EclipseLink, given this column's
 * `columnDefinition = "text[]"` hint on the entity field, was already deserializing the
 * database's array value into a plain String[] itself before ever calling this converter -
 * it never handed over a PGobject at all. Since Java generics are erased at runtime, nothing
 * catches this kind of "the type you declared isn't the type you actually get" mismatch at
 * compile time; it only surfaces as a runtime cast failure the first time real data is read.
 * Declaring AttributeConverter<String[], String[]> instead - matching what EclipseLink
 * genuinely delivers - fixed it, and turned this into a near-identity conversion (with a
 * null-safety default on the read side, since a NULL column value would otherwise become a
 * null array rather than an empty one).
 */
@Converter
public class StringArrayConverter implements AttributeConverter<String[], String[]> {

    @Override
    public String[] convertToDatabaseColumn(String[] attribute) {
        return attribute;
    }

    @Override
    public String[] convertToEntityAttribute(String[] dbData) {
        return dbData == null ? new String[0] : dbData;
    }
}
