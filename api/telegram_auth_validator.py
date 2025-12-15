#!/usr/bin/env python3
"""
Telegram WebApp Auth Validator

Скрипт для валидации и отладки Telegram WebApp initData.
Полностью соответствует алгоритму из документации:
https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app

Использование:
    # Интерактивный режим
    python3 telegram_auth_validator.py
    
    # С параметрами
    python3 telegram_auth_validator.py --token "BOT_TOKEN" --init-data "INIT_DATA"
    
    # Генерация тестовых данных
    python3 telegram_auth_validator.py --generate --token "BOT_TOKEN"
"""

import hmac
import hashlib
import json
import time
import argparse
import sys
from urllib.parse import unquote, quote
from typing import Optional, Dict, Any
from dataclasses import dataclass


@dataclass
class TelegramUser:
    """Данные пользователя Telegram"""
    id: int
    first_name: str
    last_name: Optional[str] = None
    username: Optional[str] = None
    language_code: Optional[str] = None
    photo_url: Optional[str] = None
    allows_write_to_pm: Optional[bool] = None


@dataclass
class ValidationResult:
    """Результат валидации"""
    valid: bool
    user: Optional[TelegramUser] = None
    auth_date: Optional[int] = None
    error: Optional[str] = None
    debug_info: Optional[Dict[str, Any]] = None


