<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>
<%@ page import="
    java.util.*,
    dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview,
    dk.kb.netarchivesuite.solrwayback.facade.Facade"%>

<!DOCTYPE html>


<%
String url = (String) request.getParameter("url");
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

        <div id="columns">
            <div class="column">
                <harvestinfo-container :harvest-data="harvestData"></harvestinfo-container>
            </div>
            <div class="column">
                <slider-container :harvest-data="harvestData" :show-preview="showPreview"></slider-container>
            </div>
            <div class="column">
                <datepicker-container :harvest-data="harvestData" :show-preview="showPreview"></datepicker-container>
            </div>
            <div class="column">
                <harvests-container :harvest-data="harvestData" :show-preview="showPreview" ></harvests-container>
            </div>
        </div>

        <preview-container :preview-data="previewData"></preview-container>

    </div>

<!-- This include must be at bottom -->
<script type="text/javascript" charset="utf-8" src="js/app-preview.js"></script>

</body>
</html>


