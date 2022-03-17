<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Afyastat - Incoming Queue", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Afyastat - Incoming Queue", href: ui.pageLink("afyastat", "afyastatHome")]
    ]

%>
<style>

</style>

<div class="ke-page-sidebar">
    ${ui.includeFragment("kenyaui", "widget/panelMenu", [heading: "Back", items: menuItems])}
</div>

<div class="ke-page-content">

    <div>
        <fieldset>
            <legend>Afyastat - add client/patient to afyastat registration queue</legend>
            ${ui.includeFragment("afyastat", "addRegistrationToQueue")}
        </fieldset>
    </div>
</div>

<script type="text/javascript">

    //On ready
    jq = jQuery;
    jq(function () {

    });

</script>