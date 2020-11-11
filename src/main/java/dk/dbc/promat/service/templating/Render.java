package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.Case;
import dk.dbc.promat.service.persistence.Notification;
import dk.dbc.promat.service.persistence.Reviewer;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Render {
    private static final Logger LOGGER = LoggerFactory.getLogger(Render.class);
    private CodeResolver codeResolver;
    private TemplateEngine templateEngine;
    public Render() {
        String path = Render.class.getResource("/jte/templates/").getPath();
        LOGGER.info("Using templates at: {}", path);
        codeResolver= new DirectoryCodeResolver(Path.of(path));
        templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
    }

    public Notification mail(String template, Case assignedCase) {
        TemplateOutput output = new StringOutput();
        templateEngine.render(template, assignedCase, output);
        return new Notification()
                .withBodyText(output.toString())
                .withSubject("Ny ProMat anmeldelse")
                .withToAddress(assignedCase.getReviewer().getEmail());
    }

}
