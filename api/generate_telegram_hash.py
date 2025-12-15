#!/usr/bin/env python3
"""
Скрипт для генерации валидного Telegram WebApp auth hash для тестирования.

ВАЖНО: Это алгоритм для Telegram WebApp (Mini App), 
НЕ для Login Widget (который использует SHA256 вместо HMAC).

Использование: python3 generate_telegram_hash.py
"""

import hashlib
import hmac
import time
import json
from urllib.parse import quote

# Конфигурация
BOT_TOKEN = "8353294626:AAGD0qbhUV2FzdCR2GIOv6CoIxv6xgUgb6g"
TELEGRAM_ID = 123456789
FIRST_NAME = "John"
LAST_NAME = "Doe"
USERNAME = "johndoe"
PHOTO_URL = "https://t.me/i/userpic/320/johndoe.jpg"


def calculate_webapp_hash(data_check_string: str, bot_token: str) -> str:
    """
    Вычисляет hash для Telegram WebApp согласно официальной документации:
    https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
    
    Алгоритм:
    1. secret_key = HMAC-SHA256(bot_token, "WebAppData")  # "WebAppData" - ключ
    2. hash = HMAC-SHA256(data_check_string, secret_key)
    """
    # Шаг 1: secret_key = HMAC-SHA256(bot_token, "WebAppData")
    secret_key = hmac.new(
        key=b"WebAppData",
        msg=bot_token.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    
    # Шаг 2: hash = HMAC-SHA256(data_check_string, secret_key)
    hash_value = hmac.new(
        key=secret_key,
        msg=data_check_string.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    return hash_value


def generate_webapp_init_data(
    telegram_id: int,
    first_name: str,
    last_name: str = None,
    username: str = None,
    photo_url: str = None,
    chat_instance: str = None,
    chat_type: str = None,
    bot_token: str = BOT_TOKEN
) -> dict:
    """
    Генерирует initData для Telegram WebApp с валидным hash.
    """
    auth_date = int(time.time())
    
    # Строим user JSON
    user_data = {"id": telegram_id, "first_name": first_name}
    if last_name:
        user_data["last_name"] = last_name
    if username:
        user_data["username"] = username
    if photo_url:
        user_data["photo_url"] = photo_url
    
    user_json = json.dumps(user_data, separators=(',', ':'))  # Компактный JSON
    
    # Собираем все поля для data-check-string (сортируем по алфавиту)
    params = {
        "auth_date": str(auth_date),
        "user": user_json,
    }
    if chat_instance:
        params["chat_instance"] = chat_instance
    if chat_type:
        params["chat_type"] = chat_type
    
    # Строим data-check-string: сортируем ключи, объединяем через \n
    sorted_keys = sorted(params.keys())
    data_check_string = '\n'.join(f"{key}={params[key]}" for key in sorted_keys)
    
    # Вычисляем hash
    hash_value = calculate_webapp_hash(data_check_string, bot_token)
    
    # Строим initData (URL-encoded)
    user_encoded = quote(user_json, safe='')
    init_data_parts = [f"auth_date={auth_date}"]
    if chat_instance:
        init_data_parts.append(f"chat_instance={chat_instance}")
    if chat_type:
        init_data_parts.append(f"chat_type={chat_type}")
    init_data_parts.append(f"user={user_encoded}")
    init_data_parts.append(f"hash={hash_value}")
    
    init_data = '&'.join(init_data_parts)
    
    return {
        "init_data": init_data,
        "auth_date": auth_date,
        "hash": hash_value,
        "data_check_string": data_check_string,
        "user": user_data,
    }


def main():
    print("=" * 70)
    print("Генератор Telegram WebApp Auth (initData)")
    print("=" * 70)
    print()
    
    # Генерируем данные
    result = generate_webapp_init_data(
        telegram_id=TELEGRAM_ID,
        first_name=FIRST_NAME,
        last_name=LAST_NAME,
        username=USERNAME,
        photo_url=PHOTO_URL,
        bot_token=BOT_TOKEN
    )
    
    print("Конфигурация:")
    print(f"  Bot Token: {BOT_TOKEN}")
    print(f"  Telegram ID: {TELEGRAM_ID}")
    print(f"  First Name: {FIRST_NAME}")
    print(f"  Last Name: {LAST_NAME}")
    print(f"  Username: {USERNAME}")
    print()
    
    print("Data check string:")
    print("-" * 50)
    print(result['data_check_string'])
    print("-" * 50)
    print()
    
    print(f"Вычисленный hash: {result['hash']}")
    print()
    
    print("initData (для отправки на сервер):")
    print("-" * 50)
    print(result['init_data'])
    print("-" * 50)
    print()
    
    # JSON для API запроса
    json_payload = json.dumps({"initData": result['init_data']}, indent=2)
    print("JSON для POST /v1/auth/telegram:")
    print(json_payload)
    print()
    
    # cURL команда
    print("cURL команда:")
    curl_command = f"""curl -X POST http://localhost:8091/v1/auth/telegram \\
  -H "Content-Type: application/json" \\
  -d '{json.dumps({"initData": result["init_data"]})}'"""
    print(curl_command)
    print()
    
    # Проверка актуальности
    current_time = int(time.time())
    time_diff = current_time - result['auth_date']
    print(f"⏰ Данные актуальны в течение 24 часов (86400 секунд)")
    print(f"✅ Данные актуальны еще {86400 - time_diff} секунд")
    print()
    
    print("=" * 70)
    print("Для тестирования с другими данными измените константы в начале файла")
    print("=" * 70)


if __name__ == "__main__":
    main()
