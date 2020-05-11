
.PHONY: all
all:
	tar xf ds-sim.tar
	(cd java/ && make)
	(cd python/ && make)
	(cd ds-sim/ && make)
	ln -s ../java/bin/ds-client.jar ds-sim/
	ln -s ../python/ds-client.pyz ds-sim/
	ln -s ../python/ds_client/ ds-sim/

.PHONY: clean
clean:
	rm -rf ds-sim/
	(cd java/ && make clean)
	(cd python/ && make clean)
