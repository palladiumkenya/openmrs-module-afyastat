<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Back", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome")],
            [label: "Afyastat - Outgoing Queue", iconProvider: "afyastat", icon: "outgoing-reg-07-32x32.png", label: "Afyastat - Outgoing Queue", href: ui.pageLink("afyastat", "afyastatOutgoingQueue")]
    ]

    ui.includeJavascript("kenyaemrorderentry", "jquery.twbsPagination.min.js")
    ui.includeJavascript("afyastat", "jsonViewer/jquery.json-editor.min.js")

    ui.includeJavascript("afyastat", "bootstrap/bootstrap.bundle.min.js")
    ui.includeCss("afyastat", "bootstrap/bootstrap-iso.css")
    ui.includeCss("afyastat", "jsonViewer/jquery.json-viewer.css")
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
table {
    width: 95%;
}
th, td {
    padding: 5px;
    text-align: left;
    height: 30px;
    border-bottom: 1px solid #ddd;
}
tr:nth-child(even) {background-color: #f2f2f2;}
#pager li{
    display: inline-block;
}

#queue-pager li{
    display: inline-block;
}
#chk-select-all {
    display: block;
    margin-left: auto;
    margin-right: auto;
}
.selectElement {
    display: block;
    margin-left: auto;
    margin-right: auto;
}
.nameColumn {
    width: 260px;
}
.cccNumberColumn {
    width: 150px;
}
.dateRequestColumn {
    width: 120px;
}
.clientNameColumn {
    width: 120px;
}
.selectColumn {
    width: 40px;
    padding-left: 5px;
}
.actionColumn {
    width: 350px;
}
.sampleStatusColumn {
    width: 150px;
}
.sampleTypeColumn {
    width: 100px;
}

.pagination-sm .page-link {
    padding: .25rem .5rem;
    font-size: .875rem;
}
.page-link {
    position: relative;
    display: block;
    padding: .5rem .75rem;
    margin-left: -1px;
    line-height: 1.25;
    color: #0275d8;
    background-color: #fff;
    border: 1px solid #ddd;
}

.viewPayloadButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.viewPayloadButton:hover {
    background-color: orange;
    color: black;
}
.editPayloadButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.editPayloadButton:hover {
    background-color: orange;
    color: black;
}
.mergeButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.createButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.viewButton:hover {
    background-color: steelblue;
    color: white;
}
.mergeButton:hover {
    background-color: orange;
    color: black;
}
.createButton:hover {
    background-color: orange;
    color: black;
}
.page-content{
    background: #eee;
    display: inline-block;
    padding: 10px;
    max-width: 660px;
    font-weight: bold;
}
@media screen and (min-width: 676px) {
    .modal-dialog {
        max-width: 600px; /* New width for default modal */
    }
}
</style>

<div class="ke-page-sidebar">
    ${ui.includeFragment("kenyaui", "widget/panelMenu", [heading: "Back", items: menuItems])}
</div>

<div class="ke-page-content">

    <div>
        <fieldset>
            <legend>Afyastat - Incoming Queue summary</legend>
            <div>
                <table class="simple-table" width="100%">
                    <thead>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="15%">Total queue data</td>
                        <td>${queueData}</td>
                    </tr>
                    <tr>
                        <td width="15%">Total errors</td>
                        <td>${totalErrors}</td>
                    </tr>
                    <tr>
                        <td width="15%">Potential Registration duplicates</td>
                        <td>${registrationErrors}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </fieldset>
    </div>

    <div id="program-tabs" class="ke-tabs">
        <div class="ke-tabmenu">
            <div class="ke-tabmenu-item" data-tabid="queue_data">Queue data</div>

            <div class="ke-tabmenu-item" data-tabid="error_queue">Error queue</div>

        </div>

        <div class="ke-tab" data-tabid="queue_data">
            <table cellspacing="0" cellpadding="0" width="100%">
                <tr>
                    <td style="width: 99%; vertical-align: top">
                        <div class="ke-panel-frame">
                            <div class="ke-panel-heading">Queue data</div>

                            <div class="ke-panel-content">
                                <fieldset>
                                    <legend></legend>
                                    <table class="simple-table" width="100%">
                                        <thead>
                                        <tr>
                                            <th class="clientNameColumn">Client Name</th>
                                            <th class="cccNumberColumn">Discriminator</th>
                                            <th class="sampleTypeColumn">Form</th>
                                            <th class="dateRequestColumn">Date submitted</th>
                                            <th class="dateRequestColumn">Provider</th>
                                            <th class="actionColumn"></th>
                                        </tr>
                                        </thead>
                                        <tbody id="queue-list">

                                        </tbody>

                                    </table>

                                    <div id="queue-pager">
                                        <ul id="queuePagination" class="pagination-sm"></ul>
                                    </div>
                                </fieldset>
                            </div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>
        <div class="ke-tab" data-tabid="error_queue">
            <table id="error-queue-data" cellspacing="0" cellpadding="0" width="100%">
                <tr>
                    <td style="width: 99%; vertical-align: top">
                        <div class="ke-panel-frame">
                            <div class="ke-panel-heading">Error queue</div>

                            <div class="ke-panel-content">
                                    <fieldset>
                                        <legend></legend>
                                        <table class="simple-table" width="100%">
                                            <thead>

                                            <tr>
                                                <th class="clientNameColumn">Client Name</th>
                                                <th class="cccNumberColumn">Discriminator</th>
                                                <th class="sampleTypeColumn">Form</th>
                                                <th class="dateRequestColumn">Date processed</th>
                                                <th class="sampleStatusColumn">Message</th>
                                                <th class="dateRequestColumn">Provider</th>
                                                <th class="selectColumn"><input type="checkbox" id="chk-select-all"/></th>
                                                <th class="actionColumn">
                                                    <input type="button" id="requeueErrors" value="Re-queue Selection"/>
                                                    <input type="button" id="deleteErrors" value="Delete Selection"/>
                                                </th>
                                            </tr>
                                            </thead>
                                            <tbody id="error-list">

                                            </tbody>

                                        </table>

                                        <div id="pager">
                                            <ul id="errorPagination" class="pagination-sm"></ul>
                                        </div>
                                    </fieldset>
                            </div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>
    </div>

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
                    <div class="modal-footer modal-footer-primary">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="savePayloadButton btn btn-primary">Save and Requeue</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>

