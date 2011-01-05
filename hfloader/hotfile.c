#include "hotfile.h"
#include "configfile.h"
#include "utils.h"
#include <curl/curl.h>
#include <stdlib.h>
#include <string.h>

typedef struct
{
	CURL *curl_handle;
	FILE *fp_outfile;
	char *out_folder;
} hf_session;

#pragma mark -
#pragma mark curl callbacks

//receive data and write it to fp_outfile (if fp_outfile != NULL)
size_t receive_data_callback (void *buffer, size_t size, size_t nmemb, void *user_data)
{
	if (!user_data)
		return size * nmemb;
	
	hf_session *session = (hf_session *)user_data;
	if (session->fp_outfile)
	{
		size_t bw = fwrite(buffer, size , nmemb, session->fp_outfile);
		return bw;
	}
	
	return size * nmemb;
}


//parse header responses for Content-Disposition: attachment; and open fp_outfile with that filename
size_t header_callback (void *buffer, size_t size, size_t nmemb, void *user_data)
{
	if (!user_data)
		return size * nmemb;
	
	hf_session *session = (hf_session *)user_data;
	char *pos = strcasestr(buffer, "Content-Disposition: attachment;");
	if (pos)
	{
		pos = strcasestr(buffer, "filename=");
		if (pos)
		{
			pos += strlen("filename=");
			int i = 0;
			
			while(1)
			{
				if (*(pos+i) == '\n' || *(pos+i) == '\r')
					break;
				i++;
			}
			
			char the_filename[i+1];
			memcpy(the_filename, (char *)pos, i);
			the_filename[i] = '\0';
			
			char *filename = trimquotes(the_filename);
			char *end = session->out_folder;
			while (1) 
			{
				if ( *end == '\0')
					break;
				end ++;
			}
			
			char absolute_path[strlen(session->out_folder) + strlen(filename) + 2];
			
			if (*end == '/')
				sprintf(absolute_path, "%s%s", session->out_folder, filename);
			else 
				sprintf(absolute_path, "%s/%s", session->out_folder, filename);
			
			session->fp_outfile = fopen(absolute_path, "wb");
			if (!session->fp_outfile)
			{
				fprintf(stderr, "couldn't oppen file %s for writing.\n", absolute_path);
				return 0;
			}	
		}
	}
		
	return size * nmemb;
}

#pragma mark -
#pragma mark curl funcs
static void hf_init_session (hf_session *session)
{
	*session = (hf_session)
	{
		.fp_outfile = NULL,
		.out_folder = NULL
	};
	
	session->curl_handle = curl_easy_init();
	curl_easy_setopt(session->curl_handle, CURLOPT_VERBOSE, 0);
}

static void hf_cleanup_session (hf_session *session)
{
	curl_easy_cleanup(session->curl_handle);
	*session = (hf_session)
	{
		.fp_outfile = NULL,
		.out_folder = NULL,
		.curl_handle = NULL
	};
	
}


#pragma mark -
#pragma mark hf interaction
static hf_error hf_login (hf_session *session)
{
	CURL *curl_handle = session->curl_handle;
	
	cf_credentials creds;
	cf_err errval = cf_err_noerr;

	errval = cf_get_credentials(&creds);
	if (errval != cf_err_noerr)
		return hf_err_credentials_error;

	char post_data[strlen("returnto=%%2F&user=&pass=") + strlen(creds.username) + strlen(creds.password) + 1];
	sprintf(post_data, "returnto=%%2F&user=%s&pass=%s",creds.username, creds.password);

	curl_easy_setopt(curl_handle, CURLOPT_HTTPPOST,1);
	curl_easy_setopt(curl_handle, CURLOPT_POSTFIELDS, post_data);
	curl_easy_setopt(curl_handle, CURLOPT_URL, "http://hotfile.com/login.php");
	curl_easy_setopt(curl_handle, CURLOPT_COOKIEFILE, "/tmp/hfcookie");
	curl_easy_setopt(curl_handle, CURLOPT_FOLLOWLOCATION, 1);

	curl_easy_setopt(curl_handle, CURLOPT_WRITEFUNCTION,receive_data_callback);
	curl_easy_setopt(curl_handle, CURLOPT_WRITEDATA, NULL);

	CURLcode res = curl_easy_perform(curl_handle);

	if (res != CURLE_OK)
		return hf_err_login_error;

	return hf_err_noerr;
}

hf_error hf_do_download (hf_session *session, char *url, char *folder)
{
	CURL *curl_handle = session->curl_handle;

	curl_easy_setopt(curl_handle, CURLOPT_HTTPPOST,0);
	curl_easy_setopt(curl_handle, CURLOPT_HTTPGET,1);
	curl_easy_setopt(curl_handle, CURLOPT_URL, url);
	curl_easy_setopt(curl_handle, CURLOPT_COOKIEFILE, "/tmp/hfcookie");
	curl_easy_setopt(curl_handle, CURLOPT_FOLLOWLOCATION, 1);

	curl_easy_setopt(curl_handle, CURLOPT_HEADERFUNCTION, header_callback);
	curl_easy_setopt(curl_handle, CURLOPT_HEADERDATA, session);

	//file writing
	curl_easy_setopt(curl_handle, CURLOPT_WRITEFUNCTION, receive_data_callback);
	curl_easy_setopt(curl_handle, CURLOPT_WRITEDATA, session);

	CURLcode res = curl_easy_perform(curl_handle);

	if (res != CURLE_OK)
		return hf_err_download_failed;
	
	if (session->fp_outfile)
		fclose(session->fp_outfile);
	
	return hf_err_noerr;
}

hf_error hf_download (char *url, char *folder)
{
	hf_session session;
	hf_init_session(&session);
	session.out_folder = folder;

	hf_error errval;

	errval = hf_login(&session);
	if (errval != hf_err_noerr)
	{
		hf_cleanup_session (&session);
		return errval;
	}

	errval = hf_do_download(&session, url, folder);
	if (errval != hf_err_noerr)
	{
		hf_cleanup_session (&session);
		return errval;
	}
	
	hf_cleanup_session (&session);
	return hf_err_noerr;
}
