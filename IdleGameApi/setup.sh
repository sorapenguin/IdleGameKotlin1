#!/bin/bash
# =============================================
# IdleGame API - サーバー再セットアップスクリプト
#
# 【使い方】新しいVPSにSSH後、リポジトリのルートで実行:
#   1. cp .env.template .env && nano .env   ← .env を記入（手動）
#   2. chmod +x setup.sh && sudo ./setup.sh
#
# 【前提】Ubuntu 22.04 LTS / Debian 12 推奨
# 【注意】ドメインのDNSが新サーバーのIPを向いてから実行すること
# =============================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ─────────────────────────────────────────────
# 1. .env 読み込み・必須変数チェック
# ─────────────────────────────────────────────
if [ ! -f .env ]; then
    echo "ERROR: .env が見つかりません。"
    echo "  cp .env.template .env  して各値を記入してから再実行してください。"
    exit 1
fi

# shellcheck source=/dev/null
source .env

: "${DOMAIN:?DOMAIN が .env に設定されていません}"
: "${CERTBOT_EMAIL:?CERTBOT_EMAIL が .env に設定されていません}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD が .env に設定されていません}"
: "${JWT_SECRET:?JWT_SECRET が .env に設定されていません}"

echo "ドメイン: ${DOMAIN}"

# ─────────────────────────────────────────────
# 2. Docker インストール（未インストール時のみ）
# ─────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
    echo "Docker をインストールしています..."
    curl -fsSL https://get.docker.com | sh
    echo "Docker インストール完了。"
else
    echo "Docker は導入済みです: $(docker --version)"
fi

# ─────────────────────────────────────────────
# 3. nginx 設定ファイルをテンプレートから生成
# ─────────────────────────────────────────────
if [ ! -f nginx/default.conf.template ]; then
    echo "ERROR: nginx/default.conf.template が見つかりません。"
    exit 1
fi
sed "s/DOMAIN_PLACEHOLDER/${DOMAIN}/g" nginx/default.conf.template > nginx/default.conf
echo "nginx 設定生成完了: nginx/default.conf"

# ─────────────────────────────────────────────
# 4. SSL 証明書取得（Let's Encrypt）
# ─────────────────────────────────────────────
if [ ! -d "/etc/letsencrypt/live/${DOMAIN}" ]; then
    echo "SSL証明書を取得しています (${DOMAIN})..."

    # certbot が port 80 を使うため、先に nginx コンテナを止める
    docker compose -f docker-compose.prod.yml stop nginx 2>/dev/null || true

    if ! command -v certbot &>/dev/null; then
        apt-get update -q && apt-get install -y certbot
    fi

    certbot certonly \
        --standalone \
        --non-interactive \
        --agree-tos \
        --email "${CERTBOT_EMAIL}" \
        -d "${DOMAIN}"

    echo "SSL証明書取得完了。"
else
    echo "SSL証明書は既に存在します: /etc/letsencrypt/live/${DOMAIN}"
fi

# ─────────────────────────────────────────────
# 5. コンテナをビルド・起動
# ─────────────────────────────────────────────
echo "コンテナをビルド・起動しています（初回は数分かかります）..."
docker compose -f docker-compose.prod.yml up -d --build

echo ""
echo "=========================================="
echo "  起動完了！"
echo "  https://${DOMAIN} でアクセスできます"
echo "=========================================="
