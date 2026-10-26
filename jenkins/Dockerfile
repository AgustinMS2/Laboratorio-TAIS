# Jenkins (LTS) con el plugin de Pipeline ya instalado y el job precargado
FROM jenkins/jenkins:lts-jdk21

# Sin asistente de instalacion: el laboratorio entra directo a http://localhost:8080
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl jq tar gzip ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://download.docker.com/linux/static/stable/x86_64/docker-27.3.1.tgz | tar -xz -C /usr/local/bin --strip-components=1 docker/docker && \
    mkdir -p /usr/local/lib/docker/cli-plugins && \
    curl -fsSL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose && \
    ln -s /usr/local/lib/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

RUN jenkins-plugin-cli --plugins workflow-aggregator

# Job de pipeline "copiar-nueva-version" (se carga al iniciar Jenkins)
COPY --chown=jenkins:jenkins jobs/copiar-nueva-version/config.xml /usr/share/jenkins/ref/jobs/copiar-nueva-version/config.xml
