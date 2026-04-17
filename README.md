# Network Monitoring Service (SSH-Based)

[![Java](https://img.shields.io/badge/Java-17-orange)]
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)]

## Overview

A backend service that connects to remote Linux machines using SSH and retrieves real-time system metrics like CPU usage, memory usage, and uptime.

This project focuses on interacting with actual systems rather than just building APIs. It executes commands remotely, parses raw output, and exposes structured metrics via REST endpoints.

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