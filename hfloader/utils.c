#include "utils.h"
#include <string.h>

char *trimquotes(char *str)
{
	while(*str == '"') 
		str++;
	
	if(*str == 0) 
		return str;
	
	char *end = str + strlen(str) - 1;
	while(end > str && (*end == '"')) 
		end--;
	*(end+1) = 0;

	return str;
}
