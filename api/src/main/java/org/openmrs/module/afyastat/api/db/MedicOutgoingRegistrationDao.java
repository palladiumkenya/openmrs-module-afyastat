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

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("afyastat.MedicOutgoingRegistrationDao")
public class MedicOutgoingRegistrationDao {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	public MedicOutgoingRegistration getRecordByUuid(String uuid) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public MedicOutgoingRegistration saveRecord(MedicOutgoingRegistration record) {
		getSession().saveOrUpdate(record);
		getSession().flush();
		return record;
	}
	
	public List<MedicOutgoingRegistration> getAllRecords() {
		return getSession().createCriteria(MedicOutgoingRegistration.class).list();
	}
	
	public MedicOutgoingRegistration getRecord(Integer id) {
		return (MedicOutgoingRegistration) getSession().get(MedicOutgoingRegistration.class, id);
	}
	
	public MedicOutgoingRegistration getRecordByPatientId(Integer ptId) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("patientId", ptId)).uniqueResult();
	}
	
	public MedicOutgoingRegistration getRecordByChtRef(String chtRef) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("chtRef", chtRef)).uniqueResult();
	}
	
	public MedicOutgoingRegistration getRecordByKemrRef(String kemrRef) {
		return (MedicOutgoingRegistration) getSession().createCriteria(MedicOutgoingRegistration.class)
		        .add(Restrictions.eq("kemrRef", kemrRef)).uniqueResult();
	}
	
	public void purgeRecord(MedicOutgoingRegistration record) {
		getSession().delete(record);
	}
	
	public void voidRecord(Integer id) {
		MedicOutgoingRegistration record = (MedicOutgoingRegistration) getSession().get(MedicOutgoingRegistration.class, id);
		record.setVoided(true);
	}
	
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status) {
		return getSession().createCriteria(MedicOutgoingRegistration.class).add(Restrictions.eq("status", status)).list();
	}
	
	public List<MedicOutgoingRegistration> getRecordsByPurpose(String purpose) {
		return getSession().createCriteria(MedicOutgoingRegistration.class).add(Restrictions.eq("purpose", purpose)).list();
	}
	
	public List<MedicOutgoingRegistration> getRecordsByDate(Date startDate, Date endDate) {
		Criteria criteria = getSession().createCriteria(MedicOutgoingRegistration.class);
		criteria.add(Restrictions.ge("date_created", startDate));
		criteria.add(Restrictions.le("date_created", endDate));
		return criteria.list();
	}
	
	public void recordSetStatus(Integer id, Integer status) {
		MedicOutgoingRegistration record = (MedicOutgoingRegistration) getSession().get(MedicOutgoingRegistration.class, id);
		record.setStatus(status);
	}
}
