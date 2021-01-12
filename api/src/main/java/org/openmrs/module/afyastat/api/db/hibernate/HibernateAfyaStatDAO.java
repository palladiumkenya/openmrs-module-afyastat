/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.afyastat.api.db.hibernate;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.afyastat.api.db.AfyastatDao;
import org.openmrs.module.afyastat.api.ContactTrace;
import org.openmrs.module.afyastat.api.PatientContact;
import org.openmrs.module.afyastat.api.service.MedicQueData;
import org.openmrs.module.reporting.common.DurationUnit;

import java.util.*;

public class HibernateAfyaStatDAO implements AfyastatDao {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private SessionFactory sessionFactory;
	
	/**
	 * @Autowired private HTSDAO htsDAO;
	 */
	
	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	/**
	 * @return the sessionFactory
	 */
	
	@Override
	public PatientContact savePatientContact(PatientContact patientContact) throws DAOException {
		sessionFactory.getCurrentSession().saveOrUpdate(patientContact);
		return patientContact;
		
	}
	
	@Override
	public List<PatientContact> getPatientContactByPatient(Patient patient) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(PatientContact.class);
		criteria.add(Restrictions.eq("patientRelatedTo", patient));
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}
	
	@Override
	public List<ContactTrace> getContactTraceByPatientContact(PatientContact patientContact) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(ContactTrace.class);
		criteria.add(Restrictions.eq("patientContact", patientContact));
		return criteria.list();
	}
	
	@Override
	public PatientContact getPatientContactEntryForPatient(Patient patient) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(PatientContact.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("voided", false));
		if (!CollectionUtils.isEmpty(criteria.list())) {
			return (PatientContact) criteria.list().get(0);
		}
		return null;
	}
	
	@Override
	public List<PatientContact> getPatientContacts() {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(PatientContact.class);
		criteria.add(Restrictions.eq("voided", false));
		//return result
		return criteria.list();
	}
	
	@Override
	public void voidPatientContact(int theId) {
		
		sessionFactory.getCurrentSession().saveOrUpdate(theId);
	}
	
	@Override
	public List<PatientContact> searchPatientContact(String searchName) {
		// get the current hibernate session
		
		Query query = null;
		//only search by name if name is not empty
		if (searchName != null && searchName.trim().length() > 0) {
			
		} else {
			
		}
		List<PatientContact> contacts = query.list();
		return contacts;
	}
	
	@Override
	public PatientContact getPatientContactByID(Integer patientContactId) {
		return (PatientContact) this.sessionFactory.getCurrentSession().get(PatientContact.class, patientContactId);
	}
	
	@Override
	public PatientContact getPatientContactByUuid(String uuid) {
		return (PatientContact) this.sessionFactory.getCurrentSession().createCriteria(PatientContact.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	@Override
	public ContactTrace saveClientTrace(ContactTrace contactTrace) throws DAOException {
		
		sessionFactory.getCurrentSession().saveOrUpdate(contactTrace);
		return contactTrace;
	}
	
	@Override
	public MedicQueData saveQueData(MedicQueData medicQueData) throws DAOException {
		
		sessionFactory.getCurrentSession().saveOrUpdate(medicQueData);
		return medicQueData;
	}
	
	public ContactTrace getPatientContactTraceById(Integer patientContactTraceId) {
		return (ContactTrace) this.sessionFactory.getCurrentSession().get(ContactTrace.class, patientContactTraceId);
		
	}
	
	@Override
	public ContactTrace getLastTraceForPatientContact(PatientContact patientContact) {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(ContactTrace.class);
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.eq("patientContact", patientContact));
		criteria.addOrder(Order.desc("date"));
		criteria.addOrder(Order.desc("id"));
		criteria.setMaxResults(1);
		//return result
		if (!CollectionUtils.isEmpty(criteria.list())) {
			return (ContactTrace) criteria.list().get(0);
		}
		return null;
	}
	
	@Override
	public Cohort getPatientsWithGender(boolean includeMales, boolean includeFemales, boolean includeUnknownGender) {
		
		if (!includeMales && !includeFemales && !includeUnknownGender) {
			return new Cohort();
		}
		
		String prefixTerm = "";
		StringBuilder query = new StringBuilder(
		        "select c.id from kenyaemr_hiv_testing_patient_contact c where c.voided = 0 and ( ");
		if (includeMales) {
			query.append(" c.sex = 'M' ");
			prefixTerm = " or";
		}
		if (includeFemales) {
			query.append(prefixTerm + " c.sex = 'F'");
			prefixTerm = " or";
		}
		if (includeUnknownGender) {
			query.append(prefixTerm + " c.sex is null or (c.sex != 'M' and c.sex != 'F')");
		}
		query.append(")");
		Query q = sessionFactory.getCurrentSession().createSQLQuery(query.toString());
		q.setCacheMode(CacheMode.IGNORE);
		return new Cohort(q.list());
	}
	
	@Override
	public Cohort getPatientsWithAgeRange(Integer minAge, DurationUnit minAgeUnit, Integer maxAge, DurationUnit maxAgeUnit,
	        boolean unknownAgeIncluded, Date effectiveDate) {
		
		if (effectiveDate == null) {
			effectiveDate = new Date();
		}
		if (minAgeUnit == null) {
			minAgeUnit = DurationUnit.YEARS;
		}
		if (maxAgeUnit == null) {
			maxAgeUnit = DurationUnit.YEARS;
		}
		
		String sql = "select c.id from kenyaemr_hiv_testing_patient_contact c where c.voided = 0 and ";
		Map<String, Date> paramsToSet = new HashMap<String, Date>();
		
		Date maxBirthFromAge = effectiveDate;
		if (minAge != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(effectiveDate);
			cal.add(minAgeUnit.getCalendarField(), -minAgeUnit.getFieldQuantity() * minAge);
			maxBirthFromAge = cal.getTime();
		}
		
		String c = "c.birth_date <= :maxBirthFromAge";
		paramsToSet.put("maxBirthFromAge", maxBirthFromAge);
		
		Date minBirthFromAge = null;
		if (maxAge != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(effectiveDate);
			cal.add(maxAgeUnit.getCalendarField(), -(maxAgeUnit.getFieldQuantity() * maxAge + 1));
			minBirthFromAge = cal.getTime();
			c = "(" + c + " and c.birth_date >= :minBirthFromAge)";
			paramsToSet.put("minBirthFromAge", minBirthFromAge);
		}
		
		if (unknownAgeIncluded) {
			c = "(c.birth_date is null or " + c + ")";
		}
		
		sql += c;
		
		log.debug("Executing: " + sql + " with params: " + paramsToSet);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sql);
		for (Map.Entry<String, Date> entry : paramsToSet.entrySet()) {
			query.setDate(entry.getKey(), entry.getValue());
		}
		
		return new Cohort(query.list());
	}
	
	public List<PatientContact> getPatientContactListForRegistration() {
		Criteria criteria = this.sessionFactory.getCurrentSession().createCriteria(PatientContact.class);
		criteria.add(Restrictions.isNull("patient"));
		criteria.add(Restrictions.isNotNull("appointmentDate"));
		criteria.add(Restrictions.eq("voided", false));
		criteria.setMaxResults(20);
		return criteria.list();
	}
}
