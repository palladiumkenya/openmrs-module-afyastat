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

import java.util.Date;
import java.util.List;

import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.afyastat.api.db.hibernate.HibernateMedicOutgoingRegistrationDao;
import org.openmrs.module.afyastat.api.service.MedicOutgoingRegistrationService;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;

public class MedicOutgoingRegistrationServiceImpl extends BaseOpenmrsService implements MedicOutgoingRegistrationService {
	
	private HibernateMedicOutgoingRegistrationDao medicOutgoingRegistrationDao;
	
	UserService userService;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setMedicOutgoingRegistrationDao(HibernateMedicOutgoingRegistrationDao medicOutgoingRegistrationDao) {
		this.medicOutgoingRegistrationDao = medicOutgoingRegistrationDao;
	}
	
	public HibernateMedicOutgoingRegistrationDao getMedicOutgoingRegistrationDao() {
		return (medicOutgoingRegistrationDao);
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	
	/**
	 * Returns an item by uuid. It can be called by any authenticated user. It is fetched in read
	 * only transaction.
	 * 
	 * @param uuid
	 * @return
	 * @throws APIException
	 */
	@Override
	public MedicOutgoingRegistration getRecordByUuid(String uuid) throws APIException {
		return medicOutgoingRegistrationDao.getRecordByUuid(uuid);
	}
	
	/**
	 * Saves an item. Sets the owner to superuser, if it is not set. It can be called by users with
	 * this module's privilege. It is executed in a transaction.
	 * 
	 * @param record
	 * @return
	 * @throws APIException
	 */
	@Override
	public MedicOutgoingRegistration saveOrUpdate(MedicOutgoingRegistration record) throws APIException {
		return medicOutgoingRegistrationDao.saveOrUpdate(record);
	}
	
	/**
	 * Gets a list of records.
	 * 
	 * @return the record list.
	 */
	@Override
	public List<MedicOutgoingRegistration> getAllRecords() {
		return medicOutgoingRegistrationDao.getAllRecords();
	}
	
	/**
	 * Gets a record for a given id.
	 * 
	 * @param id the record id
	 * @return the record with the given id
	 */
	@Override
	public MedicOutgoingRegistration getRecordById(Integer id) {
		return medicOutgoingRegistrationDao.getRecord(id);
	}
	
	/**
	 * Deletes a record from the database.
	 * 
	 * @param record the record to delete.
	 */
	@Override
	public void purgeRecord(MedicOutgoingRegistration record) {
		medicOutgoingRegistrationDao.purgeRecord(record);
	}
	
	/**
	 * Voids a record given an id.
	 * 
	 * @param id the record id
	 */
	@Override
	public void voidRecord(Integer id) {
		medicOutgoingRegistrationDao.voidRecord(id);
	}
	
	/**
	 * Gets records with a given status.
	 * 
	 * @param status the record status
	 * @return all records with the given status
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status) {
		return (medicOutgoingRegistrationDao.getRecordsByStatus(status));
	}
	
	/**
	 * Gets records with a given status limiting the returned records to a certain number.
	 * 
	 * @param status the record status
	 * @param limit the limit to number of records
	 * @return all records with the given status
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status, Integer limit) {
		return (medicOutgoingRegistrationDao.getRecordsByStatus(status, limit));
	}
	
	/**
	 * Gets records for a given purpose.
	 * 
	 * @param purpose the record purpose
	 * @return all records with the given purpose
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByPurpose(String purpose) {
		return (medicOutgoingRegistrationDao.getRecordsByPurpose(purpose));
	}
	
	/**
	 * Gets records for within a given date range
	 * 
	 * @param startDate the range start date
	 * @param endDate the range end date
	 * @return all records within the given date range
	 */
	@Override
	public List<MedicOutgoingRegistration> getRecordsByDate(Date startDate, Date endDate) {
		return (medicOutgoingRegistrationDao.getRecordsByDate(startDate, endDate));
	}
	
	/**
	 * Sets the status of a record
	 * 
	 * @param id the record id
	 * @param status the record status
	 */
	@Override
	public void recordSetStatus(Integer id, Integer status) {
		medicOutgoingRegistrationDao.recordSetStatus(id, status);
	}
	
	/**
	 * Gets a record for a given patient id.
	 * 
	 * @param ptId the record ptId
	 * @return the record with the given patient id
	 */
	@Override
	public MedicOutgoingRegistration getRecordByPatientId(Integer ptId) {
		return (medicOutgoingRegistrationDao.getRecordByPatientId(ptId));
	}
	
	/**
	 * Gets a record for a given chtRef.
	 * 
	 * @param chtRef the record chtRef
	 * @return the record with the given chtRef
	 */
	@Override
	public MedicOutgoingRegistration getRecordByChtRef(String chtRef) {
		return (medicOutgoingRegistrationDao.getRecordByChtRef(chtRef));
	}
	
	/**
	 * Gets a record for a given kemrRef.
	 * 
	 * @param kemrRef the record kemrRef
	 * @return the record with the given kemrRef
	 */
	@Override
	public MedicOutgoingRegistration getRecordByKemrRef(String kemrRef) {
		return (medicOutgoingRegistrationDao.getRecordByKemrRef(kemrRef));
	}
}
