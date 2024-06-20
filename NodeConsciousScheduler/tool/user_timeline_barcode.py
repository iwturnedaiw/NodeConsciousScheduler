import sys
from collections import defaultdict
import matplotlib.pyplot as plt

# 入力ファイルのパスを引数から取得
if len(sys.argv) < 2:
    print("使用法: python script.py <入力ファイルのパス>")
    sys.exit(1)

input_file = sys.argv[1]

# ユーザ毎の到着時間を格納する辞書
user_arrival_times = defaultdict(list)

# 入力ファイルを読み込んで処理
with open(input_file, "r") as file:
    for line in file:
        fields = line.strip().split()
        user_id = int(fields[11])
        arrival_time = int(fields[1])
        
        user_arrival_times[user_id].append(arrival_time)

# user_idで昇順にソート
sorted_user_arrival_times = sorted(user_arrival_times.items(), key=lambda x: x[0])

# タイムラインバーコードグラフの描画
fig, ax = plt.subplots(figsize=(10, 6))

y_labels = []
y_positions = []
for i, (user_id, arrival_times) in enumerate(sorted_user_arrival_times, start=1):
    y_labels.append(f"User {user_id}")
    y_positions.append(i)
    
    for arrival_time in arrival_times:
        ax.plot([arrival_time, arrival_time], [i-0.4, i+0.4], color='black', linewidth=0.5)

ax.set_yticks(y_positions)
ax.set_yticklabels(y_labels)
ax.set_xlabel("Time")
ax.set_title("User Arrival Timeline")
ax.grid(True)

plt.tight_layout()
plt.savefig("user_arrival_timeline.png")
plt.close()