<script type="text/javascript">

    var selectedErrors = [];
    //On ready
    jq = jQuery;
    jq(function () {
        // apply pagination

        var errorPaginationDiv = jq('#errorPagination');
        var queuePaginationDiv = jq('#queuePagination');

        var errorListDisplayArea = jq('#error-list');
        var queueListDisplayArea = jq('#queue-list');

        var numberOfErrorRecords = ${ errorListSize };
        var numberOfRecordsToProcess = ${ queueListSize };

        var errorRecords = ${ errorList };
        var queueRecords = ${ queueList };

        var errorDataDisplayRecords = [];
        var queueDataDisplayRecords = [];

        var recPerPage = 10;
        var errorStartPage = 1;
        var queueStartPage = 1;
        var totalErrorPages = Math.ceil(numberOfErrorRecords / recPerPage);
        var totalQueuePages = Math.ceil(numberOfRecordsToProcess / recPerPage);

        var visibleErrorPages = 1;
        var visibleQueuePages = 1;

        var payloadEditor = {};

        if (totalErrorPages <= 5) {
            visibleErrorPages = totalErrorPages;
        } else {
            visibleErrorPages = 5;
        }

        if (totalQueuePages <= 5) {
            visibleQueuePages = totalQueuePages;
        } else {
            visibleQueuePages = 5;
        }


        if(numberOfRecordsToProcess > 0) {
            apply_pagination(queuePaginationDiv, queueListDisplayArea, totalQueuePages, visibleQueuePages, queueRecords, queueDataDisplayRecords, 'queue', queueStartPage); // records in queue
        }

        if (numberOfErrorRecords > 0) {
            apply_pagination(errorPaginationDiv, errorListDisplayArea, totalErrorPages, visibleErrorPages, errorRecords, errorDataDisplayRecords, 'error', errorStartPage); // records in error
        }

        function apply_pagination(paginationDiv, recordsDisplayArea, totalPages, visiblePages, allRecords, recordsToDisplay, tableId, page) {
            paginationDiv.twbsPagination({
                totalPages: totalPages,
                visiblePages: visiblePages,
                onPageClick: function (event, page) {
                    displayRecordsIndex = Math.max(page - 1, 0) * recPerPage;
                    endRec = (displayRecordsIndex) + recPerPage;
                    //jq('#page-content').text('Page ' + page);
                    recordsToDisplay = allRecords.slice(displayRecordsIndex, endRec);
                    generate_table(recordsToDisplay, recordsDisplayArea, tableId);
                }
            });
        }

        jq(document).on('click','.mergeButton',function(){
            ui.navigate('afyastat', 'mergePatients', { queueUuid: jq(this).val(),  returnUrl: location.href });
        });

        jq(document).on('click','.viewPayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Checking for queue entry with uuid: " + queueUuid);

            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'getMessagePayload', { queueUuid : queueUuid }, function (result) {
                let payloadObject = [];
                try {
                    payloadObject = JSON.parse(result.payload);
                } catch(ex) {
                    payloadObject = JSON.parse("{}")
                }
                
                jq('#json-view-display').empty();
                jq('#json-view-display').jsonViewer(payloadObject,{
                    withQuotes:true,
                    rootCollapsable:true
                });
            });

            jq('#showViewPayloadDialog').modal('show');
        });

        jq(document).on('click','.editPayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Checking for queue entry with uuid: " + queueUuid);

            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'getMessagePayload', { queueUuid : queueUuid }, function (result) {
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
        });

        jq(document).on('click','.savePayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Got the edited entry with uuid: " + queueUuid);

            let newPayload = "";
            try {
                newPayload = JSON.stringify(payloadEditor.get());
                ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'updateMessagePayload', { queueUuid : queueUuid, payload : newPayload}, function (result) {
                    if(result)
                    {
                        console.log("Payload Successfully Edited");
                        alert("Payload Successfully Edited");
                        let selectedError = [];
                        selectedError.push(queueUuid);
                        let listToSubmit = selectedError.join();
                        ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'requeueErrors', { errorList : listToSubmit }, function (result) {
                            document.location.reload();
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

        // used to create new registration and bypass any patient matching on the provided patient demographics
        jq(document).on('click','.createButton',function () {
            var queueUuid = jq(this).val();
            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'createNewRegistration', { queueUuid : queueUuid }, function (result) {
                document.location.reload();
            });
        });

        // population selection list
        jq(document).on('click','.selectElement',function () {
            var queueUuid = jq(this).val();
            if (jq(this).is(":checked")) {
                selectedErrors.push(queueUuid);
            }
            else {
                 var elemIndex = selectedErrors.indexOf(queueUuid);
                 if (elemIndex > -1) {
                    selectedErrors.splice(elemIndex, 1);
                 }
                 jq('#chk-select-all').prop('checked', false);
             }
        });

        // handle select all
        jq(document).on('click','#chk-select-all',function () {
           if(jq(this).is(':checked')) {
                jq('.selectElement').prop('checked', true);
                selectedErrors = [];
           }
           else {
                jq('.selectElement').prop('checked', false);
           }

        });

        // handles button than re-queues errors
        jq(document).on('click','#requeueErrors',function () {
            // clear previously entered values
            if(confirm("Are you sure you want to requeue these queue errors?") == true) {
                //TODO: can we also check if the select all checkbox is checked?
                var listToSubmit = selectedErrors.length > 0 ? selectedErrors.join() : 'all';
                //selectedErrors
                ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'requeueErrors', { errorList : listToSubmit }, function (result) {
                    document.location.reload();
                });
            }
        });

        // handles button for deleting errors
        jq(document).on('click','#deleteErrors',function () {
            // clear previously entered values
            if(confirm("Are you sure you want to delete these queue errors?") == true) {
                //TODO: can we also check if the select all checkbox is checked?
                var listToSubmit = selectedErrors.length > 0 ? selectedErrors.join() : 'all';
                //selectedErrors
                ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'purgeErrors', { errorList : listToSubmit }, function (result) {
                    document.location.reload();
                });
            }
        });
    });

    function generate_table(displayRecords, displayObject, tableId) {
        var tr;
        displayObject.html('');
        for (var i = 0; i < displayRecords.length; i++) {

            tr = jq('<tr/>');
            tr.append("<td>" + displayRecords[i].clientName + "</td>");

            tr.append("<td>" + displayRecords[i].discriminator + "</td>");
            tr.append("<td>" + displayRecords[i].formName + "</td>");
            if (tableId === 'error') {
                tr.append("<td>" + displayRecords[i].dateProcessed + "</td>");
            } else {
                tr.append("<td>" + displayRecords[i].dateSubmitted + "</td>");
            }
            if (tableId === 'error') {
                tr.append("<td>" + displayRecords[i].message + "</td>");
            }
            tr.append("<td>" + displayRecords[i].provider + "</td>");           

            if (tableId === 'error') {
                var selectTd = jq('<td/>');
                var selectCheckbox = jq('<input/>', {
                    type: 'checkbox',
                    class: 'selectElement',
                    value: displayRecords[i].uuid
                });

                selectTd.append(selectCheckbox);
                tr.append(selectTd);

                var actionTd = jq('<td/>');

                var btnView = jq('<button/>', {
                    text: 'View',
                    class: 'viewPayloadButton',
                    value: displayRecords[i].uuid
                });

                var btnEdit = jq('<button/>', {
                    text: 'Edit',
                    class: 'editPayloadButton',
                    value: displayRecords[i].uuid
                });

                actionTd.append(btnView);
                actionTd.append(btnEdit);

                tr.append(actionTd);
            }
            if (tableId === 'error' && displayRecords[i].discriminator === 'json-registration' && displayRecords[i].message.includes('Found a patient with similar characteristic')) {
                var btnMerge = jq('<button/>', {
                    text: 'Merge',
                    class: 'mergeButton',
                    value: displayRecords[i].uuid
                });
                actionTd.append(btnMerge);
                tr.append(actionTd);
            }

            if (tableId === 'error' && displayRecords[i].discriminator === 'json-registration' && displayRecords[i].message.includes('Found a patient with similar characteristic')) {
                var btnCreateNewRegistration = jq('<button/>', {
                    text: 'Register',
                    class: 'createButton',
                    value: displayRecords[i].uuid
                });
                actionTd.append(btnCreateNewRegistration);
                tr.append(actionTd);
            }

            displayObject.append(tr);
        }
    }


</script>