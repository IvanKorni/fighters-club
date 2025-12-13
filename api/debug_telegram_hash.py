#!/usr/bin/env python3
"""
Скрипт для отладки вычисления хеша Telegram WebApp.
Использует официальный алгоритм из документации Telegram.
"""

import hmac
import hashlib
from urllib.parse import unquote, parse_qs

# Токен бота из docker-compose.yml
BOT_TOKEN = "8353294626:AAGD0qbhUV2FzdCR2GIOv6CoIxv6xgUgb6g"

# Данные из логов (примерный формат initData)
# Реальный initData должен быть URL-encoded строкой
SAMPLE_INIT_DATA = """auth_date=1765652917&chat_instance=-6996832993589169568&chat_type=private&user=%7B%22id%22%3A564568468%2C%22first_name%22%3A%22Ivan%22%2C%22last_name%22%3A%22K%22%2C%22username%22%3A%22ivkopich%22%2C%22language_code%22%3A%22ru%22%2C%22allows_write_to_pm%22%3Atrue%2C%22photo_url%22%3A%22https%3A%5C%2F%5C%2Ft.me%5C%2Fi%5C%2Fuserpic%5C%2F320%5C%2FO2Kli1ZznFC5Eogr2M0YOv7lvo8o9X0tw55HWhVTohs.svg%22%7D&hash=b406f8b7f66e5a01fd9669eb4d6ef9616299feca74287c7e067f65c4b0f6761b"""


