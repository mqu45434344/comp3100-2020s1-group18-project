#!/usr/bin/env bash

shopt -s failglob

###
your_client_cmd='java -jar ds-client.jar -a ff'
your_client_cmd='python3 -m ds_client -a ff'
ref_client_cmd='./ds-client -a ff'
config_files=(./config_simple{1,2,3,4,5,6}.xml)
server_newlines=0
server_prog='./ds-server'
###

[[ -f $server_prog ]] || {
	>&2 echo "Server program not found: $server_prog"
	exit 1
}

n_opt=; (( server_newlines )) && n_opt=-n

echo "$ref_client_cmd"
echo "$your_client_cmd"
echo -----

trap 'kill 0' EXIT

for conf in "${config_files[@]}"; do
	echo "$conf"
	ref_log=$conf.ref.log
	your_log=$conf.your.log

	$server_prog $n_opt -c "$conf" -v brief > "$ref_log" &
	sleep .1
	$ref_client_cmd >/dev/null
	wait $!

	$server_prog $n_opt -c "$conf" -v brief > "$your_log" &
	sleep .1
	$your_client_cmd
	wait $!

	if cmp -s "$ref_log" "$your_log"; then
		printf '\e[0;32m%s\e[0m\n' 'PASSED!'
	else
		printf '\e[0;31m%s\e[0m\n' 'NOT PASSED!'
	fi
done
