import sys
import re

OFFSET = 431217
SYS_SIZE_RATIO = 1/25

def extract_time_range(line):
    if consider_utilization:
        match = re.match(r'\[(\d+), (\d+)\] \(\d+h\) (\d+)', line)
    else:
        match = re.match(r'\[(\d+), (\d+)\] \(\d+h\)', line)
    
    if match:
        return map(int, match.groups())
    else:
        return None

def main():
    if len(sys.argv) < 3:
        print("Usage: python make_subtrace.py file1.txt file2.txt [--do-reduction] [--consider-utilization] ")
        sys.exit(1)

    file1_name = sys.argv[1]
    file2_name = sys.argv[2]
    global consider_utilization
    global do_reduction
    consider_utilization = False
    do_reduction = False
    if "--consider-utilization" in sys.argv:
        consider_utilization = True
    if "--do-reduction" in sys.argv:
        do_reduction = True
    if (not do_reduction) and consider_utilization:
        print("--do-reduction must be True when --consider-utilization is True.")
        sys.exit(1)

    time_ranges = []
    utilizations = []
    with open(file1_name, 'r') as file1:
        for line in file1:
            range_values = extract_time_range(line)
            if range_values:
                start, end = range_values
                start_time = start * 3600 + OFFSET
                end_time = end * 3600 + OFFSET
                time_ranges.append((start_time, end_time))
                if consider_utilization:
                    _, _, utilization = range_values
                    utilizations.append(utilization)

    with open(file2_name, 'r') as file2:
        for i, (start, end) in enumerate(time_ranges, start=1):
            output_filename = f'{file2_name}_output_{i}.txt'
            if consider_utilization:
                utilzation = utilizations[i-1]
            print(start, end)
            with open(output_filename, 'w') as output_file:
                file2.seek(0)  # Reset the file pointer to the beginning of file2
                for line in file2:
                    fields = line.split()
                    t = int(fields[1])
                    if do_reduction:
                        res1 = int(fields[4])
                        res2 = int(fields[7])
                        assert res1 == res2
                        new_res = int(res1 * SYS_SIZE_RATIO)
                        if consider_utilization:
                            new_res = int(new_res * utilzation)
                        fields[4] = str(new_res)
                        fields[7] = str(new_res)
                        line = ' '.join(fields) + '\n'
                    if start <= t < end:
                        output_file.write(line.strip() + '\n')

if __name__ == "__main__":
    main()

