![MBARI logo](etc/assets/images/logo-mbari-3b.png)

# VARS Quickstart @ MBARI

Docker-based quickstart/orchestrator for MBARI's VARS (Video Annotation and Reference System) microservices stack.

## For Users

### What is VARS?

VARS (Video Annotation and Reference System) is a comprehensive suite of microservices for managing and annotating underwater video data. This quickstart repository provides a streamlined way to deploy the full VARS stack using Docker Compose on the following MBARI servers

1. docker-rc.rc.mbari.org - R/V David Packard
2. docker-dp.dp.mbari.org - R/V Rachel Carson
3. gehenna.shore.mbari.org - Internal testing server against a testing database
4. localhost - Local testing server against a testing database

### Prerequisites

- Docker Engine 20.10+ with Docker Compose V2
- Bash shell
- Approximately 8GB of available RAM
- Network access to pull Docker images from Docker Hub
- SSL certificates (can be generated with the built-in `mkcert` command)

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/mbari-org/vars-quickstart-mbari.git
   cd vars-quickstart-mbari
   ```

2. **View available environment targets**
   ```bash
   ./varsq targets
   ```

3. **Configure your environment**
   ```bash
   ./varsq configure etc/env/localhost.env
   ```

4. **Generate SSL certificates for local development**
   ```bash
   ./varsq mkcert
   ```

5. **Start the services**
   ```bash
   ./varsq start
   ```

6. **Check service status**
   ```bash
   ./varsq status
   ```

### Common Commands

#### Environment Management

```bash
# List available environment configurations
./varsq targets

# Configure environment (merges source + core configuration)
./varsq configure etc/env/localhost.env

# View merged environment file (with variable references)
./varsq env

# View resolved environment (all variables evaluated to final values)
./varsq resolved
```

#### Service Operations

```bash
# Start all services
./varsq start

# Stop all services
./varsq stop

# Check service status
./varsq status

# View service logs
./varsq docker logs <service-name>
./varsq docker logs annosaurus

# Follow logs for a service
./varsq docker logs -f annosaurus

# Build containers (if needed)
./varsq build

# Update to latest service images
./varsq update
```

#### SSL Certificate Management

```bash
# Generate self-signed SSL certificate for local testing
# Uses mkcert if available, falls back to openssl
./varsq mkcert
```

#### Advanced Docker Compose Operations

The `varsq docker` command passes through any Docker Compose command:

```bash
# Execute a command in a running container
./varsq docker exec -it annosaurus bash

# Restart a specific service
./varsq docker restart annosaurus

# View container processes
./varsq docker ps

# Remove stopped containers
./varsq docker rm <service-name>
```

### Services Included

The VARS stack includes the following microservices:

- **annosaurus**: Annotation service for managing video annotations
- **vampire-squid**: Video asset management service
- **oni**: Knowledge base service for taxonomic and descriptive data
- **panoptes**: Image and framegrab management service
- **raziel**: API gateway and service aggregator
- **charybdis**: Query service for complex data retrieval
- **beholder**: Image cache service
- **skimmer**: Image processing service
- **nginx**: Reverse proxy with SSL termination

All services are accessible through the configured `VARS_WEB_SERVER` hostname via HTTPS (default port 443) or HTTP (default port 80).

### Troubleshooting

**Services won't start**
- Ensure SSL certificates exist: `./varsq mkcert`
- Check that required environment variables are set: `./varsq resolved`
- Verify Docker is running: `docker ps`
- Check logs: `./varsq docker logs <service-name>`

**Port conflicts**
- Verify no other services are using ports 80, 443, or the configured service ports
- Check current port bindings: `./varsq resolved | grep PORT`

**Configuration issues**
- Ensure you've run `./varsq configure <target>` before starting services
- Verify the environment file exists: `ls docker/env.sh`
- Check for typos in environment variable names
- Not all services run as `root`, so allow docker to read all subdirectories in `./temp`: `sudo chgrp -R docker ./temp`

---

## For Developers

### Architecture Overview

The VARS quickstart uses a two-tier configuration system that merges environment-specific settings with core configuration values. The `varsq` Bash script serves as the primary orchestrator, managing environment setup and proxying Docker Compose operations.

#### Service Communication

Services communicate internally via Docker hostnames (e.g., `http://annosaurus:8080`) and externally via the configured `VARS_WEB_SERVER` hostname. The Raziel service coordinates internal service URLs and acts as an API gateway.

### Project Structure

```
vars-quickstart-mbari/
├── varsq                    # Main orchestrator script
├── docker/
│   ├── compose.yml         # Docker Compose service definitions
│   ├── env.sh              # Generated merged environment file
│   └── nginx/              # Nginx reverse proxy configuration
├── etc/
│   └── env/
│       ├── core.env.sh     # Core environment variables (overrides)
│       ├── docker-dp.env   # Docker R/V David Packard configuration for docker-dp.dp.mbari.org
│       ├── docker-rc.env   # Docker R/V Rachel Carson configuration for docker-rc.rc.mbari.org
│       ├── localhost.env   # Docker testing configuration for localhost
│       ├── gehenna.env     # Docker testing configuration for gehenna.shore.mbari.org
│       └── *.env           # Additional environment targets
└── temp/                   # Temporary files, SSL certificates
```

