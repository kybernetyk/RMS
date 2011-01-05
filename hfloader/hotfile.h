#pragma once

typedef enum
{
	hf_err_noerr,
	hf_err_couldntcreate_tmpfile,
	hf_err_download_failed,
	hf_err_file_not_found,
	hf_err_file_deleted,
	hf_err_login_error,
	hf_err_credentials_error
} hf_error;

extern hf_error hf_download (char *url, char *folder);
