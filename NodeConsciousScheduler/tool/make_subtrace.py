import sys
import re

OFFSET = 431217
SYS_SIZE_RATIO = 25
INT_JOB_QUEUE = 0

def extract_time_range(line):
    if consider_utilization:
        match = re.match(r'\[(\d+), (\d+)\] \(\d+h\) (\d+\.\d+)', line)
    else:
        match = re.match(r'\[(\d+), (\d+)\] \(\d+h\)', line)
    
    if match:
        if consider_utilization:
            return map(float, match.groups())
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
                if consider_utilization:
                    start, end, utilization = range_values
                    utilizations.append(utilization)
                else:
                    start, end = range_values

                start_time = int(start) * 3600 + OFFSET
                end_time = int(end) * 3600 + OFFSET
                time_ranges.append((start_time, end_time))

    with open(file2_name, 'r') as file2:
        file2_name = file2_name.split('.')[0]
        suffix = 'subtrace'
        if do_reduction:
            suffix += '_reduced'
            if consider_utilization:
                suffix += '_consider_utilization'
        for i, (start, end) in enumerate(time_ranges, start=1):
            output_filename = f'{file2_name}_{suffix}_{i}.txt'
            job_count = 0
            core_resource_count = 0
            cpu_resource_count = 0

            ijob_count = 0
            icore_resource_count = 0
            icpu_resource_count = 0
            if consider_utilization:
                utilzation = utilizations[i-1]
            print(start, end)
            with open(output_filename, 'w') as output_file:
                file2.seek(0)  # Reset the file pointer to the beginning of file2
                for line in file2:
                    fields = line.split()
                    t = int(fields[1])
                    is_int_job = int(fields[14]) == INT_JOB_QUEUE
                    if not is_int_job and do_reduction:
                        res1 = int(fields[4])
                        res2 = int(fields[7])
                        assert res1 == res2
                        new_res = int(res1 / SYS_SIZE_RATIO)
                        if res1 % SYS_SIZE_RATIO == 0:
                            new_res += 1
                        if consider_utilization:
                            new_res = int(new_res * utilzation)
                        new_res = max(1, new_res)
                        fields[4] = str(new_res)
                        fields[7] = str(new_res)
                        line = ' '.join(fields) + '\n'
                    if start <= t < end:
                        job_count += 1
                        core_resource_count += int(fields[4])
                        cpu_resource_count += int(fields[4])*int(fields[3])
                        output_file.write(line.strip() + '\n')
                        if is_int_job:
                            ijob_count += 1
                            icore_resource_count += int(fields[4])
                            icpu_resource_count += int(fields[4])*int(fields[3])
            print(f'Subtrace {i}: {ijob_count} {job_count} {ijob_count/job_count} \
{icore_resource_count} {core_resource_count} {icore_resource_count/core_resource_count} \
{icpu_resource_count} {cpu_resource_count} {icpu_resource_count/cpu_resource_count}')

if __name__ == "__main__":
    main()

