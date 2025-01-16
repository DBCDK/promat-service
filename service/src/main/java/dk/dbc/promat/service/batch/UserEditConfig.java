package dk.dbc.promat.service.batch;


import java.util.Optional;

/**
 * Container for getting update interval of user core data. And for setting the very same value.
 */
public class UserEditConfig {
    private static int userEditTimeOut = Integer.parseInt(Optional.ofNullable(System.getenv("SECONDS_SINCE_LAST_USER_UPDATE")).orElse("1800"));

    public static int getUserEditTimeOut() {
        return userEditTimeOut;
    }

    public static void setUserEditTimeOut(int userEditTimeOut) {
        UserEditConfig.userEditTimeOut = userEditTimeOut;

    }
}
