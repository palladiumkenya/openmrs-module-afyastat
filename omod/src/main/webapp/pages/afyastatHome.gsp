<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Back", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome")]
    ]

    ui.includeJavascript("kenyaemrorderentry", "jquery.twbsPagination.min.js")
    ui.includeJavascript("afyastat", "jsonViewer/jquery.json-editor.min.js")

    ui.includeJavascript("kenyaemrorderentry", "bootstrap.min.js")
    ui.includeCss("kenyaemrorderentry", "bootstrap.min.css")
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
.nameColumn {
    width: 260px;
}
.cccNumberColumn {
    width: 150px;
}
.dateRequestColumn {
    width: 120px;
}
.selectColumn {
    width: 40px;
    padding-left: 5px;
}
.actionColumn {
    width: 250px;
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

.viewButton {
    background-color: cadetblue;
    color: white;
}
.mergeButton {
    background-color: cadetblue;
    color: white;
}
.viewButton:hover {
    background-color: steelblue;
    color: white;
}
.mergeButton:hover {
    background-color: steelblue;
    color: white;
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
            <legend>Queue summary</legend>
            <div>
                <table class="simple-table" width="30%">
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
                                    <table class="simple-table" width="90%">
                                        <thead>
                                        <tr>
                                            <th class="nameColumn">Patient/UUID</th>
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
                                        <table class="simple-table" width="90%">
                                            <thead>

                                            <tr>
                                                <th class="nameColumn">Error data UUID</th>
                                                <th class="cccNumberColumn">Discriminator</th>
                                                <th class="sampleTypeColumn">Form</th>
                                                <th class="dateRequestColumn">Date processed</th>
                                                <th class="sampleStatusColumn">Message</th>
                                                <th class="dateRequestColumn">Provider</th>
                                                <th class="selectColumn"><input type="checkbox" id="chk-select-all"/></th>
                                                <th class="actionColumn">
                                                    <input type="button" id="requeueErrors" value="Re-queue"/>
                                                    <input type="button" id="deleteErrors" value="Delete"/>
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

    <div class="modal fade" id="showPayloadDialog" tabindex="-1" role="dialog" aria-labelledby="dateModalCenterTitle" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header modal-header-primary">
                    <h5 class="modal-title" id="dateVlModalCenterTitle">Payload</h5>
                    <button type="button" class="close closeDialog" data-dismiss="modal">&times;</button>
                </div>
                <div class="modal-body">
                    <span style="color: firebrick" id="msgBox"></span>
                    <pre id="json-display"></pre>
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

        jq(document).on('click','.viewButton',function () {
            // clear previously entered values


            var queueUuid = jq(this).val();

            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'getMessagePayload', { queueUuid : queueUuid }, function (result) {
                jq('#json-display').jsonViewer(JSON.parse(result.payload),{
                    withQuotes:true,
                    rootCollapsable:true
                });
            });

            jq('#showPayloadDialog').modal('show');
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
             }
            console.log(selectedErrors);
        });

        jq(document).on('click','#requeueErrors',function () {
            // clear previously entered values
            var listToSubmit = selectedErrors.length > 0 ? selectedErrors.join() : 'all';
            //selectedErrors
            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'requeueErrors', { errorList : listToSubmit }, function (result) {
                document.location.reload();
            });

        });
    });

    function generate_table(displayRecords, displayObject, tableId) {
        var tr;
        displayObject.html('');
        for (var i = 0; i < displayRecords.length; i++) {

            tr = jq('<tr/>');
            if (tableId === 'queue') {
                tr.append("<td>" + (displayRecords[i].clientName != "" ? displayRecords[i].clientName : displayRecords[i].patientUuid) + "</td>");
            } else {
                tr.append("<td>" + displayRecords[i].patientUuid + "</td>");
            }

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
            }
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'View Payload',
                class: 'viewButton',
                value: displayRecords[i].uuid
            });
            actionTd.append(btnView);
            tr.append(actionTd);

            if (tableId === 'error' && displayRecords[i].discriminator === 'json-registration' && displayRecords[i].message.includes('Found a patient with similar characteristic')) {
                var btnMerge = jq('<button/>', {
                    text: 'Merge',
                    class: 'mergeButton',
                    value: displayRecords[i].uuid
                });
                actionTd.append(btnMerge);
                tr.append(actionTd);
            }

            displayObject.append(tr);
        }
    }


</script>