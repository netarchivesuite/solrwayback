// This is the VueJS frontend for calendar.jsp

//dateformat YYYYMMDD
function formatDate(date){
    var datestring = date.getFullYear()+ ("0"+(date.getMonth()+1)).slice(-2) + ("0" + date.getDate()).slice(-2);
    return datestring;
}

function formatDateFull(date){
    var datestring = date.getFullYear()+ ("0"+(date.getMonth()+1)).slice(-2) + ("0" + date.getDate()).slice(-2)+("0" + date.getHours()).slice(-2)+("0" + date.getMinutes()).slice(-2)+("0" + date.getSeconds()).slice(-2);
    return datestring;
}

function formatDateHuman(date){
    var datestring = date.getFullYear()+ '-'+ ("0"+(date.getMonth()+1)).slice(-2) + '-'+ ("0" + date.getDate()).slice(-2)+' '+("0" + date.getHours()).slice(-2)+':'+("0" + date.getMinutes()).slice(-2)+':'+("0" + date.getSeconds()).slice(-2);
    return datestring;
}

function generateCrawlLinks(date){
    var dateInput= formatDate(date);  //YYYYMMDD
    var html ='';
    for (let item of crawltimeSet.values()){
        var dateInput1 =  formatDate(new Date(item));
        if  (dateInput == dateInput1){
            console.log(new Date(item));
            html = html + '<p><a href="'+window.solrWaybackConfig.solrWaybackUrl+formatDateFull(new Date(item))+'/'+window.solrWaybackConfig.url+ '" target="new">' +formatDateHuman(new Date(item))+'</a></p>';
        }
    }

    return html;
}

$(function() {          // Runs after DOM is loaded.
    $('#calendar').calendar({
        clickDay: function(event) {
            $("#modalTitle").html( formatDate(event.date) );
            $("#modalBody").html( generateCrawlLinks(event.date));
            $("#myModal").modal('show');
        },

        customDayRenderer: function(element, date) {
            if(dateSet.has(formatDate(date))) {
                $(element).css('background-color', 'red');
                $(element).css('color', 'white');
                $(element).css('border-radius', '15px');
            }
        }
    });
});