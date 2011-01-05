#!/bin/bash
touch unrar.lock
sleep 1s
if [ -f download.lock ]
then
echo no
exit 0
fi

java -jar auto-unrar-1.0.jar -l

#mkv2mov

#mv -R extracted /mount/airport

rm unrar.lock
