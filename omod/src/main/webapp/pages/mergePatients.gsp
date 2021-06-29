<%
	ui.decorateWith("kenyaemr", "standardPage")
%>

<div class="ke-page-content">
	${ ui.includeFragment("afyastat", "mergePatients", [ queueUuid: queueUuid, patient2: patient2, returnUrl: returnUrl ]) }
</div>