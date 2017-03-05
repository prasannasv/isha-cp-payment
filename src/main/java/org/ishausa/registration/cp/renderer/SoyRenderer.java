package org.ishausa.registration.cp.renderer;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;

import java.io.File;
import java.util.Map;

/**
 * Helper that knows how to render content using the Soy templates.
 *
 * Created by tosri on 3/5/2017.
 */
public class SoyRenderer {

    public enum RegistrationAppTemplate {
        INDEX,
        NON_EXISTENT_ID,
        PAYMENT_SUCCESS,
        PAYMENT_FAILURE;

        @Override
        public String toString() {
            return "." + name().toLowerCase();
        }
    }

    public static final SoyRenderer INSTANCE = new SoyRenderer();

    private final SoyTofu serviceTofu;

    private SoyRenderer() {
        final SoyFileSet sfs = SoyFileSet.builder()
                .add(new File("./src/main/webapp/template/registration_app.soy"))
                .build();
        serviceTofu = sfs.compileToTofu().forNamespace("org.ishausa.registration.cp.payment");
    }

    public String render(final RegistrationAppTemplate template, final Map<String, ?> data) {
        return serviceTofu.newRenderer(template.toString()).setData(data).render();
    }
}
