FROM jbangdev/jbang-action:0.115.0 as Builder
COPY scripts /scripts
RUN cd scripts && jbang export portable /scripts/CiScripts.java
COPY scripts/command.sh /usr/bin/run-command
