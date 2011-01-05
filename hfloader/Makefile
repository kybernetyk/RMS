CC=clang
LIBS=-lcurl
CFLAGS=-c -Wall

EXECNAME=hfloader
RUNURL="http://hotfile.com/dl/94078511/ae2334b/himym0613x264dim.part1.rar.html"
RUNFOLDER=/tmp

all: hfloader

test: clean all run

hfloader: cfiles 
	$(CC) $(LIBS) *.o -o $(EXECNAME)

cfiles:
	$(CC) $(CFLAGS) *.c

clean:
	rm -rf *.o $(EXECNAME)

run:
	./$(EXECNAME) $(RUNURL) $(RUNFOLDER)
