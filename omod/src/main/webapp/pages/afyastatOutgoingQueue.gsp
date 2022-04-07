<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Afyastat - Incoming Queue", iconProvider: "afyastat", icon: "incoming-reg-07-32x32.png", label: "Afyastat - Incoming Queue", href: ui.pageLink("afyastat", "afyastatHome")]
    ]

    ui.includeJavascript("kenyaemrorderentry", "jquery.twbsPagination.min.js")
    ui.includeJavascript("afyastat", "jsonViewer/jquery.json-editor.min.js")
    ui.includeJavascript("afyastat", "bootstrap/bootstrap.bundle.min.js")   

    ui.includeCss("afyastat", "bootstrap/bootstrap-iso.css")
    ui.includeCss("afyastat", "jsonViewer/jquery.json-viewer.css")
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

</script>