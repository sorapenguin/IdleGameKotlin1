# 運用 Runbook — IdleGame API インフラ

## SLO / SLI 定義

| SLI | 目標 (SLO) | 測定方法 |
|-----|-----------|---------|
| API 可用性 | 99.5% / 月 | `curl -f https://<domain>/actuator/health` の成否 |
| API レスポンスタイム (p95) | < 500 ms | Grafana → cAdvisor / アプリログ |
| CPU 使用率 | 平均 < 60% | Grafana → Node Exporter |
| メモリ使用率 | 平均 < 70% | Grafana → Node Exporter |
| バックアップ成功 | 毎日実行・7世代保持 | `~/backups/backup.log` |

> 月間ダウンタイム許容: 99.5% = 約 3.6 時間 / 月

---

## 構成概要

```
VPS (Ubuntu / Nova xvps.ne.jp)
├── deploy ユーザー（SSH port 2222・鍵認証のみ）
├── Docker Compose（アプリ）       ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml
│   ├── idlegame-nginx  — Nginx 1.27 リバースプロキシ・HTTPS
│   ├── idlegame-api    — Spring Boot API (port 8080)
│   └── shared-postgres — PostgreSQL 16
└── Docker Compose（監視）         ~/IdleGameKotlin1/infrastructure/monitoring/docker-compose.monitoring.yml
    ├── prometheus      — メトリクス収集 (port 9090)
    ├── node-exporter   — VPS リソース監視 (port 9100)
    ├── cadvisor        — コンテナ監視 (port 8080)
    └── grafana         — ダッシュボード (port 3000・SSH tunnel 経由)
```

### アラートしきい値

| アラート名 | 条件 | Pending | 通知先 |
|-----------|------|---------|-------|
| High CPU Usage | CPU > 80% | 5 分 | Discord |
| High Memory Usage | Memory > 85% | 5 分 | Discord |
| API Container Down | コンテナ停止 | 1 分 | Discord |

---

## クイックリファレンス

```bash
# SSH接続
ssh -p 2222 deploy@x162-43-22-7.static.xvps.ne.jp

# 全コンテナ確認
docker ps

# APIヘルスチェック
curl -s https://x162-43-22-7.static.xvps.ne.jp/actuator/health | jq

# ログ確認 (最新100行)
docker logs idlegame-api --tail 100

# Grafana（SSH tunnel）
ssh -p 2222 -L 3000:localhost:3000 -N deploy@x162-43-22-7.static.xvps.ne.jp
# → ブラウザで http://localhost:3000
```

---

## 日常操作

### VPS への SSH 接続
```bash
ssh -p 2222 deploy@x162-43-22-7.static.xvps.ne.jp
```

### Grafana へのアクセス（SSH tunnel）
```bash
# ローカル PC で実行（接続したまま放置）
ssh -p 2222 -L 3000:localhost:3000 -N deploy@x162-43-22-7.static.xvps.ne.jp
```
ブラウザで http://localhost:3000 を開く

### コンテナ状態確認
```bash
docker ps
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml ps
```

### ログ確認
```bash
# API ログ
docker logs idlegame-api --tail 100 -f

# Nginx ログ
docker logs idlegame-nginx --tail 100 -f

# DB ログ
docker logs shared-postgres --tail 50
```

### バックアップ手動実行
```bash
~/backup-db.sh
ls -lh ~/backups/
tail -20 ~/backups/backup.log
```

---

## 障害対応

### P1: API 全断（503 / 接続不可）

**Discord アラート:** "API Container Down"

```bash
# 1. コンテナ状態を確認
docker ps -a

# 2. エラーログを確認
docker logs idlegame-api --tail 50

# 3. 再起動
cd ~/IdleGameKotlin1/IdleGameApi
docker compose -f docker-compose.prod.yml restart

# 4. 再起動後に疎通確認
curl -s https://x162-43-22-7.static.xvps.ne.jp/actuator/health
```

**DBが原因の場合（OOM / クラッシュ）:**
```bash
docker logs shared-postgres --tail 50
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml restart shared-postgres
```

**回復確認:** `curl -f https://x162-43-22-7.static.xvps.ne.jp/actuator/health` が `{"status":"UP"}` を返す

---

### P2: CPU/メモリ使用率高騰

**Discord アラート:** "High CPU Usage" / "High Memory Usage"

