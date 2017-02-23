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