FROM ubuntu
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update
RUN apt-get install -y gcc libxml2-dev
RUN apt-get install -y default-jdk python3
RUN apt-get install -y make less vim tmux
