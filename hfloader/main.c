#include <stdio.h>
#include "hotfile.h"

void usage (char *appname)
{
	printf("usage: %s <url> <folder to write file to>\n", appname);
			return;
}

int main (int argc, char **argv)
{
	if (argc != 3)
	{
		usage (argv[0]);
		return 2;
	}
	
	hf_error ret = hf_download(argv[1], argv[2]);
	if (ret == hf_err_noerr)
	{
		printf ("%s: dl of %s to %s succeeded\n", argv[0], argv[1], argv[2]);
		return 0;
	}

	fprintf (stderr, "%s: failed dl of %s to %s\n", argv[0], argv[1], argv[2]);
	return ret;
}
