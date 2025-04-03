import numpy as np
import matplotlib.pyplot as plt

# パラメータ設定
length = 2140  # データの長さ
a = 1.5  # Pareto分布のパラメータ（a > 0）
min_val = 0.001  # Pareto分布の最小値
mean_target = 0.13  # 目標の平均値
std_target = 0.33  # 目標の標準偏差
max_target = 2.35  # 目標の最大値

# Pareto分布を使用して時系列データを生成
data = (np.random.pareto(a, size=length) + 1) * min_val

# 平均値と標準偏差を調整
data_mean = np.mean(data)
data_std = np.std(data)
data = (data - data_mean) * (std_target / data_std) + mean_target

# 最大値を調整
data_max = np.max(data)
data = data * (max_target / data_max)

# 負の値を0に置き換える
data = np.where(data < 0, 0, data)

# 結果をプロット
plt.figure(figsize=(12, 4))
plt.plot(data)
plt.xlabel('Time')
plt.ylabel('Value')
plt.title('Generated Time Series Data with Burstiness')
plt.tight_layout()

# 画像を保存
plt.savefig('generated_data_plot_pareto.png')
plt.show()

# 時間のインデックスを作成
time_index = np.arange(len(data))

# データをタブ区切りのテキストファイルに保存
np.savetxt('generated_data_pareto.txt', np.column_stack((time_index, data)), delimiter='\t', fmt='%d\t%.6f')
