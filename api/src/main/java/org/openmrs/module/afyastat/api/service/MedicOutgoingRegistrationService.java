/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.afyastat.api.service;

import java.util.Date;
import java.util.List;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.afyastat.AfyastatConfig;
import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;
import org.springframework.transaction.annotation.Transactional;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface MedicOutgoingRegistrationService extends OpenmrsService {
	
	/**
	 * Returns a record by uuid. It can be called by any authenticated user. It is fetched in read
	 * only transaction.
	 * 
	 * @param uuid
	 * @return
	 * @throws APIException
	 */
	@Authorized()
	@Transactional(readOnly = true)
	MedicOutgoingRegistration getRecordByUuid(String uuid) throws APIException;
	
	/**
	 * Saves or edits a record. It is executed in a transaction.
	 * 
	 * @param record
	 * @return
	 * @throws APIException
	 */
	@Authorized(AfyastatConfig.MODULE_PRIVILEGE)
	@Transactional
	MedicOutgoingRegistration saveOrUpdate(MedicOutgoingRegistration record) throws APIException;
	
	/**
	 * Gets a list of records.
	 * 
	 * @return the record list.
	 */
	@Transactional(readOnly = true)
	List<MedicOutgoingRegistration> getAllRecords();
	
	/**
	 * Gets a record for a given id.
	 * 
	 * @param id the record id
	 * @return the record with the given id
	 */
	@Transactional(readOnly = true)
	MedicOutgoingRegistration getRecordById(Integer id);
	
	/**
	 * Gets a record for a given patient id.
	 * 
	 * @param ptId the record ptId
	 * @return the record with the given patient id
	 */
	@Transactional(readOnly = true)
	MedicOutgoingRegistration getRecordByPatientId(Integer ptId);
	
	/**
	 * Gets a record for a given chtRef.
	 * 
	 * @param chtRef the record chtRef
	 * @return the record with the given chtRef
	 */
	@Transactional(readOnly = true)
	public MedicOutgoingRegistration getRecordByChtRef(String chtRef);
	
	/**
	 * Gets a record for a given kemrRef.
	 * 
	 * @param kemrRef the record kemrRef
	 * @return the record with the given kemrRef
	 */
	@Transactional(readOnly = true)
	public MedicOutgoingRegistration getRecordByKemrRef(String kemrRef);
	
	/**
	 * Deletes a record from the database.
	 * 
	 * @param record the record to delete.
	 */
	@Authorized(AfyastatConfig.MODULE_PRIVILEGE)
	@Transactional
	void purgeRecord(MedicOutgoingRegistration record);
	
	/**
	 * Voids a record given an id.
	 * 
	 * @param id the record id
	 */
	@Authorized(AfyastatConfig.MODULE_PRIVILEGE)
	@Transactional
	void voidRecord(Integer id);
	
	/**
	 * Gets records with a given status.
	 * 
	 * @param status the record status
	 * @return all records with the given status
	 */
	@Transactional(readOnly = true)
	List<MedicOutgoingRegistration> getRecordsByStatus(Integer status);
	
	/**
	 * Gets records with a given status limiting the returned records to a certain number.
	 * 
	 * @param status the record status
	 * @param limit the limit to number of records
	 * @return all records with the given status
	 */
	@Transactional(readOnly = true)
	List<MedicOutgoingRegistration> getRecordsByStatus(Integer status, Integer limit);
	
	/**
	 * Gets records for a given purpose.
	 * 
	 * @param purpose the record purpose
	 * @return all records with the given purpose
	 */
	@Transactional(readOnly = true)
	List<MedicOutgoingRegistration> getRecordsByPurpose(String purpose);
	
	/**
	 * Gets records with a given patient id and purpose
	 * 
	 * @param purpose the purpose
	 * @param ptId the patient ID
	 * @return a record object
	 */
	@Transactional(readOnly = true)
	MedicOutgoingRegistration getRecordByPatientAndPurpose(Integer ptId, String purpose);
	
	/**
	 * Gets records for within a given date range
	 * 
	 * @param startDate the range start date
	 * @param endDate the range start date
	 * @return all records within the given date range
	 */
	@Transactional(readOnly = true)
	List<MedicOutgoingRegistration> getRecordsByDate(Date startDate, Date endDate);
	
	/**
	 * Sets the status of a record
	 * 
	 * @param id the record id
	 * @param status the record status
	 */
	@Authorized(AfyastatConfig.MODULE_PRIVILEGE)
	@Transactional
	void recordSetStatus(Integer id, Integer status);
}
