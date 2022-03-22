<%
	def rows = [
		[
			[ object: command, property: "person", label: "" ]
		]
	]
%>
<style>
.simple-table {
    border: solid 1px #DDEEEE;
    border-collapse: collapse;
    border-spacing: 0;
    font: normal 13px Arial, sans-serif;
}
.simple-table thead th {

    border: solid 1px #DDEEEE;
    color: #336B6B;
    padding: 10px;
    text-align: left;
    text-shadow: 1px 1px 1px #fff;
}
.simple-table td {
    border: solid 1px #DDEEEE;
    color: #333;
    padding: 5px;
    text-shadow: 1px 1px 1px #fff;
}
</style>

<form id="add-entry-form" method="post" action="">

	<div class="ke-panel-content">

        <table class="simple-table">
            <tr>
                <td width="15%">Client/Patient</td>
                <td>
                    <% rows.each { %>
                        ${ ui.includeFragment("kenyaui", "widget/rowOfFields", [ fields: it ]) }
                    <% } %>
                </td>
            </tr>
            <tr></tr>
            <tr></tr>
            <tr>
                <td>Purpose</td>
                <td>
                    <select name="status" id="purpose">
                        <option></option>
                        <option value="testing">HIV Testing services</option>
                        <option value="linkage">HIV Linkage</option>
                        <option value="prep_verification">PrEP Verification</option>
                        <option value="treatment_verification">Treatment Verification</option>
                        <option value="kp_followup">KP Followup</option>
                    </select>
                </td>
            </tr>
            <tr></tr>
            <tr></tr>
            <tr></tr>
            <tr></tr>
            <tr>
                <td colspan="2">
                    <button type="button" id="fetch-registry-entry"> Fetch details</button>
                	<button type="button" class="cancel-button" id="queue-entry"> Add to queue</button>
                </td>
            </tr>

        </table>
		<div class="ke-form-globalerrors" style="display: none"></div>

	</div>

</form>
</br>
</br>
</br>
<fieldset>
    <legend>Client/Patient information in the registration queue</legend>
    </br>
    <table class="simple-table" width="60%">
        <tr>
            <td width="30%">Client/Patient</td>
            <td width="20%">Purpose</td>
            <td width="20%">Date added</td>
            <td>Status</td>
        </tr>
        <tr>
            <td id="entry-name"></td>
            <td id="entry-purpose"></td>
            <td id="entry-date">22-11-2021</td>
            <td id="entry-status">Sent</td>
        </tr>
    </table>

</fieldset>

<script type="text/javascript">
jq(function() {
    // handle click event of fetch entry button
    jq(document).on('click','#fetch-registry-entry',function () {

        var selectedPerson = jq('input[name=person]').val();

        ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'getOutgoingEntryForPatient', { personId : selectedPerson }, function (result) {
            if (result.hasEntry) {
                jq('#entry-name').text(result.patientName);
                jq('#entry-purpose').text(result.purpose);
                jq('#entry-date').text(result.dateCreated);
                jq('#entry-status').text(result.status);
            }
        });
    });

    // handle click event of add to queue button
    jq(document).on('click','#queue-entry',function () {

        var selectedPerson = jq('input[name=person]').val();
        var purpose = jq('#purpose').val();

        ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'addOutgoingEntryForPatient', { personId : selectedPerson, purpose : purpose }, function (result) {
            if (result) {
                jq('#entry-name').text(result.patientName);
                jq('#entry-purpose').text(result.purpose);
                jq('#entry-date').text(result.dateCreated);
                jq('#entry-status').text(result.status);
            }
        });
    });
});
</script>