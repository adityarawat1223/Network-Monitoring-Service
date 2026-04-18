# Network Monitoring Service (SSH-Based)

[![Java](https://img.shields.io/badge/Java-17-orange)]
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)]

## Overview

A backend service that monitors a remote Linux server using SSH and provides real-time system metrics.

The system uses **scheduled polling + Redis** to collect and serve metrics efficiently, along with a basic health alert mechanism.

---

## What it does

- Connects to remote servers using SSH (key-based authentication)
- Executes system-level commands
- Extracts and parses:
    - CPU usage
    - Memory usage
    - Uptime
- Returns structured JSON response via API

---

## API

### GET `/sysinfo`

Returns:

```json
{
  "CpuUsage": 0.0,
  "MemoryUsage": 7.0,
  "UpTime": "System Uptime is 1 Hr and 51 Mins"
}
```

## Prerequisites

1. Java 17
2. Redis
3. Linux Based Server 
4. Private Key from Linux Server

## Usage

```Java
ssh.connect("172.22.29.205");
ssh.authPublickey("burstingfire355", privateKeyPath);
```
Add your Host address , Name and Private Key path Here
and Start the Application

```Java
gradlew.bat bootRun ; // windows
```

## Linux Command Used

```linux
cat /proc/stat  (cpu info)
cat /proc/meminfo (mem info)
cat /proc/uptime (uptime info)
```