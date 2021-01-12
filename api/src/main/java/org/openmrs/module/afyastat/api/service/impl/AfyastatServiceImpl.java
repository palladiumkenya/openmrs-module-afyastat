/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.afyastat.api.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.afyastat.api.AfyastatService;
import org.openmrs.module.afyastat.api.ContactTrace;
import org.openmrs.module.afyastat.api.PatientContact;
import org.openmrs.module.afyastat.api.db.hibernate.HibernateAfyaStatDAO;
import org.openmrs.module.afyastat.api.service.MedicQueData;
import org.openmrs.module.reporting.common.DurationUnit;

import java.util.Date;
import java.util.List;

public class AfyastatServiceImpl extends BaseOpenmrsService implements AfyastatService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private HibernateAfyaStatDAO patientContactDAO;
	
	private HibernateAfyaStatDAO queueDataDao;
	
	@Override
	public List<PatientContact> getPatientContacts() {
		return patientContactDAO.getPatientContacts();
	}
	
	@Override
	public PatientContact savePatientContact(PatientContact patientContact) {
		return patientContactDAO.savePatientContact(patientContact);
	}
	
	public void setPatientContactDAO(HibernateAfyaStatDAO patientContactDAO) {
		this.patientContactDAO = patientContactDAO;
	}
	
	public void setQueueDataDao(HibernateAfyaStatDAO queueDataDao) {
		this.queueDataDao = queueDataDao;
	}
	
	public HibernateAfyaStatDAO getQueueDataDao() {
		return queueDataDao;
	}
	
	public HibernateAfyaStatDAO getPatientContactDAO() {
		return patientContactDAO;
	}
	
	public HibernateAfyaStatDAO getDao() {
		return patientContactDAO;
	}
	
	@Override
	public List<PatientContact> searchPatientContact(String searchName) {
		
		return patientContactDAO.searchPatientContact(searchName);
	}
	
	@Override
	public void voidPatientContact(int theId) {
		patientContactDAO.voidPatientContact(theId);
	}
	
	@Override
	public PatientContact getPatientContactByID(Integer patientContactId) {
		return patientContactDAO.getPatientContactByID(patientContactId);
	}
	
	@Override
	public PatientContact getPatientContactByUuid(String uuid) {
		return patientContactDAO.getPatientContactByUuid(uuid);
	}
	
	@Override
	public List<PatientContact> getPatientContactByPatient(Patient patient) {
		return patientContactDAO.getPatientContactByPatient(patient);
	}
	
	@Override
	public ContactTrace saveClientTrace(ContactTrace contactTrace) {
		
		return patientContactDAO.saveClientTrace(contactTrace);
	}
	
	@Override
	public MedicQueData saveQueData(MedicQueData medicQueData) {
		return patientContactDAO.saveQueData(medicQueData);
	}
	
	@Override
	public ContactTrace getPatientContactTraceById(Integer patientContactTraceId) {
		return patientContactDAO.getPatientContactTraceById(patientContactTraceId);
	}
	
	@Override
	public ContactTrace getLastTraceForPatientContact(PatientContact patientContact) {
		return patientContactDAO.getLastTraceForPatientContact(patientContact);
	}
	
	@Override
	public PatientContact getPatientContactEntryForPatient(Patient patient) {
		return patientContactDAO.getPatientContactEntryForPatient(patient);
	}
	
	@Override
	public List<ContactTrace> getContactTraceByPatientContact(PatientContact patientContact) {
		
		return patientContactDAO.getContactTraceByPatientContact(patientContact);
	}
	
	@Override
	public Cohort getPatientsWithGender(boolean includeMales, boolean includeFemales, boolean includeUnknownGender) {
		return patientContactDAO.getPatientsWithGender(includeMales, includeFemales, includeUnknownGender);
	}
	
	@Override
	public Cohort getPatientsWithAgeRange(Integer minAge, DurationUnit minAgeUnit, Integer maxAge, DurationUnit maxAgeUnit,
	        boolean unknownAgeIncluded, Date effectiveDate) {
		return patientContactDAO.getPatientsWithAgeRange(minAge, minAgeUnit, maxAge, maxAgeUnit, unknownAgeIncluded,
		    effectiveDate);
	}
	
	@Override
	public List<PatientContact> getPatientContactListForRegistration() {
		return patientContactDAO.getPatientContactListForRegistration();
	}
	
	@Override
	public void onStartup() {
		
	}
	
	@Override
	public void onShutdown() {
		
	}
	
}
