package dk.dbc.promat.service.connectors;

import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorFactory;

import java.util.Collection;

/**
 * Manual test tool for {@link RecordServiceConnector}, exercising the same calls as
 * {@link dk.dbc.promat.service.api.RecordsProvider#resolveTitle(String)} against a real
 * rawrepo-record-service endpoint - to check whether the connector behaves the same
 * against both DM3 and DM2/cisterne record-service instances.
 * <p>
 * Not part of the deployed application.
 * <p>
 * Usage: RecordServiceConnectorCli &lt;base-url&gt; &lt;faust&gt;
 * <p>
 * Example (cisterne): RecordServiceConnectorCli http://rawrepo-record-service.cisterne.svc.cloud.dbc.dk 22252852
 */
public class RecordServiceConnectorCli {
    private static final int DBC_AGENCY = 870970;
    private static final String TITLE_FIELD = "245";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: RecordServiceConnectorCli <base-url> <faust>");
            System.exit(1);
        }
        final String baseUrl = args[0];
        final String faust = args[1];

        final RecordServiceConnector connector = RecordServiceConnectorFactory.create(baseUrl);
        try {
            System.out.println("baseUrl=" + baseUrl);
            System.out.println("faust=" + faust);

            final boolean exists = connector.recordExists(DBC_AGENCY, faust);
            System.out.println("recordExists=" + exists);
            if (!exists) {
                return;
            }

            final RecordServiceConnector.Params params = new RecordServiceConnector.Params()
                    .withOutputFormat(RecordServiceConnector.Params.OutputFormat.MARC_JSON);
            final Collection<MarcBinding> bindings = connector.getRecordContentCollection(DBC_AGENCY, faust, params);
            System.out.println("recordContentCollection.size=" + bindings.size());
            final String title = bindings.stream()
                    .map(marcBinding -> marcBinding.getSubFieldValue(TITLE_FIELD, 'a'))
                    .filter(v -> v != null)
                    .findFirst()
                    .orElse(null);
            System.out.println("resolvedTitle=" + (title == null ? "<none>" : title));
        } finally {
            connector.close();
        }
    }
}
