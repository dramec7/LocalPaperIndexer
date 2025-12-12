import sys
import subprocess
import threading
import os

# ================= 配置区 =================
# 请把这里替换为你之前找到的【Java绝对路径】
JAVA_BIN = "/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home/bin/java"

# 请把这里替换为你 jar 包的【绝对路径】
JAR_PATH = "/Users/develop/LocalPaperIndexer/target/LocalPaperIndexer-1.0-SNAPSHOT.jar"
# =========================================

def forward(source, dest, log_file, prefix):
    """负责搬运数据，并把每一个字节都记下来"""
    with open(log_file, "wb") as f:
        while True:
            # 每次只读 1 个字节，保证绝对的实时性
            byte = source.read(1)
            if not byte:
                break
            dest.write(byte)
            dest.flush()

            # 记录日志：如果是回车换行，记为特殊符号，否则记原样
            if byte == b'\n':
                f.write(b'<LF>\n')
            elif byte == b'\r':
                f.write(b'<CR>')
            else:
                f.write(byte)
            f.flush()

# 启动真正的 Java 进程
cmd = [JAVA_BIN, "-Dfile.encoding=UTF-8", "-jar", JAR_PATH]
process = subprocess.Popen(
    cmd,
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=sys.stderr # 错误日志直接透传，不拦截
)

# 启动两个线程来搬运数据
# 1. Inspector -> Java (记录到 input.log)
t1 = threading.Thread(target=forward, args=(sys.stdin.buffer, process.stdin, "input.log", "IN"))
# 2. Java -> Inspector (记录到 output.log)
t2 = threading.Thread(target=forward, args=(process.stdout, sys.stdout.buffer, "output.log", "OUT"))

t1.start()
t2.start()
t1.join()
t2.join()