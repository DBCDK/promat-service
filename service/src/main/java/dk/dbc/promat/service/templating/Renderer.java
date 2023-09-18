package dk.dbc.promat.service.templating;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Renderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);
    private final TemplateEngine templateEngine;
    public Renderer() {
        String path = Renderer.class.getResource("/jte/templates/").getPath();
        LOGGER.info("Using templates at: {}", path);
        CodeResolver codeResolver = new DirectoryCodeResolver(Path.of(path));
        templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
    }

    public String render(String template, Object model) {
        TemplateOutput output = new StringOutput();
        templateEngine.render(template, model, output);
        return output.toString();
    }

    public String render(String template, Map<String, Object> models) {
        TemplateOutput output = new StringOutput();
        templateEngine.render(template, models, output);
        return output.toString();
    }
}
