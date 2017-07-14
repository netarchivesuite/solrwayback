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
            <div class="column info">
                <harvestinfo-container :harvest-data="harvestData" :selected-date="selectedDate"></harvestinfo-container>
            </div>
            <div class="column">
                <slider-container :harvest-data="harvestData" :show-preview="showPreview" :hide-spinner="hideSpinner"></slider-container>
            </div>
            <div class="column otherMethods">
                <datepicker-container :harvest-data="harvestData" :show-preview="showPreview"></datepicker-container>
                <harvests-container :harvest-data="harvestData" :show-preview="showPreview" ></harvests-container>
            </div>
        </div>

        <preview-container  v-if="harvestData.length > 0" :preview-data="previewData"  :selected-date="selectedDate"></preview-container>


        <div v-if="spinner" id="overlay"></div>
        <div v-if="spinner" id="spinnerVue">Loading...</div>

    </div>


<!-- This include must be at bottom -->
<script type="text/javascript" charset="utf-8" src="js/app-preview.js"></script>

</body>
</html>


