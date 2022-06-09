package org.openmrs.module.afyastat.htmltojson;

import org.codehaus.jackson.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer for html <obs></obs> tag
 */
public class HtmlFormObsRenderer {
    private String questionConcept; // coded, text, date, numeric,
    private String obsRendering; // radio, checkbox, textarea
    private String requiredField;
    private Map<String,String> answerConcepts = new HashMap<String, String>(); // takes care of concept answers and labels




    private ObjectNode codedObsRenderer() {
        return null;
    }

    public String getQuestionConcept() {
        return questionConcept;
    }

    public void setQuestionConcept(String questionConcept) {
        this.questionConcept = questionConcept;
    }

    public String getObsRendering() {
        return obsRendering;
    }

    public void setObsRendering(String obsRendering) {
        this.obsRendering = obsRendering;
    }

    public String getRequiredField() {
        return requiredField;
    }

    public void setRequiredField(String requiredField) {
        this.requiredField = requiredField;
    }

    public Map<String, String> getAnswerConcepts() {
        return answerConcepts;
    }

    public void setAnswerConcepts(Map<String, String> answerConcepts) {
        this.answerConcepts = answerConcepts;
    }
}
