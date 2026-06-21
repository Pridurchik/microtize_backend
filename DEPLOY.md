# Деплой на сервер 157.22.188.37

## 1. Подготовка сервера (один раз)

Подключаемся по SSH:

```bash
ssh root@157.22.188.37
```

Устанавливаем Docker и Docker Compose (Ubuntu/Debian):

```bash
curl -fsSL https://get.docker.com | sh
apt install -y docker-compose-plugin
```

## 2. Копируем проект на сервер

С локальной машины (из папки проекта):

```bash
rsync -avz --exclude '.git' --exclude 'build' --exclude '.gradle' \
  ./ root@157.22.188.37:/opt/microtize_backend/
```

Либо через `git clone`, если репозиторий выложен на GitHub/GitLab:

```bash
ssh root@157.22.188.37
git clone <ваш-репозиторий-url> /opt/microtize_backend
```

## 3. Настраиваем переменные окружения

```bash
cd /opt/microtize_backend
cp .env.example .env
nano .env
```

Обязательно задайте надёжный `ADMIN_PASSWORD` — без него контейнер не запустится (есть защита в `docker-compose.yml`).

```
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<сильный пароль>
CORS_HOST=microtize.ru
```

## 4. Запуск

```bash
docker compose up -d --build
```

Проверка:

```bash
docker compose logs -f
curl http://localhost:8080/api/leads -X POST \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Тест","company":"Тест","email":"test@test.ru","phone":"123","description":"тест"}'
```

После этого приложение доступно:
- API: `http://157.22.188.37:8080/api/leads`
- Админка: `http://157.22.188.37:8080/admin`

## 5. Firewall

Если включён `ufw`, открыть порт:

```bash
ufw allow 8080/tcp
```

## 6. (Рекомендуется) HTTPS через Nginx + Let's Encrypt

Сейчас API ходит по обычному HTTP — пароль админки передаётся в открытом виде. Когда появится поддомен (например `api.microtize.ru`), направленный на `157.22.188.37`, поставьте Nginx как реверс-прокси и Certbot для TLS:

```bash
apt install -y nginx certbot python3-certbot-nginx
```

Пример конфига `/etc/nginx/sites-available/microtize-api`:

```nginx
server {
    listen 80;
    server_name api.microtize.ru;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

```bash
ln -s /etc/nginx/sites-available/microtize-api /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
certbot --nginx -d api.microtize.ru
```

После этого фронтенд на `microtize.ru` должен слать запросы на `https://api.microtize.ru` вместо `http://157.22.188.37:8080`.

## 7. Обновление после изменений в коде

```bash
cd /opt/microtize_backend
git pull   # или повторный rsync
docker compose up -d --build
```

База `leads.db` хранится в `./data/` на хосте и переживает пересборку контейнера.
