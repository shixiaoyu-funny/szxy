"""
DB 工具（MySQL 版）——直接连接 MySQL 进行查询/写操作的入口。

与 db_oracle_cli.py 的 CLI 接口保持一致（select / execute / insert / update / delete），
但本地 MySQL 可直接连接（localhost:3399），无需 JDBC 服务中转。

用法:
    python db_mysql_cli.py select "<SQL>"                查询（返回行列表）
    python db_mysql_cli.py execute "<SQL>"               执行写操作/DDL（返回受影响行数）
    python db_mysql_cli.py insert "<SQL>"                仅限 INSERT 前缀
    python db_mysql_cli.py update "<SQL>"                仅限 UPDATE 前缀
    python db_mysql_cli.py delete "<SQL>"                仅限 DELETE 前缀
    python db_mysql_cli.py shutdown                      兼容占位（无服务可关）

连接参数支持环境变量覆盖（默认即为本地开发环境）:
    MYSQL_HOST      默认 localhost
    MYSQL_PORT      默认 3399
    MYSQL_USER      默认 root
    MYSQL_PASSWORD  默认 200609
    MYSQL_DB        默认 xiangyue
"""
import json
import os
import sys

import pymysql
from pymysql.cursors import DictCursor

# 强制 stdout 使用 UTF-8，避免中文在管道/重定向时被按 GBK 解码
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# 允许执行的写操作前缀（白名单校验）
_WRITE_PREFIXES = ("INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP", "MERGE", "TRUNCATE")

_DEFAULT_CONFIG = {
    "host": os.environ.get("MYSQL_HOST", "localhost"),
    "port": int(os.environ.get("MYSQL_PORT", "3399")),
    "user": os.environ.get("MYSQL_USER", "root"),
    "password": os.environ.get("MYSQL_PASSWORD", "200609"),
    "database": os.environ.get("MYSQL_DB", "xiangyue"),
    "charset": "utf8mb4",
}


def _connect():
    return pymysql.connect(**{k: v for k, v in _DEFAULT_CONFIG.items() if k != "database"},
                           database=_DEFAULT_CONFIG["database"],
                           cursorclass=DictCursor,
                           autocommit=True)


def select(sql: str):
    """执行 SELECT 查询，返回 {"success": true, "columns": [...], "rows": [...]}"""
    conn = _connect()
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()
            columns = [desc[0] for desc in cur.description] if cur.description else []
        return {"success": True, "columns": columns, "rows": rows}
    finally:
        conn.close()


def execute(sql: str):
    """执行 INSERT/UPDATE/DELETE/DDL，返回 {"success": true, "affected": n}"""
    conn = _connect()
    try:
        with conn.cursor() as cur:
            affected = cur.execute(sql)
        return {"success": True, "affected": affected}
    finally:
        conn.close()


def insert(sql: str):
    stripped = sql.lstrip()
    if not stripped.upper().startswith("INSERT"):
        raise ValueError("insert() 只接受 INSERT 语句")
    return execute(sql)


def update(sql: str):
    stripped = sql.lstrip()
    if not stripped.upper().startswith("UPDATE"):
        raise ValueError("update() 只接受 UPDATE 语句")
    return execute(sql)


def delete(sql: str):
    stripped = sql.lstrip()
    if not stripped.upper().startswith("DELETE"):
        raise ValueError("delete() 只接受 DELETE 语句")
    return execute(sql)


def _print_result(result):
    print(json.dumps(result, ensure_ascii=False))


def main():
    if len(sys.argv) < 2:
        print("用法:", file=sys.stderr)
        print("  python db_mysql_cli.py select <SQL>   查询", file=sys.stderr)
        print("  python db_mysql_cli.py execute <SQL>  执行写操作/DDL", file=sys.stderr)
        print("  python db_mysql_cli.py insert/update/delete <SQL>  受限写操作", file=sys.stderr)
        print("  python db_mysql_cli.py shutdown       关闭服务（占位）", file=sys.stderr)
        sys.exit(1)

    cmd = sys.argv[1]

    if cmd == "shutdown":
        print('{"success": true, "message": "MySQL 直连无服务进程，无需关闭"}')
        return

    if cmd not in ("select", "insert", "update", "delete", "execute"):
        print(f"未知命令: {cmd}\n只支持 select / insert / update / delete / execute / shutdown", file=sys.stderr)
        sys.exit(1)

    if len(sys.argv) < 3:
        print(f"缺少 SQL 参数", file=sys.stderr)
        sys.exit(1)

    sql = sys.argv[2]

    try:
        if cmd == "select":
            result = select(sql)
        elif cmd == "insert":
            result = insert(sql)
        elif cmd == "update":
            result = update(sql)
        elif cmd == "delete":
            result = delete(sql)
        else:
            result = execute(sql)
        _print_result(result)
    except Exception as e:
        print(json.dumps({"success": False, "error": str(e)}, ensure_ascii=False))
        sys.exit(1)


if __name__ == "__main__":
    main()
