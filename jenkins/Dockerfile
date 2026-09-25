# Jenkins (LTS) con el plugin de Pipeline ya instalado y el job precargado
FROM jenkins/jenkins:lts-jdk21

# Sin asistente de instalacion: el laboratorio entra directo a http://localhost:8080
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

RUN jenkins-plugin-cli --plugins workflow-aggregator

# Job de pipeline "copiar-nueva-version" (se carga al iniciar Jenkins)
COPY --chown=jenkins:jenkins jobs/copiar-nueva-version/config.xml /usr/share/jenkins/ref/jobs/copiar-nueva-version/config.xml
