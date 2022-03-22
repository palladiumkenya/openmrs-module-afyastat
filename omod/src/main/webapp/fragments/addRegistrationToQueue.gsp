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
<div id="message"><span id="lblText" style="color: Red; top: 50px;"></span></div>
</br>
<fieldset>
    <legend>Client/Patient information in the registration queue</legend>
    </br>
    <table class="simple-table" width="60%">
        <thead>
            <tr>
                <th width="30%">Client/Patient</th>
                <th width="20%">Purpose</th>
                <th width="20%">Date added</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody id="queue-list">
        </tbody>
    </table>

</fieldset>

<script type="text/javascript">
    jq(function() {
        //display area
        var queueListDisplayArea = jq('#queue-list');

        // handle click event of fetch entry button
        jq(document).on('click','#fetch-registry-entry',function () {

            var selectedPerson = jq('input[name=person]').val();

            ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'getOutgoingEntryForPatient', { personId : selectedPerson }, function (result) {
                let queueRecords = result;
                jq("#queue-list").empty();
                if (queueRecords) {
                    if(queueRecords.length > 0) {
                        console.log("Generating Table: " + queueRecords.length);
                        generate_table(queueRecords, queueListDisplayArea);
                        jq("#lblText").html('Found Patient Records in the queue');
                        jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
                    } else {
                        console.log("Not Generating Table: No data available");
                        jq("#lblText").html('NO Patient Records in the queue');
                        jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
                    }
                } else {
                    console.log("Not Generating Table: No data available");
                    jq("#lblText").html('Error nothing in the queue');
                    jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
                }
            });
        });

        // handle click event of add to queue button
        jq(document).on('click','#queue-entry',function () {

            var selectedPerson = jq('input[name=person]').val();
            var purpose = jq('#purpose').val();

            ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'addOutgoingEntryForPatient', { personId : selectedPerson, purpose : purpose }, function (result) {
                let queueRecords = result;
                jq("#queue-list").empty();
                if (queueRecords) {
                    if(queueRecords.length > 0) {
                        console.log("Generating Table: " + queueRecords.length);
                        generate_table(queueRecords, queueListDisplayArea);
                        jq("#lblText").html('Record queued successfully.');
                        jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
                    } else {
                        console.log("Not Generating Table: No data available");
                        jq("#lblText").html('Failed to queue record');
                        jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
                    }
                } else {
                    console.log("Not Generating Table: No data available");
                    jq("#lblText").html('Error: Failed to queue record');
                    jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
                }
            });
        });
    });

</script>