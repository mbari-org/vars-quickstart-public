# Brian Schlining
# 2025-12-15

# -- START CORE ENVIRONMENT VARIABLES FOR VARS MICROSERVICES ----------
# The following enviroment variables are prerequisites to use this env file:
#   BEHOLDER_CACHE_DIR   - Directory that Beholder writes its cache to.
#   FRAMEGRABS_DIR       - Directory that Panoptes writes framegrabs to.
#   M3_TEMP_DIR          - Directory that VARS microservices can use for temporary files.
#   MEDIA_DIR            - Directory for serving video files
#   NGINX_ROOT_DIR       - Directory containing html content for NGINX to serve
#   PROMETHEUS_DIR       - Directory for Prometheus to write to.
#   SKIMMER_CACHE_DIR    - Directory that Skimmer writes its cache to.
#   SQLSERVER_PWD        - Password for the account for the M3_ANNOTATIONS and M3_VIDEO_ASSETS databases.
#   SQLSERVER_USER       - User for the account for the M3_ANNOTATIONS and M3_VIDEO_ASSETS databases.
#   SSL_CERT_FILE        - Path to the SSL certificate file used by Nginx.
#   SSL_KEY_FILE         - Path to the SSL key file used by Nginx.
#   VARS_DATABASE_SERVER - The database server hosting the M3 and VARS databases.
#   VARS_KB_PWD          - Password for the account for the VARS_KB database.
#   VARS_KB_USER         - User for the account for the VARS_KB database. 
#   VARS_LOG_LEVEL       - Adjust logging level for all services: TRACE, DEBUG, INFO, WARN, ERROR
#   VARS_WEB_SERVER      - The name of the web server hosting the VARS/M3 microservices

# -- Shared Environment Variables --
export LOGBACK_LEVEL=${VARS_LOG_LEVEL}
export M3_JDBC_DRIVER=org.postgresql.Driver
export M3_DOCKER_JDBC_URL="jdbc:postgresql://postgres:5432/M3_VARS?sslmode=disable&stringType=unspecified"
export M3_PUBLIC_JDBC_URL="jdbc:postgresql://${VARS_DATABASE_SERVER}:5432/M3_VARS?sslmode=disable&stringType=unspecified"
export M3_SERVER_URL="https://${VARS_WEB_SERVER}"

# -- Annosaurus Environment Variables --
export ANNOSAURUS_DATABASE_PASSWORD="${SQLSERVER_PWD}"
export ANNOSAURUS_DATABASE_URL_FOR_APPS="${M3_PUBLIC_JDBC_URL}"
export ANNOSAURUS_DATABASE_URL="${M3_DOCKER_JDBC_URL}"
export ANNOSAURUS_DATABASE_USER="${SQLSERVER_USER}"
export ANNOSAURUS_DOCKER_URL="http://annosaurus:8080/v1"
export ANNOSAURUS_MESSAGING_ZEROMQ_ENABLE=true
export ANNOSAURUS_MESSAGING_ZEROMQ_PORT=5563
export ANNOSAURUS_MESSAGING_ZEROMQ_TOPIC=vars
export ANNOSAURUS_PUBLIC_PORT=8082
export ANNOSAURUS_PUBLIC_URL="${M3_SERVER_URL}/anno/v1"

# -- Beholder Environment Variables --
export BEHOLDER_API_KEY=foo
export BEHOLDER_CACHE_FREEPCT=0.20
export BEHOLDER_CACHE_SIZE=1000
export BEHOLDER_DOCKER_URL="http://beholder:8080"
export BEHOLDER_PUBLIC_PORT=8088
export BEHOLDER_PUBLIC_URL="${M3_SERVER_URL}/capture"

# -- Charybdis Environment Variables --
export CHARYBDIS_ANNOTATION_SERVICE_PAGESIZE=1000
export CHARYBDIS_ANNOTATION_SERVICE_TIMEOUT="PT120S"
export CHARYBDIS_DOCKER_URL="http://charybdis:8080"
export CHARYBDIS_MEDIA_SERVICE_TIMEOUT="PT10S"
# export CHARYBDIS_RAZIEL_SERVICE_URL="http://localhost:8086/config"
export CHARYBDIS_PUBLIC_PORT=8086
export CHARYBDIS_PUBLIC_URL="${M3_SERVER_URL}/references/v1"

# -- Oni Environment Variables --
export ONI_DATABASE_PASSWORD=${VARS_KB_PWD}
export ONI_DATABASE_URL="${M3_JDBC_BASE_URL};databaseName=VARS_KB;trustServerCertificate=true"
export ONI_DATABASE_USER="${VARS_KB_USER}"
export ONI_DOCKER_URL="http://oni:8080/v1"
export ONI_PUBLIC_PORT=8083
export ONI_PUBLIC_URL="${M3_SERVER_URL}/kb/v1"
export ONI_URL="${M3_SERVER_URL}/kb/v1"

# -- Panoptes Environment Variables --
export PANOPTES_HTTP_CONTEXT_PATH=/panoptes
export PANOPTES_DOCKER_URL="http://panoptes:8080${PANOPTES_HTTP_CONTEXT_PATH}/v1"
export PANOPTES_FILE_ARCHIVER="org.mbari.m3.panoptes.services.OldStyleMbariDiskArchiver"
export PANOPTES_PUBLIC_PORT=8085
export PANOPTES_PUBLIC_URL="${M3_SERVER_URL}${PANOPTES_HTTP_CONTEXT_PATH}/v1"
export PANOPTES_ROOT_DIRECTORY=/framegrabs
export PANOPTES_ROOT_URL="${M3_SERVER_URL}/framegrabs"

# -- Raziel Environment Variables --
export RAZIEL_ANNOSAURUS_TIMEOUT="60 seconds"
export RAZIEL_BEHOLDER_TIMEOUT="10 seconds"
export RAZIEL_HTTP_CONTEXT=config
export RAZIEL_JWT_ISSUER="${VARS_JWT_ISSUER}"
export RAZIEL_PUBLIC_PORT=8400
export RAZIEL_PUBLIC_URL="${M3_SERVER_URL}/config"

# -- Skimmer Environment Variables --
export SKIMMER_DOCKER_URL="http://skimmer:8080"
export SKIMMER_IMAGE_CACHE_SIZE_MB=100
export SKIMMER_PUBLIC_PORT=8089
export SKIMMER_PUBLIC_URL="${M3_SERVER_URL}/skimmer"
export SKIMMER_ROI_CACHE_SIZE_MB=500

# -- VampireSquid Environment Variables --
export VAMPIRESQUID_DATABASE_PASSWORD="${SQLSERVER_PWD}"
export VAMPIRESQUID_DATABASE_URL="${M3_JDBC_BASE_URL};databaseName=M3_VIDEO_ASSETS;trustServerCertificate=true"
export VAMPIRESQUID_DATABASE_USER="${SQLSERVER_USER}"
export VAMPIRESQUID_DOCKER_URL="http://vampire-squid:8080/v1"
export VAMPIRESQUID_PUBLIC_PORT=8084
export VAMPIRESQUID_PUBLIC_URL="${M3_SERVER_URL}/vam/v1"
