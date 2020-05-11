
*mq comp3100 2020s1*

# Job Scheduling Simulator

**Group 18**

* 44860668 - Henrietta Guo
* 45434344 - Daniel Prosser
* 45701717 - Ma. Concepcion Velasco

## Running the demo

### Manual steps

1. Extract the `ds-sim.tar` file (e.g., using `tar xvf ds-sim.tar`).

2. Run `make` in the `java/` directory.

3. Copy `java/bin/ds-client.jar` to `ds-sim/`

4. Change the working directory to `ds-sim/` and run `make`

5. Run the server: `./ds-server -c config_simple2.xml &`

6. Run the client: `java -jar ds-client.jar`

### Command line steps

```shell
make
cd ds-sim/

# Run server (in background)
./ds-server -c config_simple2.xml &

# Run (java) client
java -jar ds-client.jar

# Run (python) client
python3 ds-client.pyz

# Run (python) client (from source)
python3 -m ds_client
```
