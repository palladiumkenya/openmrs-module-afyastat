<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Afyastat - Incoming Queue", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Afyastat - Incoming Queue", href: ui.pageLink("afyastat", "afyastatHome")]
    ]

    ui.includeJavascript("kenyaemrorderentry", "jquery.twbsPagination.min.js")
%>
<style>

</style>

<div class="ke-page-sidebar">
    ${ui.includeFragment("kenyaui", "widget/panelMenu", [heading: "Back", items: menuItems])}
</div>

<div class="ke-page-content">

    <div>
        <fieldset>
            <legend>Afyastat - add client/patient to afyastat outgoing registration queue</legend>
            ${ui.includeFragment("afyastat", "addRegistrationToQueue")}
        </fieldset>
    </div>

    <div>
        <fieldset>
            <legend>Afyastat - Outgoing Registration Queue</legend>
            ${ui.includeFragment("afyastat", "outgoingRegistrationQueue")}
        </fieldset>
    </div>

</div>

<script type="text/javascript">

    //On ready
    jq = jQuery;
    jq(function () {

    });

    function generate_table(displayRecords, displayObject) {
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