class TelegramAuthValidator:
    """Валидатор Telegram WebApp initData"""
    
    def __init__(self, bot_token: str, skip_time_check: bool = False):
        """
        Args:
            bot_token: Токен бота из BotFather
            skip_time_check: Пропустить проверку времени (для отладки)
        """
        self.bot_token = bot_token.strip()
        self.skip_time_check = skip_time_check
        self._secret_key = self._compute_secret_key()
    
    def _compute_secret_key(self) -> bytes:
        """
        Вычисляет секретный ключ: HMAC-SHA256(bot_token, "WebAppData")
        где "WebAppData" - ключ, bot_token - данные
        """
        return hmac.new(
            key=b"WebAppData",
            msg=self.bot_token.encode('utf-8'),
            digestmod=hashlib.sha256
        ).digest()
    
    def _compute_hash(self, data_check_string: str) -> str:
        """
        Вычисляет хеш: HMAC-SHA256(data_check_string, secret_key)
        """
        return hmac.new(
            key=self._secret_key,
            msg=data_check_string.encode('utf-8'),
            digestmod=hashlib.sha256
        ).hexdigest()
    
    def _parse_init_data(self, init_data: str) -> Dict[str, str]:
        """Парсит initData как URL-encoded строку"""
        params = {}
        for pair in init_data.split('&'):
            if '=' in pair:
                key, value = pair.split('=', 1)
                params[key] = value
        return params
    
    def _build_data_check_string(self, params: Dict[str, str]) -> str:
        """
        Строит data-check-string:
        1. Исключает ТОЛЬКО hash (signature включается!)
        2. Сортирует по ключу
        3. URL-декодирует значения
        4. Объединяет через \n
        
        ВАЖНО: Согласно официальной библиотеке aiogram, signature НЕ исключается!
        """
        filtered = {k: v for k, v in params.items() 
                   if k != 'hash'}  # Только hash исключаем!
        sorted_keys = sorted(filtered.keys())
        
        parts = []
        for key in sorted_keys:
            decoded_value = unquote(filtered[key])
            parts.append(f"{key}={decoded_value}")
        
        return '\n'.join(parts)
    
    def validate(self, init_data: str) -> ValidationResult:
        """
        Валидирует initData от Telegram WebApp
        
        Args:
            init_data: URL-encoded строка от Telegram.WebApp.initData
            
        Returns:
            ValidationResult с результатом валидации
        """
        debug_info = {
            'init_data_length': len(init_data),
            'bot_token_id': self.bot_token.split(':')[0] if ':' in self.bot_token else 'unknown',
            'secret_key_hex': self._secret_key.hex(),
        }
        
        try:
            # Парсим initData
            params = self._parse_init_data(init_data)
            debug_info['parsed_fields'] = list(params.keys())
            
            # Получаем hash
            received_hash = params.get('hash')
            if not received_hash:
                return ValidationResult(
                    valid=False,
                    error="Missing 'hash' in initData",
                    debug_info=debug_info
                )
            debug_info['received_hash'] = received_hash
            
            # Строим data check string
            data_check_string = self._build_data_check_string(params)
            debug_info['data_check_string'] = data_check_string
            
            # Вычисляем хеш
            calculated_hash = self._compute_hash(data_check_string)
            debug_info['calculated_hash'] = calculated_hash
            debug_info['hash_match'] = calculated_hash.lower() == received_hash.lower()
            
            # Проверяем хеш
            if calculated_hash.lower() != received_hash.lower():
                return ValidationResult(
                    valid=False,
                    error=f"Hash mismatch: expected {calculated_hash}, got {received_hash}",
                    debug_info=debug_info
                )
            
            # Проверяем auth_date
            auth_date_str = params.get('auth_date')
            if not auth_date_str:
                return ValidationResult(
                    valid=False,
                    error="Missing 'auth_date' in initData",
                    debug_info=debug_info
                )
            
            try:
                auth_date = int(auth_date_str)
            except ValueError:
                return ValidationResult(
                    valid=False,
                    error=f"Invalid auth_date format: {auth_date_str}",
                    debug_info=debug_info
                )
            
            debug_info['auth_date'] = auth_date
            debug_info['auth_date_human'] = time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime(auth_date))
            
            # Проверяем что данные не старше 24 часов
            if not self.skip_time_check:
                current_time = int(time.time())
                time_diff = current_time - auth_date
                debug_info['time_diff_seconds'] = time_diff
                
                if time_diff > 86400:
                    return ValidationResult(
                        valid=False,
                        error=f"Data too old: {time_diff} seconds (max 86400)",
                        debug_info=debug_info
                    )
            
            # Парсим user
            user_encoded = params.get('user')
            if not user_encoded:
                return ValidationResult(
                    valid=False,
                    error="Missing 'user' in initData",
                    debug_info=debug_info
                )
            
            user_json = unquote(user_encoded)
            user_data = json.loads(user_json)
            debug_info['user_data'] = user_data
            
            if 'id' not in user_data or 'first_name' not in user_data:
                return ValidationResult(
                    valid=False,
                    error="User data missing required fields: id or first_name",
                    debug_info=debug_info
                )
            
            user = TelegramUser(
                id=user_data['id'],
                first_name=user_data['first_name'],
                last_name=user_data.get('last_name'),
                username=user_data.get('username'),
                language_code=user_data.get('language_code'),
                photo_url=user_data.get('photo_url'),
                allows_write_to_pm=user_data.get('allows_write_to_pm'),
            )
            
            return ValidationResult(
                valid=True,
                user=user,
                auth_date=auth_date,
                debug_info=debug_info
            )
            
        except Exception as e:
            debug_info['exception'] = str(e)
            return ValidationResult(
                valid=False,
                error=f"Validation error: {str(e)}",
                debug_info=debug_info
            )
    
    def generate_init_data(
        self,
        user_id: int,
        first_name: str,
        last_name: Optional[str] = None,
        username: Optional[str] = None,
        query_id: Optional[str] = None,
        chat_instance: Optional[str] = None,
        chat_type: Optional[str] = None,
    ) -> str:
        """
        Генерирует валидный initData для тестирования
        
        Returns:
            URL-encoded initData строка
        """
        auth_date = int(time.time())
        
        # Строим user JSON
        user_data = {"id": user_id, "first_name": first_name}
        if last_name:
            user_data["last_name"] = last_name
        if username:
            user_data["username"] = username
        
        user_json = json.dumps(user_data, separators=(',', ':'))
        
        # Собираем параметры
        params = {"auth_date": str(auth_date), "user": user_json}
        if query_id:
            params["query_id"] = query_id
        if chat_instance:
            params["chat_instance"] = chat_instance
        if chat_type:
            params["chat_type"] = chat_type
        
        # Строим data check string и вычисляем хеш
        sorted_keys = sorted(params.keys())
        data_check_parts = [f"{k}={params[k]}" for k in sorted_keys]
        data_check_string = '\n'.join(data_check_parts)
        
        hash_value = self._compute_hash(data_check_string)
        
        # Строим initData
        init_data_parts = []
        for key in sorted_keys:
            value = params[key]
            if key == 'user':
                value = quote(value, safe='')
            init_data_parts.append(f"{key}={value}")
        init_data_parts.append(f"hash={hash_value}")
        
        return '&'.join(init_data_parts)


def print_colored(text: str, color: str = 'white'):
    """Печатает цветной текст"""
    colors = {
        'red': '\033[91m',
        'green': '\033[92m',
        'yellow': '\033[93m',
        'blue': '\033[94m',
        'magenta': '\033[95m',
        'cyan': '\033[96m',
        'white': '\033[97m',
        'reset': '\033[0m',
    }
    print(f"{colors.get(color, '')}{text}{colors['reset']}")


