/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class FulltextHandlerTest {
    @Test
    void getFilenameFromContentDispositionHeader() {
        var headerValue = "attachment; filename=content.txt";
        assertThat(headerValue, FulltextHandler.getFilenameFromContentDispositionHeader(headerValue).orElse(""),
                is("content.txt"));

        headerValue = "attachment; filename=\"content.txt\"";
        assertThat(headerValue, FulltextHandler.getFilenameFromContentDispositionHeader(headerValue).orElse(""),
                is("content.txt"));

        headerValue = "Content-Disposition: form-data; name=\"fieldName\"; filename=\"content.txt\"";
        assertThat(headerValue, FulltextHandler.getFilenameFromContentDispositionHeader(headerValue).orElse(""),
                is("content.txt"));

        headerValue = "attachment; filename*=UTF-8''content.txt";
        assertThat(headerValue, FulltextHandler.getFilenameFromContentDispositionHeader(headerValue).orElse(""),
                is("content.txt"));

        headerValue = "attachment; filename=\"EURO rates\"; filename*=utf-8''%e2%82%ac%20rates";
        assertThat(headerValue, FulltextHandler.getFilenameFromContentDispositionHeader(headerValue).orElse(""),
                is("EURO rates"));

        headerValue = "attachment; filename=\"omáèka.jpg\"";
        assertThat(headerValue, FulltextHandler.getFilenameFromContentDispositionHeader(headerValue).orElse(""),
                is("omáèka.jpg"));

        headerValue = "";
        assertThat("empty string", FulltextHandler.getFilenameFromContentDispositionHeader(headerValue),
                is(Optional.empty()));
    }
}