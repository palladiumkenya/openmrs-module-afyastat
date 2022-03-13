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
package org.openmrs.module.afyastat.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Location;
import org.openmrs.Provider;

/**
 * This class has been adapted from
 * https://github.com/muzima/openmrs-module-muzimacore/blob/master/api
 * /src/main/java/org/openmrs/module/muzima/model/AuditableData.java
 */
public abstract class AuditableInfo extends BaseOpenmrsData implements Info {
	
	private Integer id;
	
	private String payload;
	
	private String discriminator;
	
	private AfyaDataSource dataSource;
	
	private Location location;
	
	private Provider provider;
	
	private String formName;
	
	private String patientUuid;
	
	private String formDataUuid;
	
	private Long dateFormFilled;
	
	/**
	 * **** Audit information ******
	 */
	
	public AuditableInfo() {
	}
	
	public AuditableInfo(final AuditableInfo data) {
		setPayload(data.getPayload());
		setDataSource(data.getDataSource());
		setDiscriminator(data.getDiscriminator());
		setLocation(data.getLocation());
		setProvider(data.getProvider());
		setFormName(data.getFormName());
		setPatientUuid(data.getPatientUuid());
		setFormDataUuid(data.getFormDataUuid());
		setDateFormFilled(data.getDateFormFilled());
		
	}
	
	/**
	 * @return id - The unique Identifier for the object
	 */
	@Override
	public Integer getId() {
		return id;
	}
	
	/**
	 * @param id - The unique Identifier for the object
	 */
	@Override
	public void setId(final Integer id) {
		this.id = id;
	}
	
	/**
	 * Get the data payload of this data.
	 * 
	 * @return the payload of this data.
	 */
	@Override
	public String getPayload() {
		return payload;
	}
	
	/**
	 * Set the data payload for this data.
	 * 
	 * @param payload the payload for this data
	 */
	public void setPayload(final String payload) {
		this.payload = payload;
	}
	
	/**
	 * Get the discriminating value to determine which handler to execute.
	 * 
	 * @return the discriminating value to determine which handler to execute.
	 */
	public String getDiscriminator() {
		return discriminator;
	}
	
	/**
	 * Set the discriminating value to determine which handler to execute.
	 * 
	 * @param discriminator the discriminating value to determine which handler to execute.
	 */
	public void setDiscriminator(final String discriminator) {
		this.discriminator = discriminator;
	}
	
	/**
	 * Get the data source of this data.
	 * 
	 * @return the data source of this data.
	 */
	public AfyaDataSource getDataSource() {
		return dataSource;
	}
	
	/**
	 * Set the data source for this data.
	 * 
	 * @param dataSource the data source for this data.
	 */
	public void setDataSource(final AfyaDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Provider getProvider() {
		return provider;
	}
	
	public void setProvider(Provider provider) {
		this.provider = provider;
	}
	
	public String getFormName() {
		return formName;
	}
	
	public void setFormName(String formName) {
		this.formName = formName;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getFormDataUuid() {
		return formDataUuid;
	}
	
	public void setFormDataUuid(String formDataUuid) {
		this.formDataUuid = formDataUuid;
	}
	
	public Long getDateFormFilled() {
		return dateFormFilled;
	}
	
	public void setDateFormFilled(Long dateFormFilled) {
		this.dateFormFilled = dateFormFilled;
	}
}
