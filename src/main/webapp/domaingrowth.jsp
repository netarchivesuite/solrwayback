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
    <title>Domain growth</title>

    <link rel="stylesheet" type="text/css" media="all" href="./css/solrwayback.css">


    <script src="https://d3js.org/d3.v4.min.js"></script>

    <script type="text/javascript" src="js/jquery-3.2.1.min.js"></script>
    <script type="text/javascript" src="js/vue.js"></script>
    <script type="text/javascript" src="js/vue-resource.min.js"></script>

    <script type="text/javascript" src="js/vue-router.js"></script>

</head>

<body>
<div class="wrapper" id="app">

    <header-container :url="url"></header-container>

    <div class="chart"></div>

    <div v-if="spinner" id="overlay"></div>
    <div v-if="spinner" id="spinnerVue">Loading...</div>

</div>


<!-- This include must be at bottom -->
<script type="text/javascript" charset="utf-8" src="js/app-growth.js"></script>

</body>
</html>