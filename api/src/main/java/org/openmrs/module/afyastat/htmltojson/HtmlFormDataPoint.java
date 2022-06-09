package org.openmrs.module.afyastat.htmltojson;

/**
 * Model for html form data point/tag i.e. <obs></obs>
 */
public class HtmlFormDataPoint {
	
	private String conceptUUID;
	
	private Integer conceptId;
	
	private String conceptName;
	
	private String dataType;
	
	public String getConceptUUID() {
		return conceptUUID;
	}
	
	public void setConceptUUID(String conceptUUID) {
		this.conceptUUID = conceptUUID;
	}
	
	public Integer getConceptId() {
		return conceptId;
	}
	
	public void setConceptId(Integer conceptId) {
		this.conceptId = conceptId;
	}
	
	public String getConceptName() {
		return conceptName;
	}
	
	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
	}
	public String getDataType() {
		return dataType;
	}
	
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (getClass() != o.getClass())
			return false;
		HtmlFormDataPoint pn = (HtmlFormDataPoint) o;
		return pn.getConceptId().equals(this.getConceptId());
	}
	
	@Override
	public int hashCode() {
		return getConceptId().hashCode();
	}
}