def validate_telegram_webapp_data(init_data: str, bot_token: str) -> dict:
    """
    Валидирует данные Telegram WebApp согласно официальной документации.
    https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
    """
    
    # Парсим init_data
    parsed = {}
    for pair in init_data.split('&'):
        if '=' in pair:
            key, value = pair.split('=', 1)
            parsed[key] = value
    
    # Получаем хеш
    received_hash = parsed.pop('hash', None)
    # Удаляем signature если есть
    parsed.pop('signature', None)
    
    if not received_hash:
        return {"valid": False, "error": "No hash in data"}
    
    # Сортируем по ключам и строим data-check-string
    # ВАЖНО: значения нужно URL-декодировать!
    sorted_keys = sorted(parsed.keys())
    
    print("\n=== Debug Info ===")
    print(f"Bot token: {bot_token}")
    print(f"Bot token bytes: {bot_token.encode('utf-8')}")
    print(f"\nReceived hash: {received_hash}")
    print(f"\nParsed params (before URL decode):")
    for key in sorted_keys:
        print(f"  {key} = {parsed[key][:50]}..." if len(parsed[key]) > 50 else f"  {key} = {parsed[key]}")
    
    # Вариант 1: С URL-декодированием (как делает наш Java код)
    data_check_parts_decoded = []
    for key in sorted_keys:
        decoded_value = unquote(parsed[key])
        data_check_parts_decoded.append(f"{key}={decoded_value}")
    data_check_string_decoded = '\n'.join(data_check_parts_decoded)
    
    # Вариант 2: Без URL-декодирования
    data_check_parts_raw = []
    for key in sorted_keys:
        data_check_parts_raw.append(f"{key}={parsed[key]}")
    data_check_string_raw = '\n'.join(data_check_parts_raw)
    
    print(f"\n=== Data Check String (URL-decoded) ===")
    print(data_check_string_decoded)
    
    print(f"\n=== Data Check String (raw, not decoded) ===")
    print(data_check_string_raw)
    
    # Вычисляем secret_key = HMAC-SHA256("WebAppData", bot_token)
    # ВАЖНО: В Python hmac(key, msg) - ключ первый!
    secret_key = hmac.new(
        key=b"WebAppData",
        msg=bot_token.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    
    print(f"\nSecret key (hex): {secret_key.hex()}")
    
    # Вычисляем hash для обоих вариантов
    calculated_hash_decoded = hmac.new(
        key=secret_key,
        msg=data_check_string_decoded.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    calculated_hash_raw = hmac.new(
        key=secret_key,
        msg=data_check_string_raw.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    print(f"\n=== Hash Comparison ===")
    print(f"Received hash:           {received_hash}")
    print(f"Calculated (decoded):    {calculated_hash_decoded}")
    print(f"Calculated (raw):        {calculated_hash_raw}")
    print(f"\nMatch (decoded): {calculated_hash_decoded == received_hash}")
    print(f"Match (raw):     {calculated_hash_raw == received_hash}")
    
    return {
        "valid": calculated_hash_decoded == received_hash or calculated_hash_raw == received_hash,
        "received_hash": received_hash,
        "calculated_hash_decoded": calculated_hash_decoded,
        "calculated_hash_raw": calculated_hash_raw,
    }


def test_simple_case():
    """Тест с простыми данными для проверки алгоритма"""
    print("\n" + "="*60)
    print("TEST: Simple case")
    print("="*60)
    
    import time
    auth_date = int(time.time())
    user_json = '{"id":123456789,"first_name":"John"}'
    
    # Строим data check string
    data_check = f"auth_date={auth_date}\nuser={user_json}"
    
    # Вычисляем хеш
    secret_key = hmac.new(
        key=b"WebAppData",
        msg=BOT_TOKEN.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    
    calculated_hash = hmac.new(
        key=secret_key,
        msg=data_check.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    print(f"Data check string:\n{data_check}")
    print(f"\nCalculated hash: {calculated_hash}")
    
    # Теперь проверяем что наш Java код выдаст такой же хеш
    from urllib.parse import quote
    user_encoded = quote(user_json, safe='')
    init_data = f"auth_date={auth_date}&user={user_encoded}&hash={calculated_hash}"
    print(f"\nGenerated initData: {init_data}")
    
    # Валидируем
    result = validate_telegram_webapp_data(init_data, BOT_TOKEN)
    print(f"\nValidation result: {result['valid']}")


def test_with_extra_fields():
    """Тест с дополнительными полями как в реальных данных Telegram"""
    print("\n" + "="*60)
    print("TEST: With extra fields (chat_instance, chat_type)")
    print("="*60)
    
    import time
    auth_date = int(time.time())
    chat_instance = "-6996832993589169568"
    chat_type = "private"
    user_json = '{"id":564568468,"first_name":"Ivan","last_name":"K","username":"ivkopich"}'
    
    # Строим data check string (поля отсортированы по алфавиту)
    data_check = f"auth_date={auth_date}\nchat_instance={chat_instance}\nchat_type={chat_type}\nuser={user_json}"
    
    # Вычисляем хеш
    secret_key = hmac.new(
        key=b"WebAppData",
        msg=BOT_TOKEN.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    
    calculated_hash = hmac.new(
        key=secret_key,
        msg=data_check.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    print(f"Data check string:\n{data_check}")
    print(f"\nCalculated hash: {calculated_hash}")
    
    # Строим initData
    from urllib.parse import quote
    user_encoded = quote(user_json, safe='')
    init_data = f"auth_date={auth_date}&chat_instance={chat_instance}&chat_type={chat_type}&user={user_encoded}&hash={calculated_hash}"
    
    # Валидируем
    result = validate_telegram_webapp_data(init_data, BOT_TOKEN)
    print(f"\nValidation result: {result['valid']}")


def test_real_data_from_logs():
    """Тест с реальными данными из логов"""
    print("\n" + "="*60)
    print("TEST: Real data from logs")
    print("="*60)
    
    # Это данные которые пришли от реального Telegram
    result = validate_telegram_webapp_data(SAMPLE_INIT_DATA, BOT_TOKEN)
    
    if not result['valid']:
        print("\n!!! VALIDATION FAILED !!!")
        print("This means either:")
        print("1. The bot token is wrong")
        print("2. The hash algorithm is different")
        print("3. The data check string format is different")


if __name__ == "__main__":
    test_simple_case()
    test_with_extra_fields()
    test_real_data_from_logs()

