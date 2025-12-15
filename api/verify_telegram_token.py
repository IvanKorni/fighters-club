#!/usr/bin/env python3
"""
Скрипт для проверки токена бота Telegram.

Использование:
  python3 verify_telegram_token.py <initData_from_logs> <bot_token>

Или запустите без аргументов для интерактивного ввода.
"""

import hmac
import hashlib
import sys
from urllib.parse import unquote, parse_qs


def calculate_webapp_hash(data_check_string: str, bot_token: str) -> str:
    """Вычисляет хеш по алгоритму Telegram WebApp"""
    # secret_key = HMAC-SHA256(bot_token, "WebAppData")
    # где "WebAppData" - ключ, bot_token - данные
    secret_key = hmac.new(
        key=b"WebAppData",
        msg=bot_token.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    
    # hash = HMAC-SHA256(data_check_string, secret_key)
    calculated_hash = hmac.new(
        key=secret_key,
        msg=data_check_string.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    return calculated_hash


def parse_and_validate(init_data: str, bot_token: str) -> dict:
    """Парсит initData и проверяет хеш"""
    
    # Парсим initData
    parsed = {}
    for pair in init_data.split('&'):
        if '=' in pair:
            key, value = pair.split('=', 1)
            parsed[key] = value
    
    received_hash = parsed.pop('hash', None)
    parsed.pop('signature', None)  # Удаляем если есть
    
    if not received_hash:
        return {"error": "No hash in initData"}
    
    # Сортируем и строим data-check-string (с URL-decode)
    sorted_keys = sorted(parsed.keys())
    data_check_parts = []
    for key in sorted_keys:
        decoded_value = unquote(parsed[key])
        data_check_parts.append(f"{key}={decoded_value}")
    data_check_string = '\n'.join(data_check_parts)
    
    # Вычисляем хеш
    calculated_hash = calculate_webapp_hash(data_check_string, bot_token)
    
    return {
        "received_hash": received_hash,
        "calculated_hash": calculated_hash,
        "match": received_hash == calculated_hash,
        "data_check_string": data_check_string,
    }


def find_matching_token(init_data: str, token_prefix: str = None) -> None:
    """
    Пытается найти правильный токен методом подбора.
    Если хеши не совпадают, возможно токен неверный.
    """
    print("\n" + "="*70)
    print("Проверка токена бота")
    print("="*70)
    
    # Парсим initData
    parsed = {}
    for pair in init_data.split('&'):
        if '=' in pair:
            key, value = pair.split('=', 1)
            parsed[key] = value
    
    received_hash = parsed.get('hash')
    if not received_hash:
        print("ОШИБКА: В initData отсутствует hash")
        return
    
    print(f"\nПолученный hash от Telegram: {received_hash}")
    
    # Если указан токен, проверяем его
    if token_prefix:
        result = parse_and_validate(init_data, token_prefix)
        print(f"\nПроверка токена: {token_prefix[:20]}...")
        print(f"Вычисленный hash: {result['calculated_hash']}")
        
        if result['match']:
            print("\n✅ ТОКЕН ВЕРНЫЙ! Хеши совпадают.")
        else:
            print("\n❌ ТОКЕН НЕВЕРНЫЙ! Хеши не совпадают.")
            print("\nВозможные причины:")
            print("1. Токен бота не соответствует боту Mini App")
            print("2. initData был изменен при передаче")
            print("3. initData от другого бота")
    
    print("\n" + "-"*70)
    print("Data check string:")
    print("-"*70)
    
    # Показываем data check string
    parsed_copy = dict(parsed)
    parsed_copy.pop('hash', None)
    parsed_copy.pop('signature', None)
    sorted_keys = sorted(parsed_copy.keys())
    for key in sorted_keys:
        decoded_value = unquote(parsed_copy[key])
        # Обрезаем длинные значения для читаемости
        if len(decoded_value) > 100:
            print(f"{key}={decoded_value[:100]}...")
        else:
            print(f"{key}={decoded_value}")


def main():
    print("="*70)
    print("Диагностика Telegram WebApp Authentication")
    print("="*70)
    
    # Данные из логов пользователя
    # Можно заменить на реальные данные
    sample_init_data = """auth_date=1765652917&chat_instance=-6996832993589169568&chat_type=private&user=%7B%22id%22%3A564568468%2C%22first_name%22%3A%22Ivan%22%2C%22last_name%22%3A%22K%22%2C%22username%22%3A%22ivkopich%22%2C%22language_code%22%3A%22ru%22%2C%22allows_write_to_pm%22%3Atrue%2C%22photo_url%22%3A%22https%3A%5C%2F%5C%2Ft.me%5C%2Fi%5C%2Fuserpic%5C%2F320%5C%2FO2Kli1ZznFC5Eogr2M0YOv7lvo8o9X0tw55HWhVTohs.svg%22%7D&hash=b406f8b7f66e5a01fd9669eb4d6ef9616299feca74287c7e067f65c4b0f6761b"""
    
    current_token = "8353294626:AAGD0qbhUV2FzdCR2GIOv6CoIxv6xgUgb6g"
    
    if len(sys.argv) >= 3:
        init_data = sys.argv[1]
        bot_token = sys.argv[2]
    elif len(sys.argv) == 2:
        init_data = sample_init_data
        bot_token = sys.argv[1]
    else:
        init_data = sample_init_data
        bot_token = current_token
    
    print(f"\nТекущий токен бота: {bot_token}")
    print(f"Bot ID: {bot_token.split(':')[0] if ':' in bot_token else 'unknown'}")
    
    find_matching_token(init_data, bot_token)
    
    # Проверка с несколькими вариантами токена (если есть подозрение на опечатки)
    print("\n" + "="*70)
    print("РЕКОМЕНДАЦИИ:")
    print("="*70)
    print("""
1. Убедитесь, что Mini App зарегистрирован для бота с ID 8353294626
   (проверьте в @BotFather -> /mybots -> выберите бота -> Bot Settings -> Configure Mini App)

2. Если Mini App зарегистрирован для другого бота:
   - Либо перенастройте Mini App на правильного бота
   - Либо обновите TELEGRAM_BOT_TOKEN в docker-compose.yml и application.yml

3. Для получения токена бота:
   - Откройте @BotFather
   - Отправьте /mybots
   - Выберите бота, связанного с вашим Mini App
   - Нажмите "API Token"
   
4. После изменения токена перезапустите бэкенд:
   docker-compose restart api
""")


if __name__ == "__main__":
    main()



