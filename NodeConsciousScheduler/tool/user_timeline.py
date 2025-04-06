import sys
from collections import defaultdict
import matplotlib.pyplot as plt

# 入力ファイルのパスを引数から取得
if len(sys.argv) < 2:
    print("使用法: python script.py <入力ファイルのパス>")
    sys.exit(1)

input_file = sys.argv[1]

# ユーザ毎の情報を格納する辞書
user_data = defaultdict(lambda: {'first_arrival_time': float('inf'), 'last_arrival_time': 0})

# 入力ファイルを読み込んで処理
with open(input_file, "r") as file:
    for line in file:
        fields = line.strip().split()
        user_id = int(fields[11])
        arrival_time = int(fields[1])
        
        user_data[user_id]['first_arrival_time'] = min(user_data[user_id]['first_arrival_time'], arrival_time)
        user_data[user_id]['last_arrival_time'] = max(user_data[user_id]['last_arrival_time'], arrival_time)

# CSVヘッダを出力
print("user_id,first_arrival_time,last_arrival_time")

# user_idで昇順にソートしてCSV形式で出力
sorted_user_data = sorted(user_data.items(), key=lambda x: x[0])
for user_id, data in sorted_user_data:
    print(f"{user_id},{data['first_arrival_time']},{data['last_arrival_time']}")

# タイムライングラフの描画
fig, ax = plt.subplots(figsize=(10, 6))

y_labels = []
y_positions = []
for i, (user_id, data) in enumerate(sorted_user_data, start=1):
    y_labels.append(f"User {user_id}")
    y_positions.append(i)
    ax.plot([data['first_arrival_time'], data['last_arrival_time']], [i, i], linewidth=8)

ax.set_yticks(y_positions)
ax.set_yticklabels(y_labels)
ax.set_xlabel("Time")
ax.set_title("User Arrival Time Range")
ax.grid(True)

plt.tight_layout()
plt.savefig("user_arrival_time_range.png")
plt.close()
