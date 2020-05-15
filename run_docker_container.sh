#!/usr/bin/env bash

command -v docker &>/dev/null || {
	>&2 echo 'Aborting: `docker` command not found'
	exit 1
}

docker info &>/dev/null || {
	>&2 echo 'Start the Docker daemon first!'
	exit 1
}

attach() {
	[[ $(docker ps -q -f name="^dim_sim$") ]] && {
		docker attach dim_sim || true;
	}
}
start() {
	docker start dim_sim
}
create() {
	[[ $(docker images -q ds-sim) ]] && {
		docker create -it -v "$(pwd):/project" -w /project --name dim_sim ds-sim
	}
}
build() {
	docker build -t ds-sim .
}

cmds=(attach start create build)

for ((i = 0; i < ${#cmds[@]}; i++)); do
	cmd="${cmds[i]}"
	echo -n "attempting: "
	echo "$ $cmd"
	if eval $cmd; then
		for ((k = i - 1; k > -1; k--)); do
			cmd="${cmds[k]}"
			echo -n "performing: "
			echo "$ $cmd"
			eval $cmd || exit 1
		done
		break
	fi
done
