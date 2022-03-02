/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.afyastat.api.db.hibernate;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.openmrs.module.afyastat.api.db.MedicOutgoingRegistrationDao;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;

public class HibernateMedicOutgoingRegistrationDao implements MedicOutgoingRegistrationDao {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	/**
	 * Get a record with a given UUID
	 * 
	 * @param uuid the record UUID
	 * @return a record object
	 */
	@Override
	public MedicOutgoingRegistration getRecordByUuid(String uuid) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	/**
	 * Save or Update a record
	 * 
	 * @param record the record to save or update
	 * @return the saved or updated record object
	 */
	@Override
	public MedicOutgoingRegistration saveOrUpdate(MedicOutgoingRegistration record) {
		getSession().saveOrUpdate(record);
		getSession().flush();
		return record;
	}
	
	/**
	 * Get all records
	 * 
	 * @return a list of records
	 */
	@Override
	public List<MedicOutgoingRegistration> getAllRecords() {
		Criteria criteria = getSession().createCriteria(MedicOutgoingRegistration.class);
		criteria.addOrder(Order.asc("dateCreated"));
		return criteria.list();
	}
	
	/**
	 * Get a record with a given ID
	 * 
	 * @param ptId the record ID
	 * @return a record object
	 */
	@Override
	public MedicOutgoingRegistration getRecord(Integer id) {
		return (MedicOutgoingRegistration) getSession().get(MedicOutgoingRegistration.class, id);
	}
	
	/**
	 * Get a record with a given patient ID
	 * 
	 * @param ptId the patient ID
	 * @return a record object
	 */
	@Override
	public MedicOutgoingRegistration getRecordByPatientId(Integer ptId) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("patientId", ptId)).uniqueResult();
	}
	
	/**
	 * Get a record with a given CHT ref
	 * 
	 * @param chtRef the CHT ref
	 * @return a record object
	 */
	@Override
	public MedicOutgoingRegistration getRecordByChtRef(String chtRef) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("chtRef", chtRef)).uniqueResult();
	}
	
	/**
	 * Get a record with a given KEMR ref
	 * 
	 * @param kemrRef the KEMR ref
	 * @return a record object
	 */
	@Override
	public MedicOutgoingRegistration getRecordByKemrRef(String kemrRef) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("kemrRef", kemrRef)).uniqueResult();
	}
	
	/**
	 * Delete a record
	 * 
	 * @param record the record object to delete
	 */
	@Override
	public void purgeRecord(MedicOutgoingRegistration record) {
		getSession().delete(record);
	}
	
	/**
	 * Mark a record as voided
	 * 
	 * @param id the id of the record
	 */
	@Override
	public void voidRecord(Integer id) {
		MedicOutgoingRegistration record = (MedicOutgoingRegistration) getSession().get(MedicOutgoingRegistration.class, id);
		record.setVoided(true);
	}
	
	/**
	 * Get records with a given status
	 * 
	 * @param status the status (0=unsynced, 1=synced)
	 * @return a list of record objects
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status) {
		Criteria criteria = getSession().createCriteria(MedicOutgoingRegistration.class);
		criteria.add(Restrictions.eq("status", status));
		criteria.addOrder(Order.asc("dateCreated"));
		return criteria.list();
	}
	
	/**
	 * Get records with a given status
	 * 
	 * @param status the status (0=unsynced, 1=synced)
	 * @param limit the limit to number of records to return
	 * @return a list of record objects
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status, Integer limit) {
		Criteria criteria = getSession().createCriteria(MedicOutgoingRegistration.class);
		criteria.add(Restrictions.eq("status", status));
		criteria.addOrder(Order.asc("dateCreated"));
		criteria.setMaxResults(limit);
		return criteria.list();
	}
	
	/**
	 * Gets records with a given purpose
	 * 
	 * @param purpose the purpose
	 * @return a list of record objects
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByPurpose(String purpose) {
		Criteria criteria = getSession().createCriteria(MedicOutgoingRegistration.class);
		criteria.add(Restrictions.eq("purpose", purpose));
		criteria.addOrder(Order.asc("dateCreated"));
		return criteria.list();
	}
	
	/**
	 * Gets a range of records given the start date and end date
	 * 
	 * @param startDate the start date
	 * @param endDate the end date
	 * @return a list of record objects
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByDate(Date startDate, Date endDate) {
		Criteria criteria = getSession().createCriteria(MedicOutgoingRegistration.class);
		criteria.add(Restrictions.ge("dateCreated", startDate));
		criteria.add(Restrictions.le("dateCreated", endDate));
		criteria.addOrder(Order.asc("dateCreated"));
		return criteria.list();
	}
	
	/**
	 * Sets the status of the queue record 0=unsynced, 1=synced
	 * 
	 * @param id the id of the record
	 * @param status the status to set
	 */
	@Override
	public void recordSetStatus(Integer id, Integer status) {
		MedicOutgoingRegistration record = (MedicOutgoingRegistration) getSession().get(MedicOutgoingRegistration.class, id);
		record.setStatus(status);
	}
}
