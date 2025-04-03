import numpy as np
import matplotlib.pyplot as plt

# パラメータ設定
length = 2140  # データの長さ
lambda_param = 0.1  # 指数分布のパラメータ
mean_target = 0.13  # 目標の平均値
std_target = 0.33  # 目標の標準偏差
max_target = 2.35  # 目標の最大値

# 指数分布を使用して時系列データを生成
data = np.random.exponential(scale=1/lambda_param, size=length)

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
plt.title('Generated Time Series Data')
plt.tight_layout()

# 画像を保存
plt.savefig('generated_data_plot.png')
plt.show()

# 時間のインデックスを作成
time_index = np.arange(len(data))

# データをタブ区切りのテキストファイルに保存
np.savetxt('generated_data.txt', np.column_stack((time_index, data)), delimiter='\t', fmt='%d\t%.6f')
