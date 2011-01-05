#include "configfile.h"

#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <pwd.h>
#include <stdio.h>

//TODO: make a real cfgfile parser lol :]
cf_err cf_get_credentials (cf_credentials *out_creds)
{
	uid_t uid;
	uid = getuid();
	
	struct passwd *pwd = getpwuid(uid);
	
	char cfname[strlen(pwd->pw_dir) + strlen(".hfloaderrc") + 2];
	sprintf(cfname, "%s/.hfloader", pwd->pw_dir);
	
	FILE *f = fopen(cfname, "rt");
	if (!f)
	{	
		fprintf(stderr, ".hfloader config file was not found in home dir (%s)\n", pwd->pw_dir);
		return cf_err_configfile_notfound;
	}

	fscanf(f, "username=%s\n", out_creds->username);
	fscanf(f, "password=%s\n", out_creds->password);	
	if (strlen(out_creds->username) == 0 ||
		strlen(out_creds->password) == 0)
	{
		fprintf(stderr, ".hfloader config malformatted. couldn't read credentials!\n");
		return cf_err_credentials_missing;
	}
	
//	printf("credentials = %s:%s\n", out_creds->username, out_creds->password);
	return cf_err_noerr;
}
	