```bash
# 1. どのプロセスが消費しているか確認
top -b -n 1 | head -20
docker stats --no-stream

# 2. Nginx アクセスログで異常トラフィックを確認
docker logs idlegame-nginx --tail 200 | grep -v "health"

# 3. 不審なIPを UFW でブロック（例）
sudo ufw deny from 1.2.3.4

# 4. 高負荷な Docker イメージを削除してメモリを確保
docker image prune -f
```

---

### P3: ディスク逼迫（df -h で 80% 超）

```bash
# 使用量確認
df -h
du -sh ~/backups/*     # バックアップ
docker system df        # Docker

# 古いバックアップを手動削除（7日以上前）
find ~/backups/ -name "*.sql.gz" -mtime +7 -delete

# 未使用 Docker リソースを削除
docker system prune -f
```

---

### P4: HTTPS 証明書期限切れ

Let's Encrypt 証明書は通常 90 日ごと。期限切れの場合：

```bash
# 証明書の期限を確認
sudo certbot certificates

# 手動更新
sudo certbot renew

# Nginx に反映
docker exec idlegame-nginx nginx -s reload
```

---

### P5: GitHub Actions デプロイ失敗

1. GitHub → [Actions タブ](https://github.com/sorapenguin/IdleGameKotlin1/actions) でエラーログを確認
2. SSH 接続できるか確認（VPS_HOST / VPS_SSH_KEY が有効か）
3. VPS 上で手動デプロイを実行して原因を切り分ける

```bash
# VPS 上で手動デプロイ
cd ~/IdleGameKotlin1
git pull origin master
cd IdleGameApi
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
```

---

## DB バックアップ・復元

### 手動バックアップ
```bash
~/backup-db.sh
```

### 復元手順

```bash
# バックアップファイルを確認
ls -lh ~/backups/

# テスト復元（本番 DB への影響なし）
docker exec shared-postgres createdb -U IdleGame1 restore_test
gunzip -c ~/backups/idlegame_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i shared-postgres psql -U IdleGame1 restore_test
docker exec shared-postgres psql -U IdleGame1 restore_test -c "\dt"
docker exec shared-postgres dropdb -U IdleGame1 restore_test

# 本番 DB への復元（サービス停止推奨）
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml stop idlegame-api
gunzip -c ~/backups/idlegame_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i shared-postgres psql -U IdleGame1 idlegame_db
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml start idlegame-api
```

---

## セキュリティ

### fail2ban 状態確認
```bash
sudo fail2ban-client status
sudo fail2ban-client status sshd

# BAN されたIP
sudo fail2ban-client get sshd banned

# 手動 BAN 解除
sudo fail2ban-client set sshd unbanip <IP>
```

### UFW 状態確認
```bash
sudo ufw status verbose

# ルール追加例（IP ブロック）
sudo ufw deny from <攻撃元IP>
```

### 不審なログイン試行の確認
```bash
sudo journalctl -u ssh.service --since "24 hours ago" | grep "Failed\|Invalid"
```

---

## デプロイ

### 自動デプロイ
`master` ブランチに push → GitHub Actions → SSH でVPSにデプロイ  
（`IdleGameApi/` または `.github/workflows/` 配下の変更時のみ発火）

### 手動デプロイ
```bash
cd ~/IdleGameKotlin1
git pull origin master
cd IdleGameApi
docker compose -f docker-compose.prod.yml up -d --build
docker image prune -f
```

---

## 監視スタックの操作

### 起動
```bash
cd ~/IdleGameKotlin1/infrastructure/monitoring
docker compose -f docker-compose.monitoring.yml up -d
```

### 停止
```bash
docker compose -f docker-compose.monitoring.yml down
```

### Grafana ダッシュボード ID
| ダッシュボード | Import ID |
|--------------|-----------|
| Node Exporter Full（VPSリソース） | 1860 |
| cAdvisor（コンテナ） | 19792 |

---

## エスカレーション

| 状況 | 対応 |
|------|------|
| コンテナが再起動しても起動しない | OOM / 設定ミスを疑う。`docker logs` + `dmesg | tail` で確認 |
| DB が起動しない | データ破損の可能性。バックアップから復元を検討 |
| VPS 自体に到達できない | Nova コントロールパネルでVPS再起動 → 起動しない場合は再構築 |
| VPS 再構築が必要 | [RECOVERY_CHECKLIST.md](RECOVERY_CHECKLIST.md) に従って実施 |
