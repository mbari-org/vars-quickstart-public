# VARS Real-time Annotation

## Overview

[VARS](https://docs.mbari.org/vars-annotation/) (Video Annotation and Reference System) is MBARI's software stack for creating and managing video annotations. In real-time mode, VARS captures frames directly from a live video feed via a Blackmagic capture device, allowing scientists to annotate observations as they happen (e.g., during ROV dives). This guide covers the setup for real-time annotation on a Mac.

## Requirements

### Hardware

- [Blackmagic Design capture device](https://www.blackmagicdesign.com/products) - MBARI uses the [UltraStudio 4K Mini](https://www.blackmagicdesign.com/products/ultrastudio) and the [Decklink 8K Pro](https://www.blackmagicdesign.com/products/decklink)
- [Mac computer](https://www.apple.com/mac/) with at least 8 GB of available RAM

### Software

- [Docker Engine 20.10+](https://www.docker.com/products/docker-desktop/) with Docker Compose V2 - Used for running VARS services
- [vars-quickstart-public](https://github.com/mbari-org/vars-quickstart-public) - Project for quickly spinning up VARS database, web server, and microservices
- **libbmagic** - A software bridge that allows applications to request framegrabs from the live video feed via Blackmagic Design capture devices. This is not currently open-source software. Contact MBARI for a built executable and installation instructions; we are happy to share it.
- [VARS Annotation](https://github.com/mbari-org/vars-annotation/releases) - The main real-time annotation application. Download the latest release for macOS.

## Quickstart

### 1. Start VARS Services

Clone and configure [vars-quickstart-public](https://github.com/mbari-org/vars-quickstart-public):

```sh
git clone https://github.com/mbari-org/vars-quickstart-public.git
cd vars-quickstart-public
./varsq configure localhost
./varsq mkcert
./varsq start
```

Verify the services are running:

```sh
./varsq status
```

For detailed instructions (environment variables, SSL certificates, service logs, etc.), see the [vars-quickstart-public README](https://github.com/mbari-org/vars-quickstart-public).

### 2. Start the Framecapture Server (libbmagic)

Start libbmagic for framecapture. The exact parameters vary depending on the capture device and video signal encoding. Here's the script we use to launch it on the boat:

#### start_framecapture.sh

```sh
#!/usr/bin/env bash

cd $HOME/Documents/VARS/libbmagic

# For 4K
./bin/bfgsvr --api-key="foo" --host=localhost:9000 --verbose=5 --del-ms=0 --log-dir=$PWD/logs --format=4k59:v210

# For HD
# ./bin/bfgsvr --api-key="foo" --host=localhost:9000 --verbose=5 --log-dir=$PWD/logs --format=Hp59:v210
```

**Parameters:**

| Parameter     | Description                                                                 |
|---------------|-----------------------------------------------------------------------------|
| `--api-key`   | Shared secret used by clients to authenticate with the framecapture server  |
| `--host`      | Address and port the server listens on                                      |
| `--verbose`   | Log verbosity level                                                         |
| `--del-ms`    | Delay in milliseconds before capture (0 for immediate)                      |
| `--log-dir`   | Directory for log output                                                    |
| `--format`    | Video format and codec (e.g., `4k59:v210` for 4K at 59fps, `Hp59:v210` for 1080p at 59fps) |

#### Test the Framecapture Server

Verify the server is working by requesting a test frame:

#### test_framecapture.sh

```sh
#!/usr/bin/env bash

cd $HOME/Documents/VARS/libbmagic

mkdir -p $PWD/img
./bin/bfgcli --api-key="foo" --verbose=5 --host=localhost:9000 --rto-ms=15000 $PWD/img/foo.png
```

If successful, a captured frame will be saved to `$PWD/img/foo.png`.

### 3. Configure and Start VARS Annotation

1. **Install VARS Annotation**: Download the latest release from the [VARS Annotation releases page](https://github.com/mbari-org/vars-annotation/releases).

2. **Connect to VARS services**: Open VARS Annotation and click the settings button. Enter your configuration URL. If you are running services locally with `vars-quickstart-public`, the URL is:

   ```
   https://localhost/config
   ```

   For full configuration details, see the [VARS Annotation setup docs](https://docs.mbari.org/vars-annotation/setup/).

3. **Log in**: The default account is username `admin` with password `admin`. You can create additional user accounts in the app as needed to track who is annotating.

4. **Configure the Blackmagic Server**: In the VARS Annotation settings dialog, select the **Blackmagic Server** tab and set the parameters to match those used to start libbmagic. Using the example script above, the settings would be:

   | Setting       | Value     |
   |---------------|-----------|
   | Host          | localhost |
   | Port          | 9000      |
   | API Key       | foo       |
   | Timeout (sec) | 5         |

## Troubleshooting

- **Services won't start**: Ensure Docker is running and ports 80 and 443 are not in use. Check logs with `./varsq docker logs <service-name>`.
- **Framecapture fails**: Verify the Blackmagic capture device is connected and recognized by the system. Check that the `--format` flag matches the incoming video signal.
- **VARS Annotation can't connect**: Confirm VARS services are running (`./varsq status`). If using self-signed SSL certificates, you may need to accept the certificate in your browser first by visiting `https://localhost` and trusting it.
- **Port 5000 conflict on macOS**: macOS uses port 5000 for AirPlay Receiver. If you use Sharktopoda for video playback, change its frame capture port from 5000 to 5001 in Sharktopoda's preferences.
- **Test framecapture timeout**: Increase the `--rto-ms` value in the test script, or verify the server is running and accessible on the configured host and port.
