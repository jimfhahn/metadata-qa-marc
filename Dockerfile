FROM ubuntu:18.04

LABEL maintainer="Péter Király <pkiraly@gwdg.de>, Ákos Takács <rimelek@rimelek.hu>"

LABEL description="QA catalogue - a metadata quality assessment tool for MARC based library catalogues."

ARG DEBIAN_FRONTEND=noninteractive

# install R
ENV TZ=Etc/UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone

RUN apt-get update \
    # Install add-apt-repository command
 && apt-get install -y --no-install-recommends software-properties-common \
    # add PPA with pre-compiled cran packages
 && add-apt-repository -y ppa:openjdk-r/ppa \
 && add-apt-repository -y ppa:marutter/rrutter3.5 \
 && add-apt-repository -y ppa:marutter/c2d4u3.5 \
 && apt-get install -y --no-install-recommends \
      # install basic OS tools
      apt-utils \
      nano \
      jq \
      curl \
      openssl \
      # install Java
      openjdk-8-jre-headless \
      # Install R
      r-base \
      # Install R packages from ppa:marutter
      r-cran-tidyverse \
      r-cran-stringr \
      r-cran-gridextra \
 && rm -rf /var/lib/apt/lists/*

# install metadata-qa-marc
RUN mkdir -p /opt/metadata-qa-marc/scripts \
 && mkdir -p /opt/metadata-qa-marc/target

COPY target/metadata-qa-marc-0.4-SNAPSHOT-jar-with-dependencies.jar /opt/metadata-qa-marc/target/
COPY scripts/*.* /opt/metadata-qa-marc/scripts/
COPY setdir.sh.template /opt/metadata-qa-marc/setdir.sh

# copy common scripts
COPY common-variables \
     common-script \
     metadata-qa.sh \
     LICENSE \
     README.md /opt/metadata-qa-marc/

# copy analysis scripts
COPY validator \
     prepare-solr \
     index \
     solr-functions \
     completeness \
     classifications \
     authorities \
     tt-completeness \
     shelf-ready-completeness \
     serial-score \
     formatter \
     functional-analysis \
     network-analysis /opt/metadata-qa-marc/

RUN mkdir -p /opt/metadata-qa-marc/marc \
 && sed -i.bak 's,BASE_INPUT_DIR=your/path,BASE_INPUT_DIR=/opt/metadata-qa-marc/marc,' /opt/metadata-qa-marc/setdir.sh \
 && sed -i.bak 's,BASE_OUTPUT_DIR=your/path,BASE_OUTPUT_DIR=/opt/metadata-qa-marc/marc/_output,' /opt/metadata-qa-marc/setdir.sh

ARG SMARTY_VERSION=3.1.33

# install web application
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      apache2 \
      php \
      unzip \
 && rm -rf /var/lib/apt/lists/* \
 && cd /var/www/html/ \
 && curl -s -L https://github.com/pkiraly/metadata-qa-marc-web/archive/master.zip --output master.zip \
 && unzip -q master.zip \
 && rm master.zip \
 && mv metadata-qa-marc-web-master metadata-qa \
 && echo dir=/opt/metadata-qa-marc/marc/_output > /var/www/html/metadata-qa/configuration.cnf \
 && cp /var/www/html/metadata-qa/configuration.js.template /var/www/html/metadata-qa/configuration.js \
 && touch /var/www/html/metadata-qa/selected-facets.js \
 && mkdir /var/www/html/metadata-qa/cache \
 && mkdir /var/www/html/metadata-qa/libs \
 && mkdir /var/www/html/metadata-qa/images \
 && cd /var/www/html/metadata-qa/libs/ \
 && curl -s -L https://github.com/smarty-php/smarty/archive/v${SMARTY_VERSION}.zip --output v$SMARTY_VERSION.zip \
 && unzip -q v${SMARTY_VERSION}.zip \
 && rm v${SMARTY_VERSION}.zip \
 && mkdir -p /var/www/html/metadata-qa/libs/_smarty/templates_c \
 && chmod a+w -R /var/www/html/metadata-qa/libs/_smarty/templates_c/ \
 && sed -i.bak 's,</VirtualHost>,        <Directory /var/www/html/metadata-qa>\n                Options Indexes FollowSymLinks MultiViews\n                AllowOverride All\n                Order allow\,deny\n                allow from all\n                DirectoryIndex index.php index.html\n        </Directory>\n</VirtualHost>,' /etc/apache2/sites-available/000-default.conf \
 && echo "\nWEB_DIR=/var/www/html/metadata-qa/\n" >> /opt/metadata-qa-marc/common-variables

ARG SOLR_VERSION=8.4.1

# install Solr
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      lsof \
 && apt-get --assume-yes autoremove \
 && rm -rf /var/lib/apt/lists/* \
 && cd /opt \
 && curl -s -L http://archive.apache.org/dist/lucene/solr/${SOLR_VERSION}/solr-${SOLR_VERSION}.zip --output solr-${SOLR_VERSION}.zip \
 && unzip -q solr-${SOLR_VERSION}.zip \
 && rm solr-${SOLR_VERSION}.zip \
 && ln -s solr-${SOLR_VERSION} solr

# init process configuration
RUN apt-get update \
 && apt-get install -y --no-install-recommends supervisor \
 && mkdir -p /var/log/supervisor \
 && rm -rf /var/lib/apt/lists/*

COPY docker/supervisord.conf /etc/

WORKDIR /opt/metadata-qa-marc

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]

