FROM scratch AS build
ADD backend_container.orig.tar.bz2 /

FROM registry-proxy.k3s.os2.oxui.de/library/debian:bullseye-slim

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=registry-proxy.k3s.os2.oxui.de/library/eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ENV LANG=C.UTF-8
ENV LANGUAGE=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV PATH=$PATH:/opt/open-xchange/sbin/
ENV YQ_VERSION=4.27.2

ADD https://artifactory.production.cloud.oxoe.io/artifactory/binaries/yq/v${YQ_VERSION}/yq_linux_amd64 /usr/local/bin/yq

# hadolint ignore=DL3022
COPY --from=registry.open-xchange.com/appsuite-core-internal/appsuite-tools:latest /ox_props /usr/local/bin/ox_props
COPY entrypoint.sh /entrypoint.sh

ARG OX_USER=open-xchange
ARG OX_GROUP=${OX_USER}
ARG OX_UID=1000
ARG OX_GID=${OX_UID}
ARG OX_HOME=/opt/open-xchange
ARG OX_SPOOL_DIR=/var/spool/open-xchange

# hadolint ignore=DL3008
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        bzip2 \
        ca-certificates \
        curl \
        file \
        gnupg2 \
        locales \
        mariadb-client \
        netcat \
        net-tools \
        procps \
        tini \
    && chmod +x /usr/local/bin/yq \
    && apt-get autoremove -yqq \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g ${OX_GID} "${OX_GROUP}" \
    && useradd -l -u ${OX_UID} -g ${OX_GID} -d "${OX_HOME}" -m -s /bin/bash "${OX_USER}" \
    && mkdir -p "${OX_HOME}/etc/languages/appsuite" \
    && chown -R "${OX_UID}:${OX_GID}" "${OX_HOME}" 

COPY --chown=${OX_UID}:${OX_GID} /configuration /configuration
COPY --chown=${OX_UID}:${OX_GID} --from=build /etc /etc/
COPY --chown=${OX_UID}:${OX_GID} --from=build /opt /opt/
COPY --chown=${OX_UID}:${OX_GID} --from=build /usr /usr/
COPY --chown=${OX_UID}:${OX_GID} --from=build /var /var/
COPY --chown=${OX_UID}:${OX_GID} --from=registry.gitlab.open-xchange.com/documents/pdftool/pdftoolwrapper:main /pdftool ${OX_HOME}/sbin

VOLUME [ "${OX_HOME}", "${OX_SPOOL_DIR}" ]

USER ${OX_USER}
WORKDIR ${OX_HOME}
ENTRYPOINT [ "/usr/bin/tini", "--", "/entrypoint.sh", "/opt/open-xchange/sbin/open-xchange-cloud" ]

EXPOSE 8009 1099
