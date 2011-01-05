#!/bin/bash
# -----------------------------------------------------------------------------
# This script will send an email notification after each Auto Unrar-Job.
# 
# Sending mail notification is only an example, demonstrating which work can
# be done with shell scripts with reference to Auto Unrar. You can use this 
# script as it is or modify it in any form. If you want to use this script,
# please modify the variables (beginning at line 29) to fit your email-account.
# 
# This script requires the package sendmail.
# 
# -----------------------------------------------------------------------------
# 
# Auto Unrar will execute beforejobend.sh at the end of each unrar job
# The following parameters will be passed to the script by auto unrar:
# 
# $1 : number of successful extracted archives
# $2 : number of archives failed to extract
# $3 : list of successful extracted archives (separated by newline)
# $4 : list of archives failed to extract (separated by newline)
# 
# -----------------------------------------------------------------------------

function fappend {
    echo "$2">>$1;
}

# CHANGE THESE VARIABLES TO FIT YOUR EMAIL-ACCOUNT
TOEMAIL="receiver@email.com";
FREMAIL="sender@email.com";
SUBJECT="Auto Unrar Status-Report";
MSGBODY="<html><header/><body><h3>Auto Unrar has finished an extraction job with the following result:</h3>
<p>
<table><tr><td>Number of successful extracted archives</td><td>"$1"</td></tr>
<tr><td>Number of archives failed to extract</td><td>"$2"</td></tr>
<tr><td>List of successful extracted archives</td><td>"$3"</td></tr>
<tr><td>List of archives failed to extract</td><td>"$4"</td></tr>
</body></html>"
# END OF MODIFICATION-AREA

echo "Sending Status Report to $TOEMAIL";
TMP="/tmp/tmpfil_123"$RANDOM;

rm -rf $TMP;
fappend $TMP "Content-Type: text/html";
fappend $TMP "From: $FREMAIL";
fappend $TMP "To: $TOEMAIL";
fappend $TMP "Reply-To: $FREMAIL";
fappend $TMP "Subject: $SUBJECT";
fappend $TMP "";
fappend $TMP "$MSGBODY";
fappend $TMP "";
fappend $TMP "";
cat $TMP|sendmail -t;
rm $TMP;

