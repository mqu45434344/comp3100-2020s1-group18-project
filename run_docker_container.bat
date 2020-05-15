@echo off
setlocal EnableDelayedExpansion
goto :main

:attach
	docker ps -q -f name="^dim_sim$" | >nul find /v "" && (
		docker attach dim_sim || (call;)
	)
exit /b
:start
	docker start dim_sim
exit /b
:create
	docker images -q ds-sim | >nul find /v "" && (
		docker create -it -v "%cd%:/project" -w /project --name dim_sim ds-sim
	)
exit /b
:build
	docker build -t ds-sim .
exit /b

:main
where /q docker || (
	>&2 echo Aborting: `docker` command not found
	exit /b 1
)

docker info >nul 2>&1 || (
	>&2 echo Start the Docker daemon first^^!
	exit /b 1
)

set "cmds[1]=attach"
set "cmds[2]=start"
set "cmds[3]=create"
set "cmds[4]=build"

for /f %%I in ('set cmds[ ^| find /v /c ""') do set "cmds[#]=%%I"
for /l %%I in (1 1 %cmds[#]%) do (
	set "cmd=!cmds[%%I]!"
	<nul set/p="attempting: "
	echo $ "!cmd!"
	call :!cmd!
	if not errorlevel 1 (
		set /a k_start=%%I - 1
		for /l %%K in (!k_start! -1 1) do (
			set "cmd=!cmds[%%K]!"
			<nul set/p="performing: "
			echo $ "!cmd!"
			call :!cmd! || exit /b 1
		)
		goto :break
	)
)
:break