def print_result(result: ValidationResult):
    """Красиво печатает результат валидации"""
    print("\n" + "=" * 70)
    
    if result.valid:
        print_colored("✅ VALIDATION SUCCESSFUL", 'green')
        print(f"\nUser ID: {result.user.id}")
        print(f"Name: {result.user.first_name} {result.user.last_name or ''}")
        if result.user.username:
            print(f"Username: @{result.user.username}")
        print(f"Auth Date: {time.strftime('%Y-%m-%d %H:%M:%S UTC', time.gmtime(result.auth_date))}")
    else:
        print_colored("❌ VALIDATION FAILED", 'red')
        print_colored(f"\nError: {result.error}", 'yellow')
    
    if result.debug_info:
        print("\n" + "-" * 70)
        print_colored("Debug Info:", 'cyan')
        
        for key, value in result.debug_info.items():
            if key == 'data_check_string':
                print(f"\n{key}:")
                for line in str(value).split('\n'):
                    if len(line) > 100:
                        print(f"  {line[:100]}...")
                    else:
                        print(f"  {line}")
            elif isinstance(value, str) and len(value) > 80:
                print(f"{key}: {value[:80]}...")
            else:
                print(f"{key}: {value}")
    
    print("=" * 70)


def interactive_mode():
    """Интерактивный режим работы"""
    print_colored("\n🔐 Telegram WebApp Auth Validator", 'cyan')
    print_colored("=" * 50, 'cyan')
    
    # Значение по умолчанию
    default_token = "8353294626:AAGD0qbhUV2FzdCR2GIOv6CoIxv6xgUgb6g"
    
    print(f"\nDefault token: {default_token[:30]}...")
    token_input = input("Bot Token (Enter для default): ").strip()
    bot_token = token_input if token_input else default_token
    
    print("\nВставьте initData (одной строкой, затем Enter):")
    init_data = input().strip()
    
    if not init_data:
        print_colored("initData не может быть пустым!", 'red')
        return
    
    validator = TelegramAuthValidator(bot_token, skip_time_check=True)
    result = validator.validate(init_data)
    print_result(result)
    
    # Предложить сгенерировать тестовые данные
    if not result.valid:
        print("\n" + "-" * 70)
        generate = input("Сгенерировать тестовые данные с этим токеном? (y/n): ").strip().lower()
        if generate == 'y':
            test_init_data = validator.generate_init_data(
                user_id=123456789,
                first_name="Test",
                last_name="User",
                username="testuser"
            )
            print_colored("\n✅ Generated test initData:", 'green')
            print(test_init_data)
            
            # Провалидируем сгенерированные данные
            print_colored("\n🔍 Validating generated data...", 'cyan')
            test_result = validator.validate(test_init_data)
            print_result(test_result)


def main():
    parser = argparse.ArgumentParser(
        description='Telegram WebApp Auth Validator',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Interactive mode
  python3 telegram_auth_validator.py
  
  # Validate initData
  python3 telegram_auth_validator.py --token "BOT_TOKEN" --init-data "INIT_DATA"
  
  # Generate test data
  python3 telegram_auth_validator.py --generate --token "BOT_TOKEN"
  
  # Skip time check (for old data)
  python3 telegram_auth_validator.py --skip-time-check --token "TOKEN" --init-data "DATA"
        """
    )
    
    parser.add_argument('--token', '-t', help='Bot token from BotFather')
    parser.add_argument('--init-data', '-d', help='initData string to validate')
    parser.add_argument('--generate', '-g', action='store_true', help='Generate test initData')
    parser.add_argument('--skip-time-check', '-s', action='store_true', help='Skip auth_date time check')
    parser.add_argument('--user-id', type=int, default=123456789, help='User ID for generation')
    parser.add_argument('--first-name', default='Test', help='First name for generation')
    parser.add_argument('--username', default='testuser', help='Username for generation')
    
    args = parser.parse_args()
    
    # Если нет аргументов - интерактивный режим
    if len(sys.argv) == 1:
        interactive_mode()
        return
    
    # Нужен токен для любой операции
    if not args.token:
        print_colored("Error: --token is required", 'red')
        parser.print_help()
        return
    
    validator = TelegramAuthValidator(args.token, skip_time_check=args.skip_time_check)
    
    if args.generate:
        # Генерация тестовых данных
        init_data = validator.generate_init_data(
            user_id=args.user_id,
            first_name=args.first_name,
            username=args.username
        )
        print_colored("✅ Generated initData:", 'green')
        print(init_data)
        
        print_colored("\n🔍 Validating...", 'cyan')
        result = validator.validate(init_data)
        print_result(result)
        
    elif args.init_data:
        # Валидация
        result = validator.validate(args.init_data)
        print_result(result)
        
    else:
        print_colored("Error: --init-data or --generate required", 'red')
        parser.print_help()


if __name__ == "__main__":
    main()

