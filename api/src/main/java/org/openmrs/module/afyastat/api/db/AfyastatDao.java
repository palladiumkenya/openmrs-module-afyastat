/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.afyastat.api.db;

import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.module.afyastat.api.ContactTrace;
import org.openmrs.module.afyastat.api.PatientContact;
import org.openmrs.module.afyastat.api.service.MedicQueData;
import org.openmrs.module.reporting.common.DurationUnit;

import java.util.Date;
import java.util.List;

public interface AfyastatDao {
	
	public PatientContact savePatientContact(PatientContact patientContact);
	
	public List<PatientContact> getPatientContactByPatient(Patient patient);
	
	public List<PatientContact> getPatientContacts();
	
	public void voidPatientContact(int theId);
	
	public List<PatientContact> searchPatientContact(String searchName);
	
	public PatientContact getPatientContactByID(Integer patientContactId);
	
	public PatientContact getPatientContactByUuid(String uuid);
	
	public ContactTrace saveClientTrace(ContactTrace contactTrace);
	
	public MedicQueData saveQueData(MedicQueData medicQueData);
	
	public ContactTrace getPatientContactTraceById(Integer patientContactTraceId);
	
	public ContactTrace getLastTraceForPatientContact(PatientContact patientContact);
	
	List<ContactTrace> getContactTraceByPatientContact(PatientContact patientContact);
	
	public PatientContact getPatientContactEntryForPatient(Patient patient);
	
	public Cohort getPatientsWithGender(boolean includeMales, boolean includeFemales, boolean includeUnknownGender);
	
	public Cohort getPatientsWithAgeRange(Integer minAge, DurationUnit minAgeUnit, Integer maxAge, DurationUnit maxAgeUnit,
	        boolean unknownAgeIncluded, Date effectiveDate);
	
	public List<PatientContact> getPatientContactListForRegistration();
}
