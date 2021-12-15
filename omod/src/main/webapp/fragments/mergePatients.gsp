<%
	ui.decorateWith("kenyaui", "panel", [ heading: "Merge Patients", frameOnly: true ])
%>

<form id="merge-patients-form" method="post" action="${ ui.actionLink("afyastat", "mergePatients", "merge") }">


	<div class="ke-panel-content">

		<div class="ke-warning" style="margin-bottom: 5px">Merging patients is something that should not be taken lightly. There is no automatic way to undo this action.</div>

		<div class="ke-form-globalerrors" style="display: none; margin-bottom: 5px"></div>

		<table style="width: 100%">
			<tr>
				<td class="ke-field-label" style="width: 50%; text-align: center">Patient 1 (in error queue)</td>
				<td class="ke-field-label" style="width: 50%; text-align: center">Patient 2 (EMR patient)</td>
			</tr>
			<tr>
				<td style="text-align: center"><input type="hidden" name="queueUuid" value="${queueUuid}"></td>
				<td style="text-align: center">${ ui.includeFragment("kenyaui", "widget/field", [ id: "patient2-select", object: command, property: "patient2" ]) }</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Information</legend>
						<div id="patient1-infopoints" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Information</legend>
						<div id="patient2-infopoints" class="patient2-item"></div>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Names</legend>
						<div id="patient1-names" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Names</legend>
						<div id="patient2-names" class="patient2-item"></div>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Identifiers</legend>
						<div id="patient1-identifiers" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Identifiers</legend>
						<div id="patient2-identifiers" class="patient2-item"></div>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Attributes</legend>
						<div id="patient1-attributes" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Attributes</legend>
						<div id="patient2-attributes" class="patient2-item"></div>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Encounters</legend>
						<div id="patient1-encounters" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Encounters</legend>
						<div id="patient2-encounters" class="patient2-item"></div>
					</fieldset>
				</td>
			</tr>
		</table>

	</div>

	<div class="ke-panel-controls">
		<button type="button" class="merge-button">
			<img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" /> Merge
		</button>
		<button type="button" class="cancel-button">
			<img src="${ ui.resourceLink("kenyaui", "images/glyphs/cancel.png") }" /> Cancel
		</button>
	</div>

</form>

<script type="text/javascript">
	jq(function() {
	    var queueUuid = '${queueUuid}';

		jq('#merge-patients-form .merge-button').click(function() {
			kenyaui.openConfirmDialog({
				heading: 'Merge',
				message: 'Merge the two selected patient records? Continue only if you are 100% positive these are the same patients.',
				okCallback: function () { jq('#merge-patients-form').submit(); }
			});
		});
		jq('#merge-patients-form .cancel-button').click(function() {
			location.href = '${ returnUrl }';
		});

		jq('#patient2-select').on('change', function() {
			updatePatientSummary(jq(this).val(), '2');
		});

		kenyaui.setupAjaxPost('merge-patients-form', {
			onSuccess: function(data) {
				location.href = '${ returnUrl }';
			}
		});

		var initialPatient2 = jq('#patient2-select').val();

        updateErrorQueuePatientSummary(queueUuid, '1');
		if (initialPatient2) {
			updatePatientSummary(initialPatient2, '2');
		}
	});


	/**
	 * Updates a patient summary
	 * @param patientId the patient side
	 * @param position '1' or '2'
	 */
	function updatePatientSummary(patientId, position) {
		jq('.patient' + position + '-item').html('').addClass('ke-loading');

		ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'patientSummary', { patientId : patientId }, function (patient) {
			showDataPoints('#patient' + position + '-infopoints', patient.infopoints);
			showDataPoints('#patient' + position + '-names', patient.names);
			showDataPoints('#patient' + position + '-identifiers', patient.identifiers);
			showDataPoints('#patient' + position + '-attributes', patient.attributes);
			showDataPoints('#patient' + position + '-encounters', patient.encounters);

			jq('.patient' + position + '-item').removeClass('ke-loading');
		});
	}

    /**
     * Updates a patient summary for error queue patient
     * @param queueUuid the queue uuid
     * @param position '1' or '2'
     */
    function updateErrorQueuePatientSummary(queueUuid, position) {
        jq('.patient' + position + '-item').html('').addClass('ke-loading');

        ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'errorQueueRegistrationSummary', { errorQueueUuid : queueUuid }, function (patient) {
            showDataPoints('#patient' + position + '-infopoints', patient.infopoints);
            showDataPoints('#patient' + position + '-names', patient.names);
            showDataPoints('#patient' + position + '-identifiers', patient.identifiers);
            showDataPoints('#patient' + position + '-attributes', patient.attributes);
            showDataPoints('#patient' + position + '-encounters', patient.encounters);

            jq('.patient' + position + '-item').removeClass('ke-loading');
        });
    }

	function showDataPoints(selector, dataPoints) {
		var html = '';
		for (var i = 0; i < dataPoints.length; ++i) {
			html += createDataPoint(dataPoints[i].label, dataPoints[i].value);
		}
		jq(selector).html(html);
	}

	function createDataPoint(label, value) {
		if (label) {
			return '<div class="ke-datapoint"><span class="ke-label">' + label + '</span>: <span class="ke-value">' + value + '</span></div>';
		} else {
			return '<div class="ke-datapoint"><span class="ke-value">' + value + '</span></div>';
		}
	}

</script>