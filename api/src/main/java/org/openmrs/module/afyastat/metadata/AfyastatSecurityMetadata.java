package org.openmrs.module.afyastat.metadata;

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.openmrs.module.metadatadeploy.bundle.Requires;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.idSet;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.privilege;
import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.role;

/**
 * Implementation of access control to the app.
 */
@Component
@Requires(org.openmrs.module.kenyaemr.metadata.SecurityMetadata.class)
public class AfyastatSecurityMetadata extends AbstractMetadataBundle {

    public static class _Privilege {
        public static final String APP_AFYASTAT_ADMIN = "App: kenyaemr.afyastat.home";
    }

    public static final class _Role {
        public static final String APPLICATION_AFYASTAT_ADMIN = "Afyastat queue administration";
    }

    /**
     * @see AbstractMetadataBundle#install()
     */
    @Override
    public void install() {

        install(privilege(_Privilege.APP_AFYASTAT_ADMIN, "Able to view action on afyastat queue data"));
        install(role(_Role.APPLICATION_AFYASTAT_ADMIN, "Can access Afyastat app", idSet(
                org.openmrs.module.kenyaemr.metadata.SecurityMetadata._Role.API_PRIVILEGES_VIEW_AND_EDIT
        ), idSet(
                _Privilege.APP_AFYASTAT_ADMIN
        )));
    }
}
