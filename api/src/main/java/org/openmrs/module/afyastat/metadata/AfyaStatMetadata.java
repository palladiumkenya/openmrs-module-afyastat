/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.afyastat.metadata;

import org.openmrs.PatientIdentifierType;
import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.*;

/**
 * Metadata constants
 */
@Component
public class AfyaStatMetadata extends AbstractMetadataBundle {
	
	public static final String MODULE_ID = "afyastat";
	
	public static final String MEDIC_MOBILE_LAST_PATIENT_CONTACT_ENTRY = "medic.lastSavedPatientContact";
	
	public static final String MEDIC_MOBILE_SERVER_URL = "medic.chtServerUrl";
	
	public static final String MEDIC_MOBILE_LAST_PATIENT_ENTRY = "medic.lastSavedPersonId";
	
	public static final String MEDIC_MOBILE_USER = "medic.chtUser";
	
	public static final String MEDIC_MOBILE_PWD = "medic.chtPwd";
	
	public static final String TELEPHONE_CONTACT = "b2c38640-2603-4629-aebd-3b54f33f1e3a";
	
	public static final class _PatientIdentifierType {
		
		public static final String CHT_RECORD_UUID = "c6552b22-f191-4557-a432-1f4df872d473";
	}
	
	@Override
	public void install() throws Exception {
		// doing this in the scheduled task so that previous value set is preserved
		//install(globalProperty(MODULE_ID +".contactListingMigrationChore", "Migrates contact previously listed using family history form", "false"));
		
		install(globalProperty(MEDIC_MOBILE_LAST_PATIENT_CONTACT_ENTRY, "Id for the last case contact entry for CHT", null));
		install(globalProperty(MEDIC_MOBILE_LAST_PATIENT_ENTRY, "Medic last patient entry ID", null));
		install(globalProperty(MEDIC_MOBILE_SERVER_URL, "Server URL for Medic Mobile CHT", null));
		install(globalProperty(MEDIC_MOBILE_USER, "Medic Mobile CHT user", null));
		install(globalProperty(MEDIC_MOBILE_PWD, "Medic Mobile CHT pwd", null));
		
		install(patientIdentifierType("CHT Record Reference UUID", "Record reference UUID from CHT", null, null, null,
		    PatientIdentifierType.LocationBehavior.NOT_USED, false, _PatientIdentifierType.CHT_RECORD_UUID));
	}
	
}
