package dk.dbc.promat.service.templating;

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

public class Template {
    private static final Logger LOGGER = LoggerFactory.getLogger(Template.class);
    private CodeResolver codeResolver;
    private TemplateEngine templateEngine;
    public Template() {
        String path = Template.class.getResource("/jte/templates/").getPath();
        LOGGER.info("Using templates at: {}", path);
        codeResolver= new DirectoryCodeResolver(Path.of(path));
        templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
    }

    public String mail(String template, Reviewer reviewer) {
        TemplateOutput output = new StringOutput();
        templateEngine.render(template, reviewer, output);
        return output.toString();
    }

}
