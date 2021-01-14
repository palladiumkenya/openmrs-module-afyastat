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

import java.util.Date;
import java.util.Set;

/**
 */
public class ErrorInformation extends AuditTableData {
	
	private String message;
	
	private Date dateProcessed;
	
	private Set<ErrorMessagesInfo> errorMessages;
	
	public ErrorInformation() {
	}
	
	public ErrorInformation(final AuditTableData data) {
		super(data);
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(final String message) {
		this.message = message;
	}
	
	public Date getDateProcessed() {
		return dateProcessed;
	}
	
	public void setDateProcessed(final Date dateProcessed) {
		this.dateProcessed = dateProcessed;
	}
	
	public Set<ErrorMessagesInfo> getErrorMessages() {
		return errorMessages;
	}
	
	public void setErrorMessages(Set<ErrorMessagesInfo> errorMessages) {
		this.errorMessages = errorMessages;
	}
}
