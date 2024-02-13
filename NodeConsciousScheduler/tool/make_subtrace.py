import math
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

def print_distirbution(distribution):
    max_key = -1
    for key, value in distribution.items():
        if key > max_key:
            max_key = key
    for i in range(1, max_key+1):
        if i in distribution:
            print(f'{i}: {distribution[i]}', end=', ')
        else:
            print(f'{i}: 0', end=', ')
    print(f'')

def main():
    if len(sys.argv) < 3:
        print("Usage: python make_subtrace.py file1.txt file2.txt [--do-reduction] [--consider-utilization] ")
        sys.exit(1)

    file1_name = sys.argv[1]
    file2_name = sys.argv[2]
    global consider_utilization
    global do_reduction
    global trim_maximum_time
    global do_reduction_all_one
    consider_utilization = False
    trim_maximum_time = False
    do_reduction = False
    do_reduction_all_one = False
    if "--consider-utilization" in sys.argv:
        consider_utilization = True
    if "--do-reduction" in sys.argv:
        do_reduction = True
    if "--do-reduction-all-one" in sys.argv:
        do_reduction_all_one = True
    if "--trim-maximum-time" in sys.argv:
        trim_maximum_time = True
    if (not do_reduction) and consider_utilization:
        print("--do-reduction must be True when --consider-utilization is True.")
        sys.exit(1)
    if do_reduction and do_reduction_all_one:
        print("Both --do-reduction and --do-reduction-all-one cannnot be True.")
        sys.exit(1)
    if consider_utilization and do_reduction_all_one:
        print("Both --consider-utilization and --do-reduction-all-one cannnot be True.")
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
        if trim_maximum_time:
            suffix += '_trimmedMaximumTime'
        if do_reduction:
            suffix += '_reduced'
            if consider_utilization:
                suffix += '_consider_utilization'
        if do_reduction_all_one:
            suffix += '_reduced_to_one'
        for i, (start, end) in enumerate(time_ranges, start=1):
            maximum_executing_time = end - start
            output_filename = f'{file2_name}_{suffix}_{i}.swf'
            job_count = 0
            core_resource_count = 0
            cpu_resource_count = 0

            ijob_count = 0
            icore_resource_count = 0
            icpu_resource_count = 0
            if consider_utilization:
                utilzation = utilizations[i-1]

            org_batch_rsc1 = 0
            org_batch_rsc2 = 0
            batch_rsc1 = 0
            batch_rsc2 = 0

            org_distribution = {}
            new_distribution = {}

            with open(output_filename, 'w') as output_file:
                file2.seek(0)  # Reset the file pointer to the beginning of file2
                for line in file2:
                    fields = line.split()
                    t = int(fields[1])

                    if not (start <= t < end):
                        continue

                    execution_time = int(fields[3])
                    required_time = int(fields[8])

                    if trim_maximum_time and execution_time > maximum_executing_time:
                        distance = execution_time - maximum_executing_time
                        new_required_time = required_time - distance
                        new_execution_time = maximum_executing_time
                        fields[3] = str(new_execution_time)
                        fields[8] = str(new_required_time)

                    is_int_job = int(fields[14]) == INT_JOB_QUEUE

                    if not is_int_job:
                        org_batch_rsc1 += int(fields[4])
                        org_batch_rsc2 += int(fields[4]) * int(fields[3])

                        res1 = int(fields[4])
                        res2 = int(fields[7])
                        assert res1 == res2
                        new_res = res1
                        if do_reduction:
                            new_res = math.ceil(res1 / SYS_SIZE_RATIO)
                        if consider_utilization:
                            new_res = int(new_res * utilzation)
                        if do_reduction_all_one:
                            new_res = 1
                        new_res = max(1, new_res)
                        fields[4] = str(new_res)
                        fields[7] = str(new_res)

                        batch_rsc1 += new_res
                        batch_rsc2 += new_res * int(fields[3])

                        key = res1
                        if key in org_distribution:
                            org_distribution[key] += 1
                        else:
                            org_distribution[key] = 1

                        key = new_res
                        if key in new_distribution:
                            new_distribution[key] += 1
                        else:
                            new_distribution[key] = 1

                        print(f'JobId: {fields[0]}, Core rsc: {res1} -> {new_res}, {new_res/res1}')

                    
                    job_count += 1
                    core_resource_count += int(fields[4])
                    cpu_resource_count += int(fields[4])*int(fields[3])
                    line = ' '.join(fields) + '\n'
                    output_file.write(line.strip() + '\n')
                    if is_int_job:
                        ijob_count += 1
                        icore_resource_count += int(fields[4])
                        icpu_resource_count += int(fields[4])*int(fields[3])
            print(f'Subtrace {i}: {ijob_count} {job_count} {ijob_count/job_count} \
{icore_resource_count} {core_resource_count} {icore_resource_count/core_resource_count} \
{icpu_resource_count} {cpu_resource_count} {icpu_resource_count/cpu_resource_count}')
            print(f'Original distribution')
            print_distirbution(org_distribution)
            print(f'New distribution')
            print_distirbution(new_distribution)
            if do_reduction or do_reduction_all_one:
                print(f'Core rsc: {org_batch_rsc1} -> {batch_rsc1}, {batch_rsc1/org_batch_rsc1} \
Core*t rsc: {org_batch_rsc2} -> {batch_rsc2}, {batch_rsc2/org_batch_rsc2}')

if __name__ == "__main__":
    main()

