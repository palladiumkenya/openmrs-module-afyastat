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
.viewPayloadButton {
    background-color: cadetblue;
    color: white;
    margin-right: 10px;
    margin-left: 10px;
}
.viewPayloadButton:hover {
    background-color: orange;
    color: black;
}
.editPayloadButton {
    background-color: cadetblue;
    color: white;
    margin-right: 10px;
    margin-left: 10px;
}
.editPayloadButton:hover {
    background-color: orange;
    color: black;
}
@media screen and (min-width: 676px) {
    .modal-dialog {
        max-width: 600px; /* New width for default modal */
    }
}
</style>

<div class="ke-page-content">
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
                        <select name="status" id="purpose" class="purpose">
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
                        <button type="button" id="fetch-registry-entry" disabled> Fetch details</button>
                        <button type="button" class="cancel-button" id="queue-entry" disabled> Add to queue</button>
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
        <table class="simple-table" width="100%">
            <thead>
                <tr>
                    <th width="20%">Client/Patient</th>
                    <th width="20%">Purpose</th>
                    <th width="20%">Date added</th>
                    <th width="10%">Status</th>
                    <th class="actionColumn">Actions</th>
                </tr>
            </thead>
            <tbody id="queue-list">
            </tbody>
        </table>

    </fieldset>

    <div class="bootstrap-iso">
        <div class="modal fade" id="showViewPayloadDialog" tabindex="-1" role="dialog" aria-labelledby="backdropLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-xl" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="backdropLabel">View Payload</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <span style="color: firebrick" id="msgBox"></span>
                        <pre id="json-view-display"></pre>
                    </div>
                    <div class="modal-footer modal-footer-primary">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="showEditPayloadDialog" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" role="dialog" aria-labelledby="staticBackdropLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-xl" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="staticBackdropLabel">Edit Payload</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <span style="color: firebrick" id="msgBox"></span>
                        <pre id="json-edit-display"></pre>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="savePayloadButton btn btn-primary">Save And Requeue</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

