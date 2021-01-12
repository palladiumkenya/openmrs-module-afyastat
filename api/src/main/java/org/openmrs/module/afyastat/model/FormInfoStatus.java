package org.openmrs.module.afyastat.model;

import org.openmrs.BaseOpenmrsData;

public class FormInfoStatus extends BaseOpenmrsData {
	
	private Integer id;
	
	private String status;
	
	public FormInfoStatus() {
		super();
	}
	
	public FormInfoStatus(String formDataUuid) {
		setUuid(formDataUuid);
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
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
}
