/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import java.nio.file.Path;
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

}