### Environment Configuration System

The configuration system uses a two-tier approach:

1. **Source files** (`etc/env/*.env`): Environment-specific base configuration
2. **Core file** (`etc/env/core.env.sh`): Canonical variables that override source files

#### Critical Configuration Precedence

When `./varsq configure <source>` runs, it:
1. Writes the source file FIRST to `docker/env.sh`
2. Appends `etc/env/core.env.sh` to the same file

In Bash, later `export` statements override earlier ones, so **variables in `core.env.sh` take precedence**. This ordering must not be changed.

See varsq:79-84 for the implementation.

#### Required Environment Variables

Source files must define these prerequisite variables:

- `VARS_LOG_LEVEL` - Logging level (TRACE, DEBUG, INFO, WARN, ERROR)
- `VARS_WEB_SERVER` - Web server hostname
- `VARS_DATABASE_SERVER` - Database server hostname
- `SQLSERVER_PWD` / `SQLSERVER_USER` - M3 database credentials
- `VARS_KB_PWD` / `VARS_KB_USER` - VARS KB database credentials
- `BEHOLDER_CACHE_DIR`, `FRAMEGRABS_DIR`, `M3_TEMP_DIR` - File system paths
- `SSL_CERT_FILE`, `SSL_KEY_FILE` - SSL certificate paths

### Adding New Services

To add a new service to the VARS stack:

1. **Add environment variables** to `etc/env/core.env.sh`
   ```bash
   export NEWSERVICE_PUBLIC_PORT="9000"
   export NEWSERVICE_DOCKER_URL="http://newservice:8080"
   export NEWSERVICE_PUBLIC_URL="https://${VARS_WEB_SERVER}/newservice"
   ```

2. **Update `docker/compose.yml`** with the service definition
   ```yaml
   newservice:
     container_name: newservice
     image: mbari/newservice
     restart: always
     ports:
       - "${NEWSERVICE_PUBLIC_PORT}:8080"
     environment:
       - LOGBACK_LEVEL=${LOGBACK_LEVEL}
     networks:
       - m3
   ```

3. **Update the `_update()` function** in varsq:211-223 to include image pulls
   ```bash
   docker pull mbari/newservice
   ```

4. **Update Nginx configuration** (if the service needs external access)
   Add proxy configuration in `docker/nginx/conf/` directory

### Service URL Patterns

Follow the existing pattern of dual URL variables:
- `*_DOCKER_URL`: Internal Docker network URL (e.g., `http://service:8080`)
- `*_PUBLIC_URL`: External URL via web server (e.g., `https://vars.example.com/service`)

### Development Workflow

1. **Create a new environment target**
   ```bash
   cp etc/env/docker-dp.env etc/env/myenv.env
   # Edit myenv.env with your settings
   ```

2. **Configure your environment**
   ```bash
   ./varsq configure etc/env/myenv.env
   ```

3. **Verify resolved configuration**
   ```bash
   ./varsq resolved | grep MYSERVICE_
   ```

4. **Start services**
   ```bash
   ./varsq start
   ```

5. **Test your changes**
   ```bash
   ./varsq docker logs myservice
   ./varsq docker exec -it myservice bash
   ```

### Working with the varsq Script

The `varsq` script is the main entry point (varsq:1-277). Key implementation details:

- **Working directory**: All Docker Compose operations execute in the `docker/` directory
- **Environment sourcing**: The merged `docker/env.sh` is sourced at startup (varsq:45-49)
- **Pass-through commands**: The `docker` subcommand forwards all arguments to Docker Compose (varsq:94-105)

### Environment File Conventions

- Use bash-style `export VAR=value` syntax
- Follow quoting conventions in `etc/env/core.env.sh`
- Never commit real secrets (use placeholders only)
- Use descriptive variable names with service prefixes (e.g., `ANNOSAURUS_*`, `PANOPTES_*`)

### Nginx Configuration

Nginx serves as the SSL-terminating reverse proxy. Configuration files are in `docker/nginx/conf/`.

Key volume mounts:
- `${FRAMEGRABS_DIR}` → `/usr/local/nginx/html/framegrabs`
- `${MEDIA_DIR}/media` → `/usr/local/nginx/html/media`
- `${SSL_CERT_FILE}` and `${SSL_KEY_FILE}` for SSL termination

### Testing Changes

After modifying configuration:

1. **Reconfigure**
   ```bash
   ./varsq configure etc/env/your-env.env
   ```

2. **Rebuild if needed**
   ```bash
   ./varsq build
   ```

3. **Restart services**
   ```bash
   ./varsq stop
   ./varsq start
   ```

4. **Verify**
   ```bash
   ./varsq status
   ./varsq docker logs <service-name>
   ```

### Contributing

When contributing to this repository:

1. Test your changes with multiple environment targets
2. Ensure the configuration precedence system remains intact
3. Document any new environment variables in this README
4. Follow the existing patterns for service URL variables
5. Update the `_update()` function for any new Docker images
6. Keep environment files free of real secrets

### License

Copyright MBARI (Monterey Bay Aquarium Research Institute)

### Support

For issues and questions:
- File an issue on the GitHub repository
- Contact the MBARI VARS development team
