<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>
<%@ page import="
    java.util.*,
    dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview,
    dk.kb.netarchivesuite.solrwayback.facade.Facade"%>

<!DOCTYPE html>


<%
String url = (String) request.getParameter("url");

ArrayList<PagePreview> allPreviews= Facade.getPagePreviewsForUrl(url);


ArrayList<PagePreview> show10Previews = new ArrayList<PagePreview>();

for (int i =0;i<4;i++){
  show10Previews.add(allPreviews.get((int) (Math.random()*allPreviews.size())));  
}

%>


<html>
<head>
    <meta charset="UTF-8">
    <title>Page previews</title>

    <link rel="stylesheet" type="text/css" media="all" href="./css/jquery-ui.min.css">
    <link rel="stylesheet" type="text/css" media="all" href="./css/solrwayback.css">

    <script type="text/javascript" src="js/jquery-1.10.2.min.js"></script>
    <script type="text/javascript" src="js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="js/vue.js"></script>
    <script type="text/javascript" src="js/vue-resource.min.js"></script>

    <script type="text/javascript">
        var url = "<%=url %>";
    </script>
</head>

<body>

<div class="wrapper" id="app">

    <header-container :harvest-data="harvestData" :url="url"></header-container>

    <harvests :harvest-data="harvestData"></harvests>

</div>
<!-- This include must be at bottom -->
<script type="text/javascript" charset="utf-8" src="js/app-preview.js"></script>

<script type="text/javascript">
    /* Datepicker */
    $('.datepicker').datepicker({
        inline: true,
        showOtherMonths: true,
        dayNamesMin: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
        firstDay: 1,
        changeYear: true,
        yearRange: "c-50:c",
        changeMonth: true,
        dateFormat: "dd-mm-yy",
    });
</script>
<script type="text/javascript">
    $( function() {
        $( ".slider" ).slider({
            value: 2005,
            min: 2005,
            max: 2017,
            slide: function( event, ui ) {
                $( "#year" ).val(ui.value );
            }
        });
        $( "#year" ).val( $( ".slider" ).slider( "value" ) );
    } );
</script>

</body>
</html>


