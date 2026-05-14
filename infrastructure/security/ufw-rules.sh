#!/bin/bash
# UFWルール再構築スクリプト
set -e
SSH_PORT=${1:-2222}
ufw default deny incoming
ufw default allow outgoing
ufw allow ${SSH_PORT}/tcp comment 'SSH'
ufw allow 80/tcp            comment 'HTTP'
ufw allow 443/tcp           comment 'HTTPS'
ufw --force enable
ufw status verbose
