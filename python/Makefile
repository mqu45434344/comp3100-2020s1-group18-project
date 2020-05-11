
ds-client.pyz: $(shell find ds_client -type f)
	mkdir ds-client
	cp -R ds_client ds-client
	echo 'import runpy; runpy.run_module("ds_client", run_name="__main__")' > ds-client/__main__.py
	python3 -m zipapp -p '/usr/bin/env python3' ds-client
	rm -rf ds-client

.PHONY: clean
clean:
	rm -f ds-client.pyz
