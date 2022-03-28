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
#queue-pager li{
    display: inline-block;
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
</style>

<div class="ke-page-content">
    <div>
        <fieldset>
            <legend>Summary</legend>
            <div>
                <table class="simple-table" width="100%">
                    <thead>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="30%">Total Queue Data</td>
                        <td>${totalQueueCount}</td>
                    </tr>
                    <tr>
                        <td width="30%">Total Sent</td>
                        <td>${sentQueueCount}</td>
                    </tr>
                    <tr>
                        <td width="30%">Total Pending</td>
                        <td>${queueListSize}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </fieldset>
    </div>

    <div>
        <fieldset>
            <legend>Pending</legend>
            <div>
                <table class="simple-table" width="100%">
                    <thead>
                        <tr>
                            <th width="30%">Client/Patient</th>
                            <th width="20%">Purpose</th>
                            <th width="20%">Date added</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody id="total-queue-list">
                    </tbody>
                </table>
                <div id="queue-pager">
                    <ul id="queuePagination" class="pagination-sm"></ul>
                </div>
            </div>
        </fieldset>
    </div>
</div>

<script type="text/javascript">
    jq(function() {

        // apply pagination
        var queuePaginationDiv = jq('#queuePagination');
        var queueListDisplayArea = jq('#total-queue-list');
        var numberOfRecordsToProcess = ${ queueListSize };
        var queueRecords = ${ queueList };
        var queueDataDisplayRecords = [];

        var recPerPage = 10;
        var queueStartPage = 1;
        var totalQueuePages = Math.ceil(numberOfRecordsToProcess / recPerPage);
        var visibleQueuePages = 1;

        if (totalQueuePages <= 5) {
            visibleQueuePages = totalQueuePages;
        } else {
            visibleQueuePages = 5;
        }

        if(numberOfRecordsToProcess > 0) {
            apply_pagination(queuePaginationDiv, queueListDisplayArea, totalQueuePages, visibleQueuePages, queueRecords, queueDataDisplayRecords, queueStartPage); // records in queue
        }

        function apply_pagination(paginationDiv, recordsDisplayArea, totalPages, visiblePages, allRecords, recordsToDisplay, page) {
            paginationDiv.twbsPagination({
                totalPages: totalPages,
                visiblePages: visiblePages,
                onPageClick: function (event, page) {
                    displayRecordsIndex = Math.max(page - 1, 0) * recPerPage;
                    endRec = (displayRecordsIndex) + recPerPage;

                    recordsToDisplay = allRecords.slice(displayRecordsIndex, endRec);
                    generate_queue_table(recordsToDisplay, recordsDisplayArea);
                }
            });
        }

    });

    function generate_queue_table(displayRecords, displayObject) {
        var tr;
        displayObject.html('');

        for (var i = 0; i < displayRecords.length; i++) {

            if(displayRecords[i].hasEntry) {
                console.log("Found Table Entry");
                tr = jq('<tr/>');
                tr.append("<td>" + displayRecords[i].clientName + "</td>");
                tr.append("<td>" + displayRecords[i].purpose + "</td>");
                tr.append("<td>" + displayRecords[i].dateCreated + "</td>");
                tr.append("<td>" + displayRecords[i].status + "</td>");

                displayObject.append(tr);
            } else {
                console.log("No Table Entry");
            }

        }
    }

</script>
