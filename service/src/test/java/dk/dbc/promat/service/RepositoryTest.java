package dk.dbc.promat.service;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class RepositoryTest {

    @Test
    void isEmailShapedReturnsTrueForEmailAddress() {
        assertThat(Repository.isEmailShaped("klnp@dbc.dk"), is(true));
    }

    @Test
    void isEmailShapedReturnsFalseForPlainUserId() {
        assertThat(Repository.isEmailShaped("klnp"), is(false));
    }

    @Test
    void isEmailShapedReturnsFalseForNull() {
        assertThat(Repository.isEmailShaped(null), is(false));
    }
}
