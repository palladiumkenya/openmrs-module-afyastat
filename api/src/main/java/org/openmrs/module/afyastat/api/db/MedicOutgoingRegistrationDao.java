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

import org.openmrs.module.afyastat.model.MedicOutgoingRegistration;

public interface MedicOutgoingRegistrationDao {
	
	/**
	 * Get a record with a given UUID
	 * 
	 * @param uuid the record UUID
	 * @return a record object
	 */
	public MedicOutgoingRegistration getRecordByUuid(String uuid);
	
	/**
	 * Save or Update a record
	 * 
	 * @param record the record to save or update
	 * @return the saved or updated record object
	 */
	public MedicOutgoingRegistration saveOrUpdate(MedicOutgoingRegistration record);
	
	/**
	 * Get all records
	 * 
	 * @return a list of records
	 */
	public List<MedicOutgoingRegistration> getAllRecords();
	
	/**
	 * Get a record with a given ID
	 * 
	 * @param ptId the record ID
	 * @return a record object
	 */
	public MedicOutgoingRegistration getRecord(Integer id);
	
	/**
	 * Get a record with a given patient ID
	 * 
	 * @param ptId the patient ID
	 * @return a list of objects
	 */
	public List<MedicOutgoingRegistration> getRecordsByPatientId(Integer ptId);
	
	/**
	 * Get a record with a given CHT ref
	 * 
	 * @param chtRef the CHT ref
	 * @return a record object
	 */
	public MedicOutgoingRegistration getRecordByChtRef(String chtRef);
	
	/**
	 * Get a record with a given KEMR ref
	 * 
	 * @param kemrRef the KEMR ref
	 * @return a record object
	 */
	public MedicOutgoingRegistration getRecordByKemrRef(String kemrRef);
	
	/**
	 * Delete a record
	 * 
	 * @param record the record object to delete
	 */
	public void purgeRecord(MedicOutgoingRegistration record);
	
	/**
	 * Mark a record as voided
	 * 
	 * @param id the id of the record
	 */
	public void voidRecord(Integer id);
	
	/**
	 * Get records with a given status
	 * 
	 * @param status the status (0=unsynced, 1=synced)
	 * @return a list of record objects
	 */
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status);
	
	/**
	 * Get records with a given status
	 * 
	 * @param status the status (0=unsynced, 1=synced)
	 * @param limit the limit to number of records to return
	 * @return a list of record objects
	 */
	public List<MedicOutgoingRegistration> getRecordsByStatus(Integer status, Integer limit);
	
	/**
	 * Gets records with a given purpose
	 * 
	 * @param purpose the purpose
	 * @return a list of record objects
	 */
	public List<MedicOutgoingRegistration> getRecordsByPurpose(String purpose);
	
	/**
	 * Gets records with a given patient id and purpose
	 * 
	 * @param purpose the purpose
	 * @param ptId the patient ID
	 * @return a record object
	 */
	public MedicOutgoingRegistration getRecordByPatientAndPurpose(Integer ptId, String purpose);
	
	/**
	 * Gets a range of records given the start date and end date
	 * 
	 * @param startDate the start date
	 * @param endDate the end date
	 * @return a list of record objects
	 */
	public List<MedicOutgoingRegistration> getRecordsByDate(Date startDate, Date endDate);
	
	/**
	 * Sets the status of the queue record 0=unsynced, 1=synced
	 * 
	 * @param id the id of the record
	 * @param status the status to set
	 */
	public MedicOutgoingRegistration recordSetStatus(Integer id, Integer status);
	
	/**
	 * Sets the status of the queue record 0=unsynced, 1=synced
	 * 
	 * @param uuid the uuid of the record
	 * @param status the status to set
	 */
	public MedicOutgoingRegistration recordSetStatus(String uuid, Integer status);
	
	/**
	 * Sets the payload of a record
	 * 
	 * @param uuid the record uuid
	 * @param payload the record payload
	 */
	public MedicOutgoingRegistration recordSetPayload(String uuid, String payload);
}
