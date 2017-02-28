<!DOCTYPE html>
<html>
<head>
    <title>Calendar Graph</title>
    <meta charset="UTF-8">
    <link rel="stylesheet" type="text/css" href="css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="css/bootstrap-theme.min.css">
    <script>
        // Hand over configuration from JSP to Javascript
        window.solrWaybackConfig = {};
        window.solrWaybackConfig.url = "<%= request.getParameter("url") %>";
    </script>
</head>

<body>
    <div class="col-md-8 col-md-offset-2">
        <div id="app">
            <harvest-title :url="url"></harvest-title>
            <harvest-date :url="url"></harvest-date>
        </div>
    </div>
    <script src="js/calendar-widget/dist/bundle.js"></script>
</body>
</html>
