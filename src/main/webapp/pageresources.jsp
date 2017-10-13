<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">

    <title>SOLR Wayback page resources</title>
    <link rel="stylesheet" type="text/css" media="all" href="./css/solrwayback.css">

    <script type="text/javascript" src="js/jquery-1.10.2.min.js"></script>

    <script type="text/javascript" src="js/vue.js"></script>
    <script type="text/javascript" src="js/vue-resource.min.js"></script>
    <script type="text/javascript" src="js/vue-router.js"></script>
</head>
<body>
<div class="wrapper" id="app">

    <page-resources :resource-obj="resourceObj"></page-resources>

    <div v-if="spinner" id="overlay"></div>
    <div v-if="spinner" id="spinnerVue">Loading...</div>

</div>
<!-- This include must be at bottom -->
<script type="text/javascript" charset="utf-8" src="./js/app-resources.js"></script>
</body>
</html>