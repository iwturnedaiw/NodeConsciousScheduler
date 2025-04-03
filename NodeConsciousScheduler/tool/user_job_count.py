import sys
from collections import defaultdict

# 入力ファイルのパスを引数から取得
if len(sys.argv) < 2:
    print("使用法: python script.py <入力ファイルのパス>")
    sys.exit(1)

input_file = sys.argv[1]

# ユーザ毎の情報を格納する辞書
user_data = defaultdict(lambda: {'count': 0, 'total_exec_time': 0, 'total_cpu_time': 0})

# 全体の総和を格納する変数
total_count = 0
total_exec_time = 0
total_cpu_time = 0

# 入力ファイルを読み込んで処理
with open(input_file, "r") as file:
    for line in file:
        fields = line.strip().split()
        user_id = int(fields[11])
        exec_time = int(fields[3])
        parallel = int(fields[4])
        
        user_data[user_id]['count'] += 1
        user_data[user_id]['total_exec_time'] += exec_time
        user_data[user_id]['total_cpu_time'] += exec_time * parallel
        
        total_count += 1
        total_exec_time += exec_time
        total_cpu_time += exec_time * parallel

# CSVヘッダを出力
print("user_id,count,count(ratio),total_exec_time,total_exec_time(ratio),total_cpu_time,total_cpu_time(ratio)")

# 結果をCSV形式で出力
for user_id, data in user_data.items():
    count_ratio = data['count'] / total_count
    exec_time_ratio = data['total_exec_time'] / total_exec_time
    cpu_time_ratio = data['total_cpu_time'] / total_cpu_time
    
    print(f"{user_id},{data['count']},{count_ratio:.4f},{data['total_exec_time']},{exec_time_ratio:.4f},{data['total_cpu_time']},{cpu_time_ratio:.4f}")
