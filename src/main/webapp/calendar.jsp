<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>
<%@ page import="
    java.util.*,         
    dk.kb.netarchivesuite.solrwayback.facade.Facade,
    dk.kb.netarchivesuite.solrwayback.properties.*,
    dk.kb.netarchivesuite.solrwayback.service.dto.*"%>

<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" type="text/css" href="css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="css/bootstrap-datepicker.min.css">
    <link rel="stylesheet" type="text/css" href="css/bootstrap-theme.min.css">
    <link rel="stylesheet" type="text/css" href="css/bootstrap-year-calendar.min.css">
    <link rel="stylesheet" type="text/css" href="css/solrwayback.css">

    <script src="js/respond.min.js"></script>
    <script src="js/jquery-1.10.2.min.js"></script>
    <script src="js/bootstrap.min.js"></script>
    <script src="js/bootstrap-datepicker.min.js"></script>
    <script src="js/bootstrap-year-calendar.min.js"></script>
    <script src="js/bootstrap-popover.js"></script>
    <script src="js/plotly-latest.min.js"></script>
    <script src="js/calendar-util.js"></script>

    <meta charset="UTF-8">
</head>

<body>
    <div id="wrapper">
        <%
            String url = request.getParameter("url");
            HarvestDates dates = Facade.getHarvestTimesForUrl(url);
        %>


        <h2 align="center"><%= url %> (#Harvest:<%=dates.getDates().size()%>)</h2>

        <div id="app">
            <harvest-date></harvest-date>
        </div>

        <div id="calendar" data-provide="calendar"></div>
        <div id="yearplot"></div>
    </div>

    <script>
        // Hand over configuration from JSP to Javascript
        window.solrWaybackConfig = {};
        window.solrWaybackConfig.solrWaybackUrl = "<%= PropertiesLoader.WAYBACK_BASEURL %>wayback?waybackdata=" + "/";
        // window.solrWaybackConfig.calendarHarvestDatesUrl = "<%= PropertiesLoader.WAYBACK_BASEURL %>wayback?waybackdata=" + "/";
        window.solrWaybackConfig.url = "<%= url %>";
    </script>

    <script>
        // Extract this to a REST call
        // dateSet is used in the calendar
        // crawltimeSet is used in the plotly graph.
        var dateSet = new Set();
        var crawltimeSet = new Set();

        <% for (Long d: dates.getDates()) { %>
            dateSet.add(formatDate(new Date(<%= d %>)));
            crawltimeSet.add(<%= d %>);
        <% } %>
        // </Extract>


        var xdata = Array.from(crawltimeSet);
        var ydata=[];

        for (var i in xdata) {
            ydata.push(1);
        }

        var data = [{
            x: xdata.map(toDate),
            y: ydata,
            type: 'bar'
        }];

        var layout = {
            xaxis: {
                tickformat: '%x %H:%M',
                type: 'date',
                zeroline:true, // this doesn't seem to work.
                showline:true, // this doesn't seem to work.
            },
            yaxis: {
                range: [0, 1],
                zeroline: false,
                showline: false,
                showticklabels:false,
                showgrid:false,
            }
        };

        Plotly.newPlot('yearplot', data, layout, {
            displayModeBar: false
        });

        var myPlot = document.getElementById('yearplot');

        myPlot.on('plotly_click', function(event){
            var time = event.points[0].x;
            var year = time.substring(0,4);
            $('#calendar').data('calendar').setYear(year);
        });

        function toDate(ts) {
            return new Date(ts);
        }
    </script>

    <div class="modal fade" id="myModal" role="dialog">
        <div class="modal-dialog">

            <!-- Modal content-->
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                    <h4 id="modalTitle" class="modal-title">Modal Header</h4>
                </div>
                <div  id="modalBody" class="modal-body">
                    <p>Some text in the modal.</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </div>
            </div>

        </div>
    </div>
    <script src="js/calendar-widget/dist/bundle.js"></script>
</body>
</html>
