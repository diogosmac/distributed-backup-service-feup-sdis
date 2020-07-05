# compilation
JC = javac
OUT_DIR := build/

all: mkdir
	$(JC) -d $(OUT_DIR) src/*.java src/*/*.java src/*/*/*.java

mkdir:
	@mkdir -p $(OUT_DIR)
 
clean:
	@rm -rf build/

rmi:
	rmiregistry -J-Djava.class.path=$(OUT_DIR) &

#execution
ADDRESS = "localhost"
PORT = 30001
KNOWN_ADDR = "localhost"
KNOWN_PORT = 30001
AP = "ap14661"
FILE = "text.txt"
RD = 1
SIZE = 100

start:
	@sh scripts/start_chord.sh $(ADDRESS) $(PORT)

join:
	@sh scripts/join_chord.sh $(ADDRESS) $(PORT) $(KNOWN_ADDR) $(KNOWN_PORT)

backup:
	@sh scripts/backup.sh $(AP) ../test/$(FILE) $(RD)

restore:
	@sh scripts/restore.sh $(AP) ../test/$(FILE)

delete:
	@sh scripts/delete.sh $(AP) ../test/$(FILE)

reclaim:
	@sh scripts/reclaim.sh $(AP) $(SIZE)

state:
	@sh scripts/state.sh $(AP)
