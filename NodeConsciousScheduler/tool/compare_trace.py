import sys
import numpy as np
import pandas as pd
from statsmodels.tsa.stattools import acf, pacf
from fastdtw import fastdtw
from scipy.spatial.distance import euclidean

def calculate_burstiness(data):
    burst_threshold = 0.1  # バーストとみなす閾値
    bursts = data[data > burst_threshold]
    burst_ratio = len(bursts) / len(data)
    return burst_ratio

def compare_acf_pacf(data1, data2, nlags=10):
    acf1 = acf(data1, nlags=nlags)
    acf2 = acf(data2, nlags=nlags)
    pacf1 = pacf(data1, nlags=nlags)
    pacf2 = pacf(data2, nlags=nlags)
    acf_diff = np.mean(np.abs(acf1 - acf2))
    pacf_diff = np.mean(np.abs(pacf1 - pacf2))
    return acf_diff, pacf_diff

def compare_dtw(data1, data2):
    distance, _ = fastdtw(data1, data2, dist=euclidean)
    return distance

def compare_time_series(file_a, file_b):
    # データの読み込み
    data_a = pd.read_csv(file_a, sep='\t', header=None, names=['time', 'value'])
    data_b = pd.read_csv(file_b, sep='\t', header=None, names=['time', 'value'])

    # バースト性の比較
    burstiness_a = calculate_burstiness(data_a['value'])
    burstiness_b = calculate_burstiness(data_b['value'])
    print(f"Burstiness of Data A: {burstiness_a:.4f}")
    print(f"Burstiness of Data B: {burstiness_b:.4f}")

    # ACFとPACFの比較
    acf_diff, pacf_diff = compare_acf_pacf(data_a['value'], data_b['value'], nlags=10)
    print(f"ACF difference: {acf_diff:.4f}")
    print(f"PACF difference: {pacf_diff:.4f}")

    # DTWの比較
    dtw_distance = compare_dtw(data_a['value'], data_b['value'])
    print(f"DTW distance: {dtw_distance:.4f}")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python script.py <file_a> <file_b>")
        sys.exit(1)

    file_a = sys.argv[1]
    file_b = sys.argv[2]

    compare_time_series(file_a, file_b)
