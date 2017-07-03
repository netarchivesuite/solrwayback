(function () {

    var uploadfiles = document.querySelector('#uploadfiles');
    uploadfiles.addEventListener('change', function () {
        var files = this.files;
        for(var i=0; i<files.length; i++){
            uploadFile(this.files[i]);
        }

    }, false);


    /**
     * Upload a file
     * @param file
     */
    function uploadFile(file){
        var url = "services/upload/gethash";
        var xhr = new XMLHttpRequest();
        var fd = new FormData();
        var sha1;
        xhr.open("POST", url, true);
        xhr.onreadystatechange = function() {        	
        	if (xhr.readyState == 4 && xhr.status == 200) {               
            	// Every thing ok, file uploaded. Maybe do error handling?
                console.log(xhr.responseText); // log sha1 to console
                sha1=xhr.responseText; //Set variable... TODO: make it upload search field to hash:"sha1"
        	}
        };
        fd.append('file', file);
        xhr.send(fd);
    }
}());