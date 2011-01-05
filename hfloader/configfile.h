#pragma once

typedef enum
{
	cf_err_noerr,
	cf_err_configfile_notfound,
	cf_err_credentials_missing
} cf_err;

typedef struct
{
	char username[255];
	char password[255];
} cf_credentials;

extern cf_err cf_get_credentials (cf_credentials *out_creds);
	

