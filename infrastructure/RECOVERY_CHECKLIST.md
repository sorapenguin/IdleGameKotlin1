# VPS再構築チェックリスト

VPSを削除・再作成した場合の復旧手順。上から順に実施する。

---

## STEP 1: VPS初期設定（root で作業）

```bash
adduser deploy
usermod -aG sudo docker deploy
```

```bash
# ローカルPCの公開鍵を追加
mkdir -p /home/deploy/.ssh
echo "ssh-ed25519 AAAA...（ローカルPCの~/.ssh/id_ed25519.pubの内容）" \
  >> /home/deploy/.ssh/authorized_keys
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys
```

```bash
# SSH port 2222 に変更（socket activation）
mkdir -p /etc/systemd/system/ssh.socket.d/
tee /etc/systemd/system/ssh.socket.d/override.conf << 'EOF'
[Socket]
ListenStream=
ListenStream=0.0.0.0:2222
ListenStream=[::]:2222
EOF
systemctl daemon-reload
systemctl restart ssh.socket ssh.service
```

- [ ] `ssh -p 2222 deploy@NEW_VPS_IP` で別ターミナルから接続できることを確認してから次へ

```bash
# sshd_config 更新
sed -i 's/^PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/^PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl restart ssh.service
```

---

## STEP 2: ファイアウォール（deploy で作業）

```bash
sudo bash ~/IdleGameKotlin1/infrastructure/security/ufw-rules.sh 2222
```

- [ ] `sudo ufw status verbose` で 2222/80/443 のみ表示されることを確認

---

## STEP 3: fail2ban

```bash
sudo apt install fail2ban -y
sudo cp ~/IdleGameKotlin1/infrastructure/security/jail.local /etc/fail2ban/jail.local
sudo systemctl enable fail2ban && sudo systemctl start fail2ban
```

- [ ] `sudo fail2ban-client status sshd` で稼働確認

---

## STEP 4: リポジトリ取得

```bash
cd ~
git clone https://github.com/sorapenguin/IdleGameKotlin1.git
cd IdleGameKotlin1
git checkout master
```

---

## STEP 5: .env ファイルの復元　※手動

```bash
nano ~/IdleGameKotlin1/IdleGameApi/.env
```

GitHub Secrets または別途保管しているバックアップから以下を記入：

```
POSTGRES_DB=idlegame_db
POSTGRES_USER=IdleGame1
POSTGRES_PASSWORD=（本番パスワード）
JWT_SECRET=（本番シークレット）
```

- [ ] `.env` が存在することを確認

---

## STEP 6: アプリ起動

```bash
cd ~/IdleGameKotlin1/IdleGameApi
docker compose -f docker-compose.prod.yml up -d
docker ps
```

- [ ] nginx / app / postgres の3コンテナが `Up` であることを確認

---

## STEP 7: Let's Encrypt SSL証明書　※手動

```bash
sudo apt install certbot -y
sudo certbot certonly --standalone \
  -d NEW_DOMAIN \
  --email your-email@example.com \
  --agree-tos --non-interactive
```

- [ ] `curl -I https://NEW_DOMAIN/` で 200 または 403 が返ることを確認

---

## STEP 8: DBデータの復元　※データがある場合のみ

```bash
# バックアップをVPSにコピー（ローカルから）
scp -P 2222 idlegame_YYYYMMDD.sql.gz deploy@NEW_VPS_IP:~/backups/

# 復元
gunzip -c ~/backups/idlegame_YYYYMMDD.sql.gz \
  | docker exec -i shared-postgres psql -U IdleGame1 idlegame_db
```

- [ ] APIにアクセスしてデータが戻っていることを確認

---

## STEP 9: バックアップ cron 登録

```bash
mkdir -p ~/backups
cp ~/IdleGameKotlin1/infrastructure/backup/backup-db.sh ~/backup-db.sh
chmod +x ~/backup-db.sh
(crontab -l 2>/dev/null; echo "0 3 * * * /home/deploy/backup-db.sh >> /home/deploy/backups/backup.log 2>&1") | crontab -
```

- [ ] `crontab -l` で登録確認

---

## STEP 10: 監視スタック起動

```bash
cd ~/IdleGameKotlin1/infrastructure/monitoring
docker compose -f docker-compose.monitoring.yml up -d
docker ps | grep monitoring
```

- [ ] 4コンテナ（prometheus/node-exporter/cadvisor/grafana）が `Up` であることを確認

---

## STEP 11: Grafana 設定の復元　※手動

SSH tunnelを張ってブラウザで http://localhost:3000 を開く：

```bash
# ローカルPCで
ssh -p 2222 -L 3000:localhost:3000 -N deploy@NEW_VPS_IP
```

1. **Alerting → Notification policies** → Default policy → `discord` に変更
   - Discord Webhook URL を再設定
2. **Dashboards → Import**
   - ID `1860`（Node Exporter Full）をインポート
   - ID `19792`（cAdvisor）をインポート
3. **Alerting → Alert rules** → 3つのルールを再作成
   - `infrastructure/monitoring/grafana/provisioning/alerting/` のファイルを参照

- [ ] Grafanaダッシュボードでメトリクスが表示されることを確認
- [ ] テストアラートがDiscordに届くことを確認

---

## STEP 12: GitHub Secrets 更新　※IPが変わった場合

`https://github.com/sorapenguin/IdleGameKotlin1/settings/secrets/actions`

- [ ] `VPS_HOST` を新しいIPに更新
- [ ] GitHub Actionsを手動実行して自動デプロイが動くことを確認

---

## 復旧完了チェック

```bash
# API疎通
curl -I https://NEW_DOMAIN/

# セキュリティヘッダ確認
curl -I https://NEW_DOMAIN/ | grep -E "Strict|X-Frame|X-Content"

# バックアップ動作確認
~/backup-db.sh

# 全コンテナ確認
docker ps
```
