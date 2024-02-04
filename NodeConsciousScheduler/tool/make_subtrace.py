import sys
import re

OFFSET = 431217

if len(sys.argv) != 3:
    print("Usage: python make_subtrace.py file1.txt file2.txt")
    sys.exit(1)

file1_name = sys.argv[1]
file2_name = sys.argv[2]

def extract_time_range(line):
    match = re.match(r'\[(\d+), (\d+)\] \(\d+h\)', line)
    if match:
        return map(int, match.groups())
    else:
        return None

with open(file1_name, 'r') as file1:
    time_ranges = []
    for line in file1:
        range_values = extract_time_range(line)
        if range_values:
            start, end = range_values
            start_time = start * 3600 + OFFSET
            end_time = end * 3600 + OFFSET
            time_ranges.append((start_time, end_time))

with open(file2_name, 'r') as file2:
    for i, (start, end) in enumerate(time_ranges, start=1):
        output_filename =  f'{file2_name}_output_{i}.txt'
        print(start, end)
        with open(output_filename, 'w') as output_file:
            file2.seek(0)  # Reset the file pointer to the beginning of file2
            for line in file2:
                fields = line.split()
                t = int(fields[1])
                if start <= t < end:
                    output_file.write(line.strip() + '\n')

