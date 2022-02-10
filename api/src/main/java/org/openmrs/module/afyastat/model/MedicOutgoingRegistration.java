package org.openmrs.module.afyastat.model;

import org.openmrs.BaseOpenmrsData;

import javax.persistence.*;
import java.io.Serializable;

public class MedicOutgoingRegistration extends BaseOpenmrsData implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue
	private Integer id;
	
	private Integer patientId;
	
	private String chtRef;
	
	private String kemrRef;
	
	private String purpose;
	
	private String payload;
	
	private Integer status;
	
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
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Integer getPatientId() {
		return patientId;
	}
	
	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}
	
	public String getChtRef() {
		return chtRef;
	}
	
	public void setChtRef(String chtRef) {
		this.chtRef = chtRef;
	}
	
	public String getKemrRef() {
		return kemrRef;
	}
	
	public void setKemrRef(String kemrRef) {
		this.kemrRef = kemrRef;
	}
	
	public String getPurpose() {
		return purpose;
	}
	
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	
	public String getPayload() {
		return payload;
	}
	
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	public Integer getStatus() {
		return status;
	}
	
	public void setStatus(Integer status) {
		this.status = status;
	}
}
