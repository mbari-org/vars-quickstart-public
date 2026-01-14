# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Docker-based quickstart/orchestrator for MBARI's VARS (Video Annotation and Reference System) microservices. The `varsq` Bash script is the primary orchestrator that manages environment configuration and Docker Compose operations.

## Core Architecture

### The varsq CLI

The `varsq` script (varsq:1-200) is the main entry point for all operations. It manages environment file merging and proxies Docker Compose commands.

**Critical environment file precedence**: When `./varsq configure <source>` runs, it writes the source file FIRST, then appends `etc/env/core.env`. In Bash, later `export` statements override earlier ones, so variables in `core.env` take precedence. Do not change this ordering.

### Environment Configuration

The repository uses a two-tier environment variable system:
- **Source files** (e.g., `etc/env/docker-dp.env`): Base configuration specific to deployment targets
- **Core file** (`etc/env/core.env`): Canonical environment variables that override source files

Configuration requires these prerequisite variables to be defined in source files:
- `VARS_LOG_LEVEL` - Logging level (TRACE, DEBUG, INFO, WARN, ERROR)
- `VARS_WEB_SERVER` - Web server hostname
- `VARS_DATABASE_SERVER` - Database server hostname
- `SQLSERVER_PWD` / `SQLSERVER_USER` - M3 database credentials
- `VARS_KB_PWD` / `VARS_KB_USER` - VARS KB database credentials
- `BEHOLDER_CACHE_DIR`, `FRAMEGRABS_DIR`, `M3_TEMP_DIR` - File system paths
- `SSL_CERT_FILE`, `SSL_KEY_FILE` - SSL certificate paths

### Microservices

The system orchestrates these MBARI services (defined in docker/compose.yml):
- **annosaurus**: Annotation service (port via `ANNOSAURUS_PUBLIC_PORT`, ZMQ messaging on port 5563)
- **vampire-squid**: Video asset service
- **oni**: Knowledge base service
- **panoptes**: Image/framegrab service
- **raziel**: API gateway/aggregator (coordinates internal service URLs)
- **charybdis**: Query service
- **beholder**: Image cache service
- **skimmer**: Image processing service
- **nginx**: Reverse proxy (ports 80/443)

Services communicate internally via Docker hostnames (e.g., `http://annosaurus:8080`) and externally via the configured `VARS_WEB_SERVER` hostname.

## Common Commands

### Environment Setup
```bash
# List available environment targets
./varsq targets

# Configure environment (merges source + core.env into docker/env.sh)
./varsq configure etc/env/docker-dp.env

# View merged environment (raw file with variable references)
./varsq env

# View resolved environment (all variables evaluated to their final values)
./varsq resolved
```

### Service Operations
```bash
# Start all services
./varsq start

# Stop all services
./varsq stop

# Check service status
./varsq status

# Build containers (if needed)
./varsq build
```

### Docker Compose Pass-through
```bash
# Any docker compose command can be passed through
./varsq docker ps
./varsq docker logs annosaurus
./varsq docker exec -it annosaurus bash
```

### Update Images
```bash
# Pull latest versions of all service images
./varsq update
```

## Development Patterns

### Adding/Modifying Services

When adding or modifying services:
1. Add environment variables to `etc/env/core.env` with appropriate prefixes (e.g., `NEWSERVICE_*`)
2. Update `docker/compose.yml` with the service definition
3. If adding a new Docker image, update the `_update()` function in varsq:148-160
4. Preserve the pattern of both `*_DOCKER_URL` (internal) and `*_PUBLIC_URL` (external) variables

### Environment Files

Environment files use bash-style `export VAR=value` syntax. Follow existing quoting conventions in `etc/env/core.env`. The files should never contain real secrets—use placeholders only.

### Working Directory

All Docker Compose operations execute in the `docker/` directory. The merged environment file is written to `docker/env.sh` and sourced by the varsq script (varsq:43-47).

### Nginx Configuration

Nginx serves as a reverse proxy with SSL termination. Configuration files are in `docker/nginx/conf/`. Volume mounts include:
- Framegrabs directory: `${FRAMEGRABS_DIR}` → `/usr/local/nginx/html/framegrabs`
- SSL certificates: `${SSL_CERT_FILE}` and `${SSL_KEY_FILE}`
- Media files: `${MEDIA_DIR}/media` → `/usr/local/nginx/html/media`
