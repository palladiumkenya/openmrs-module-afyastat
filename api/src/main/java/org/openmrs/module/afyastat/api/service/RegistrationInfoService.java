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
package org.openmrs.module.afyastat.api.service;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.afyastat.model.RegistrationInfo;

import java.util.List;

/**
 * This service exposes module's core functionality. It is a Spring managed bean which is configured
 * in moduleApplicationContext.xml.
 * <p/>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(RegistrationDataService.class).someMethod();
 * </code> The design has been adapted from
 * https://github.com/muzima/openmrs-module-muzimacore/blob/
 * master/api/src/main/java/org/openmrs/module
 * /muzima/api/service/impl/RegistrationDataServiceImpl.java
 * 
 * @see org.openmrs.api.context.Context
 */
public interface RegistrationInfoService extends OpenmrsService {
	
	/**
	 * Get registration data by the internal database id of the registration data.
	 * 
	 * @param id the internal database id.
	 * @return the registration data with matching internal database id.
	 */
	RegistrationInfo getRegistrationDataById(final Integer id);
	
	/**
	 * Get registration data by the uuid of the registration data.
	 * 
	 * @param uuid the uuid of the registration data.
	 * @return the registration data with matching uuid.
	 */
	RegistrationInfo getRegistrationDataByUuid(final String uuid);
	
	/**
	 * Get registration data based on the temporary uuid assigned to a patient created through the
	 * registration form.
	 * 
	 * @param temporaryUuid the temporary uuid assigned to a patient.
	 * @return the registration data based on the temporary uuid.
	 */
	RegistrationInfo getRegistrationDataByTemporaryUuid(final String temporaryUuid);
	
	/**
	 * Get registration data based on the patient real uuid.
	 * 
	 * @param assignedUuid the patient real uuid.
	 * @return list of temporary uuid which correspond to temporary patients data created through
	 *         registration.
	 */
	List<RegistrationInfo> getRegistrationDataByAssignedUuid(final String assignedUuid);
	
	/**
	 * Create a new registration data entry in the database.
	 * 
	 * @param registrationData the registration data to be created.
	 * @return the new registration data.
	 */
	RegistrationInfo saveRegistrationData(final RegistrationInfo registrationData);
	
	/**
	 * Delete a registration data.
	 * 
	 * @param registrationData the registration data to be deleted.
	 */
	void deleteRegistrationData(final RegistrationInfo registrationData);
	
	/**
	 * Get all registration data information from the database.
	 * 
	 * @param pageNumber the page number.
	 * @param pageSize the page size.
	 * @return all registration data in the database.
	 */
	List<RegistrationInfo> getRegistrationData(final Integer pageNumber, final Integer pageSize);
	
	/**
	 * Count the number of registration data in the database.
	 * 
	 * @return the number of registration data in the database.
	 */
	Number countRegistrationData();
}
