FROM java:openjdk-7-jre

MAINTAINER EGerrit <egerrit-dev@eclipse.org>

ENV GERRIT_USER gerrit2
ENV GERRIT_HOME /home/$GERRIT_USER
ENV GERRIT_WAR $GERRIT_HOME/gerrit.war
ARG GERRIT_VERSION=2.11.5

RUN useradd -m $GERRIT_USER

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
		git-core \
		openssh-client \
    && rm -rf /var/lib/apt/lists/*

#Download gerrit and put it in place
ADD https://gerrit-releases.storage.googleapis.com/gerrit-$GERRIT_VERSION.war $GERRIT_WAR

#Copy files necessary to do the setup
COPY * $GERRIT_HOME/
RUN chmod +x $GERRIT_HOME/setupUsers.sh

#Move ownership of the copied files to the gerrit user
RUN chown -R $GERRIT_USER:$GERRIT_USER $GERRIT_HOME

USER $GERRIT_USER
WORKDIR $GERRIT_HOME

#Initialize a gerrit server
RUN java -jar $GERRIT_WAR init --batch --no-auto-start -d $GERRIT_HOME/gerrit 

#Override the generated defaults
ADD gerrit.config $GERRIT_HOME/gerrit/etc/gerrit.config

#Create users
RUN $GERRIT_HOME/setupUsers.sh

EXPOSE 8080 29418

CMD ["/home/gerrit2/gerrit/bin/gerrit.sh", "daemon"]
