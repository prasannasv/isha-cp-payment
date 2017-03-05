package org.ishausa.registration.cp.security;

import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.logging.Logger;

import static spark.Spark.halt;

/**
 * Created by Prasanna Venkat on 3/5/2017.
 */
public class HttpsEnforcer implements Filter {
    private static final Logger log = Logger.getLogger(HttpsEnforcer.class.getName());

    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    @Override
    public void handle(final Request request, final Response response) throws Exception {

        if (!request.url().startsWith("http://localhost:")) {
            final String forwardedProto = request.headers(X_FORWARDED_PROTO);

            if ("http".equals(forwardedProto)) {
                log.info("Redirecting to a secure scheme on a non-secure request");
                response.redirect(request.url().replace("http://", "https://"));
                halt();
            }
        }
    }
}
