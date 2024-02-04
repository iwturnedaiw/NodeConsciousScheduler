import csv
import sys

#OFFSET=431217
OFFSET=0
SYS_SIZE=147*12

def read_file(file_path):
    data = []
    with open(file_path) as csvfile:
        reader = csv.reader(csvfile, delimiter='\t')
        header = next(reader)
        for row in reader:
            data.append(row)
    data_started_order = sorted(data, key=lambda x: int(x[5]))
    data_finished_order = sorted(data, key=lambda x: int(x[6]))
    def save_data_to_csv(file_path, data):
        with open(file_path, 'w', newline='') as csvfile:
            writer = csv.writer(csvfile, delimiter='\t')
            writer.writerow(header)
            writer.writerows(data)

    # Save data to CSV file
    save_data_to_csv('./output.csv', data)
    return header, data_started_order, data_finished_order


def count_rows_by_condition(data_started_order, data_finished_order, condition, started_rsc_size, finished_rsc_size, started_job_count, finished_job_count, j):
    for i in range(started_job_count, len(data_started_order)):
        if condition - 3600 <= int(data_started_order[i][5]) and int(data_started_order[i][5]) < condition:
            started_rsc_size += float(data_started_order[i][14])
            started_job_count += 1
        elif int(data_started_order[i][5]) > condition:
            break
    

    for i in range(finished_job_count, len(data_finished_order)):
        if condition - 3600 <= int(data_finished_order[i][6]) and int(data_finished_order[i][6]) < condition:
            finished_rsc_size += float(data_finished_order[i][14])
            finished_job_count += 1
        elif int(data_finished_order[i][6]) > condition:
            break
    return started_rsc_size, finished_rsc_size, started_job_count, finished_job_count, j+1

def main():
    if len(sys.argv) != 2:
        print("Usage: python script.py <file_path>")
        sys.exit(1)

    file_path = sys.argv[1]
    header, data_started_order, data_finished_order = read_file(file_path)

    time_condition = 0 - OFFSET
    started_job_count = 0
    finished_job_count = 0
    finished_rsc_size = 0
    started_rsc_size = 0
    j = -1
    while finished_job_count < len(data_finished_order):
        started_rsc_size, finished_rsc_size, started_job_count, finished_job_count, j= count_rows_by_condition(data_started_order, data_finished_order, time_condition, started_rsc_size, finished_rsc_size, started_job_count, finished_job_count, j)
        res = (started_rsc_size- finished_rsc_size)/SYS_SIZE
        print(f'{j}: {res}, {started_job_count}, {finished_job_count}')
        time_condition += 3600

if __name__ == "__main__":
    main()