<script type="text/javascript">
    jq(function() {
        //display area
        var queueListDisplayArea = jq('#queue-list');
        var payloadEditor = {};

        function display_message(msg) {
            jq("#lblText").html(msg);
            jq('#message').fadeIn('slow').delay(3000).fadeOut('slow');
        }

        function checkVarIfEmptyOrNull(msg) {
            return((msg && (msg = msg.trim())) ? true : false);
        }

        jq(".purpose").change(function() {
            let purpose = jq(".purpose").val();
            let patient = jq('input[name=person]').val();
            if(checkVarIfEmptyOrNull(purpose) && checkVarIfEmptyOrNull(patient))
            {
                jq('#queue-entry').attr('disabled', false);
            } else {
                jq('#queue-entry').attr('disabled', true);
            }
        });

        jq('input[name=person]').change(function() {
            let purpose = jq(".purpose").val();
            let patient= jq('input[name=person]').val();
            if(checkVarIfEmptyOrNull(patient))
            {
                jq('#fetch-registry-entry').attr('disabled', false);
                if(checkVarIfEmptyOrNull(purpose))
                {
                    jq('#queue-entry').attr('disabled', false);
                } else {
                    jq('#queue-entry').attr('disabled', true);
                }
            } else {
                jq('#fetch-registry-entry').attr('disabled', true);
            }
        });

        jq(document).on('click','.viewPayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Checking for queue entry with uuid: " + queueUuid);

            try {
                ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'getQueueEntryPayload', { queueUuid : queueUuid }, function (result) {
                    let payloadObject = [];
                    try {
                        payloadObject = JSON.parse(result.payload);
                    } catch(ex) {
                        payloadObject = JSON.parse("{}")
                    }
                    
                    jq('#json-view-display').empty();
                    jq('#json-view-display').jsonViewer(JSON.parse(result.payload),{
                        withQuotes:true,
                        rootCollapsable:true
                    });
                });

                jq('#showViewPayloadDialog').modal('show');
            } catch (ex) {
                alert(ex);
            }
        });

        jq(document).on('click','.editPayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Checking for queue entry with uuid: " + queueUuid);

            try {
                ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'getQueueEntryPayload', { queueUuid : queueUuid }, function (result) {
                    let payloadObject = [];
                    try {
                        payloadObject = JSON.parse(result.payload);
                    } catch(ex) {
                        payloadObject = JSON.parse("{}")
                    }

                    jq('#json-edit-display').empty();
                    payloadEditor = new JsonEditor('#json-edit-display', payloadObject,{
                        withQuotes:true,
                        rootCollapsable:true
                    });
                    jq('.savePayloadButton').val(queueUuid);
                });

                jq('#showEditPayloadDialog').modal('show');
            } catch (ex) {
                alert(ex);
            }
        });

        jq(document).on('click','.savePayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Got the edited entry with uuid: " + queueUuid);

            let newPayload = "";
            try {
                newPayload = JSON.stringify(payloadEditor.get());
                ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'updateMessagePayload', { queueUuid : queueUuid, payload : newPayload}, function (result) {
                    if(result)
                    {
                        console.log("Payload Successfully Edited");
                        alert("Payload Successfully Edited");

                        ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'requeueOutgoingRecord', { queueUuid : queueUuid }, function (result) {
                            fetchRegistryEntry();
                        });

                        jq('#showEditPayloadDialog').modal('hide');
                    } else {
                        console.log("Error Editing Payload");
                        alert("Error Editing Payload");
                    }
                });
            } catch (ex) {
                console.log("Payload JSON Error: " + ex);
                alert(ex);
            }
        });

        // handle click event of fetch entry button
        jq(document).on('click','#fetch-registry-entry',function () {
            fetchRegistryEntry();
        });

        function fetchRegistryEntry() {
            var selectedPerson = jq('input[name=person]').val();

            ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'getOutgoingEntryForPatient', { personId : selectedPerson }, function (result) {
                let queueRecords = result;
                jq("#queue-list").empty();
                if (queueRecords) {
                    if(queueRecords.length > 0) {
                        console.log("Generating Table: " + queueRecords.length);
                        generate_patient_table(queueRecords, queueListDisplayArea);
                        display_message('Found Patient Records in the queue');
                    } else {
                        console.log("Not Generating Table: No data available");
                        display_message('NO Patient Records in the queue');
                    }
                } else {
                    console.log("Not Generating Table: No data available");
                    display_message('Error nothing in the queue');
                }
            });
        }

        // handle click event of add to queue button
        jq(document).on('click','#queue-entry',function () {

            var selectedPerson = jq('input[name=person]').val();
            var purpose = jq('#purpose').val();

            ui.getFragmentActionAsJson('afyastat', 'addRegistrationToQueue', 'addOutgoingEntryForPatient', { personId : selectedPerson, purpose : purpose }, function (result) {
                let queueRecords = result;
                jq('#queue-list').empty();
                if (queueRecords) {
                    if(queueRecords.length > 0) {
                        console.log("Generating Table: " + queueRecords.length);
                        generate_patient_table(queueRecords, queueListDisplayArea);
                        display_message('Record queued successfully.');
                    } else {
                        console.log("Not Generating Table: No data available");
                        display_message('Failed to queue record');
                    }
                } else {
                    console.log("Not Generating Table: No data available");
                    display_message('Error: Failed to queue record');
                }
            });
        });
    });

    function generate_patient_table(displayRecords, displayObject) {
        var tr;
        displayObject.html('');

        for (var i = 0; i < displayRecords.length; i++) {

            if(displayRecords[i].hasEntry) {
                tr = jq('<tr/>');
                tr.append("<td>" + displayRecords[i].clientName + "</td>");
                tr.append("<td>" + displayRecords[i].purpose + "</td>");
                tr.append("<td>" + displayRecords[i].dateCreated + "</td>");
                tr.append("<td>" + displayRecords[i].status + "</td>");
                var actionTd = jq('<td/>');

                var btnView = jq('<button/>', {
                    text: 'View Payload',
                    class: 'viewPayloadButton',
                    value: displayRecords[i].uuid
                });
                actionTd.append(btnView);

                /** 
                 * Uncomment if Edit Button is required for editing the payload on outgoing queue
                 * 
                var btnEdit = jq('<button/>', {
                    text: 'Edit Payload',
                    class: 'editPayloadButton',
                    value: displayRecords[i].uuid
                });
                actionTd.append(btnEdit);
                */

                tr.append(actionTd);

                displayObject.append(tr);
            } else {
                console.log("No Table Entry");
            }

        }
    }

</script>