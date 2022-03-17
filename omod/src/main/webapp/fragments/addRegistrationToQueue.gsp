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
                        <option value="hts">HIV Testing services</option>
                        <option value="linkage">HIV Linkage</option>
                        <option value="kp">Key Population</option>
                    </select>
                </td>
            </tr>
            <tr></tr>
            <tr></tr>
            <tr></tr>
            <tr></tr>
            <tr>
                <td colspan="2">
                    <button type="button"> Fetch details</button>
                	<button type="button" class="cancel-button"> Add to queue</button>
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
            <td>Chairman Kibor</td>
            <td>Tracing and linkage</td>
            <td>22-11-2021</td>
            <td>Sent</td>
        </tr>
    </table>

</fieldset>

<script type="text/javascript">
jq(function() {

});
</script>