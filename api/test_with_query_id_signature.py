#!/usr/bin/env python3
"""
Тест валидации с query_id и signature (как в реальных логах)
"""

import hmac
import hashlib
from urllib.parse import unquote, quote
import json

BOT_TOKEN = "8353294626:AAGD0qbhUV2FzdCR2GIOv6CoIxv6xgUgb6g"

# Данные из реальных логов
# query_id=AAGUoaYhAAAAAJShpiFl_7mN
# user с id=564568468, first_name=Ivan
# signature (неизвестно полное значение, но должно быть включено в расчет!)

def calculate_webapp_hash(data_check_string: str, bot_token: str) -> str:
    """Вычисляет хеш по алгоритму Telegram WebApp"""
    secret_key = hmac.new(
        key=b"WebAppData",
        msg=bot_token.encode('utf-8'),
        digestmod=hashlib.sha256
    ).digest()
    
    calculated_hash = hmac.new(
        key=secret_key,
        msg=data_check_string.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    
    return calculated_hash


def test_with_query_id_and_signature():
    """Тест с query_id и signature (как в реальных логах)"""
    print("="*70)
    print("TEST: With query_id and signature (as in real logs)")
    print("="*70)
    
    import time
    auth_date = int(time.time())
    query_id = "AAGUoaYhAAAAAJShpiFl_7mN"
    signature = "test_signature_value"  # В реальности это значение от Telegram
    user_json = '{"id":564568468,"first_name":"Ivan"}'
    
    # Строим data check string (ВАЖНО: signature ВКЛЮЧАЕТСЯ!)
    # Поля должны быть отсортированы по алфавиту
    params = {
        "auth_date": str(auth_date),
        "query_id": query_id,
        "signature": signature,
        "user": user_json
    }
    
    sorted_keys = sorted(params.keys())
    data_check_parts = [f"{k}={params[k]}" for k in sorted_keys]
    data_check_string = '\n'.join(data_check_parts)
    
    print(f"\nData check string:")
    print(data_check_string)
    
    # Вычисляем хеш
    calculated_hash = calculate_webapp_hash(data_check_string, BOT_TOKEN)
    print(f"\nCalculated hash: {calculated_hash}")
    
    # Строим initData (URL-encoded)
    user_encoded = quote(user_json, safe='')
    init_data_parts = [
        f"auth_date={auth_date}",
        f"query_id={query_id}",
        f"signature={signature}",
        f"user={user_encoded}",
        f"hash={calculated_hash}"
    ]
    init_data = '&'.join(init_data_parts)
    
    print(f"\nGenerated initData:")
    print(init_data)
    
    # Теперь валидируем
    print("\n" + "-"*70)
    print("Validation:")
    print("-"*70)
    
    # Парсим initData
    parsed = {}
    for pair in init_data.split('&'):
        if '=' in pair:
            key, value = pair.split('=', 1)
            parsed[key] = value
    
    received_hash = parsed.pop('hash', None)
    print(f"Received hash: {received_hash}")
    
    # Строим data check string для валидации (signature ВКЛЮЧАЕТСЯ!)
    sorted_keys = sorted(parsed.keys())
    data_check_parts = []
    for key in sorted_keys:
        decoded_value = unquote(parsed[key])
        data_check_parts.append(f"{key}={decoded_value}")
    data_check_string_valid = '\n'.join(data_check_parts)
    
    print(f"\nData check string for validation:")
    print(data_check_string_valid)
    
    calculated_hash_valid = calculate_webapp_hash(data_check_string_valid, BOT_TOKEN)
    print(f"\nCalculated hash (validation): {calculated_hash_valid}")
    
    match = calculated_hash_valid == received_hash
    print(f"\nMatch: {match}")
    
    if match:
        print("✅ VALIDATION SUCCESSFUL")
    else:
        print("❌ VALIDATION FAILED")
        print(f"Expected: {received_hash}")
        print(f"Got:      {calculated_hash_valid}")


def test_real_initdata_from_logs():
    """Тест с реальным initData из логов (с query_id и signature)"""
    print("\n" + "="*70)
    print("TEST: Real initData from logs (with query_id and signature)")
    print("="*70)
    
    # Из логов: query_id=AAGUoaYhAAAAAJShpiFl_7mN&user=%7B%22id%22%3A564568468%2C%22first_name%22%3A%22Ivan%22%2C%22...
    # Но полный initData неизвестен, так как signature обрезан в логах
    
    # Попробуем реконструировать на основе того, что есть
    # В логах видно что hash не совпадает, значит либо:
    # 1. Токен неверный
    # 2. signature не включен в расчет (но мы его включаем)
    # 3. Порядок полей неправильный
    
    print("\n⚠️  Для полного теста нужен полный initData из логов")
    print("   В логах видно только начало: query_id=AAGUoaYhAAAAAJShpiFl_7mN&user=...")


if __name__ == "__main__":
    test_with_query_id_and_signature()
    test_real_initdata_from_logs()



