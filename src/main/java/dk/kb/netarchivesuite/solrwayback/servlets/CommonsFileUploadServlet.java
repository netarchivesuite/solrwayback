package dk.kb.netarchivesuite.solrwayback.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;



public class CommonsFileUploadServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(CommonsFileUploadServlet.class);
	private static final String TMP_DIR_PATH = System.getProperty("java.io.tmpdir");
	private File tmpDir = new File(TMP_DIR_PATH);   
	private static final String UPLOAD_FOLDER =System.getProperty("java.io.tmpdir");	
	private File upload_folder_file = new File(UPLOAD_FOLDER);
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	    log.info("temp dir:"+TMP_DIR_PATH);
		DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
		/*
		 *Set the size threshold, above which content will be stored on disk. Just in case...
		 */
		fileItemFactory.setSizeThreshold(20*1024*1024); //20 MB
		/*
		 * Set the temporary directory to store the uploaded files of size above threshold.
		 */
		fileItemFactory.setRepository(tmpDir);

		ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
		try {
			/*
			 * Parse the request
			 */
			List<FileItem> items = uploadHandler.parseRequest(request);
			Iterator<FileItem> itr = items.iterator();

			FileItem item = itr.next(); // always 1

			/*
			 * Handle Form Fields.
			 */
			//Handle Uploaded files.
			String fieldName = item.getFieldName();
			String fileName = getFileNameFromPath(item.getName());
			String extension = FilenameUtils.getExtension(fileName);
			log.info("Upload Filename:"+fileName +" Extension:"+extension);
			
			String contentType = item.getContentType();
			long fileSize=item.getSize();
		
			/*
			 * Write uploaded PDF to download folder
			 */
			File file = new File(upload_folder_file, fileName);
			item.write(file);	
		   
			String sha1 = Sha1Hash.createSha1(file);
		    file.delete();
			request.setAttribute("message", "Upload OK");

			String searchText="hash:\""+sha1+"\"";
			String filterText="";
			
		    SearchResult result = Facade.search(searchText, filterText);	          
	        request.setAttribute("searchResult", result);       
	        request.setAttribute("searchText",searchText);
	        request.setAttribute("filterText",filterText);	        
	        request.setAttribute("tab","0");
		    
			RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
			dispatcher.forward(request, response);							

		}
		catch(Exception ex) {
			log.error("Error encountered while uploading file",ex);
			request.setAttribute("message", "Fejl ved upload:"+ex.getMessage());

			RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
			dispatcher.forward(request, response);		 
		}

	}

    //  c://documents and settings/teg/123.pdf -> 123.pdf
	//  123.pdf -> 123.pdf
	private static String getFileNameFromPath(String filePath){
		
		StringTokenizer st = new StringTokenizer(filePath,"\\");
	    String lastToken=null;
	    while (st.hasMoreTokens()){
	    	lastToken=(String) st.nextElement();
	    	    	
	    }
		return lastToken;
	}

	
}
