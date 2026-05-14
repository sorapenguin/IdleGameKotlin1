# 運用Runbook — IdleGame API インフラ

## 構成概要

```
VPS (Ubuntu / Nova xvps.ne.jp)
├── deploy ユーザー（SSH port 2222・鍵認証のみ）
├── Docker Compose（アプリ）
│   ├── nginx:1.27      — リバースプロキシ・HTTPS
│   ├── idlegame-api    — Spring Boot アプリ
│   └── shared-postgres — PostgreSQL 16
└── Docker Compose（監視）
    ├── prometheus      — メトリクス収集
    ├── node-exporter   — VPSリソース監視
    ├── cadvisor        — コンテナ監視
    └── grafana         — ダッシュボード（SSH tunnel経由）
```

---

## 日常操作

### VPSへのSSH接続
```bash
ssh -p 2222 deploy@x162-43-22-7.static.xvps.ne.jp
```

### Grafanaへのアクセス（SSH tunnel）
```bash
# ローカルPCで実行（接続したまま放置）
ssh -p 2222 -L 3000:localhost:3000 -N deploy@x162-43-22-7.static.xvps.ne.jp
```
ブラウザで http://localhost:3000 を開く（admin / admin）

### コンテナ状態確認
```bash
docker ps
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml ps
```

### ログ確認
```bash
# APIログ
docker logs idlegame-api --tail 100 -f

# Nginxログ
docker logs idlegame-nginx --tail 100 -f

# DBログ
docker logs shared-postgres --tail 50
```

### バックアップ手動実行
```bash
~/backup-db.sh
ls -lh ~/backups/
```

### バックアップログ確認
```bash
tail -20 ~/backups/backup.log
```

---

## 障害対応

### コンテナが落ちている場合
```bash
# 状態確認
docker ps -a

# 再起動
cd ~/IdleGameKotlin1/IdleGameApi
docker compose -f docker-compose.prod.yml restart

# ログで原因確認
docker logs idlegame-api --tail 50
```

### APIが503を返す場合
```bash
# アプリとnginxの疎通確認
docker exec idlegame-nginx wget -qO- http://app:8080/actuator/health

# DBへの接続確認
docker exec shared-postgres pg_isready -U IdleGame1 -d idlegame_db
```

### ディスクがひっ迫している場合
```bash
df -h
du -sh ~/backups/*
docker system df

# 古いDockerイメージを削除
docker image prune -f
```

---

## DBバックアップ・復元

### 手動バックアップ
```bash
~/backup-db.sh
```

### 復元手順
```bash
# バックアップファイルを確認
ls -lh ~/backups/

# テスト復元（本番DBへの影響なし）
docker exec shared-postgres createdb -U IdleGame1 restore_test
gunzip -c ~/backups/idlegame_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i shared-postgres psql -U IdleGame1 restore_test
docker exec shared-postgres psql -U IdleGame1 restore_test -c "\dt"
docker exec shared-postgres dropdb -U IdleGame1 restore_test

# 本番DBへの復元（※サービス停止推奨）
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml stop app
gunzip -c ~/backups/idlegame_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i shared-postgres psql -U IdleGame1 idlegame_db
docker compose -f ~/IdleGameKotlin1/IdleGameApi/docker-compose.prod.yml start app
```

---

## セキュリティ

### fail2ban 状態確認
```bash
sudo fail2ban-client status
sudo fail2ban-client status sshd

# BANされたIPの確認
sudo fail2ban-client get sshd banned
```

### UFW 状態確認
```bash
sudo ufw status verbose
```

### 不審なログイン試行の確認
```bash
sudo journalctl -u ssh.service --since "24 hours ago" | grep "Failed\|Invalid"
```

---

## デプロイ

### 自動デプロイ
`master` ブランチに push → GitHub Actions が自動デプロイ（`IdleGameApi/` 配下の変更時のみ）

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
