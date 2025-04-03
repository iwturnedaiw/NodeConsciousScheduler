import csv
import sys

OFFSET=431217
offset = 3600 - (OFFSET % 3600)
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
    data_arrival_order = sorted(data, key=lambda x: int(x[3]))
    return header, data_started_order, data_finished_order, data_arrival_order

def count_rows(data_started_order, data_finished_order, data_arrival_order, array_size):
    started_resource = [0] * int(array_size)
    finished_resource = [0] * int(array_size)
    arrival_resource = [0] * int(array_size)

    for i in range(0, len(data_started_order)):
        start_time_offset = int(data_started_order[i][5]) + offset
        index = int(start_time_offset/3600) + 1
        if start_time_offset % 3600 == 0:
            index -= 1
        started_resource[index] += float(data_started_order[i][14])
    
    for i in range(0, len(started_resource) - 1):
        started_resource[i + 1] += started_resource[i]

    for i in range(0, len(data_finished_order)):
        finished_time_offset = int(data_finished_order[i][6]) + offset
        index = int(finished_time_offset/3600) + 1
        if finished_time_offset % 3600 == 0:
            index -= 1
        finished_resource[index] += float(data_finished_order[i][14])

    for i in range(0, len(finished_resource) - 1):
        finished_resource[i + 1] += finished_resource[i]

    for i in range(0, len(data_arrival_order)):
        arrival_time_offset = int(data_started_order[i][3]) + offset
        print("arrival_time_offset: " + str(arrival_time_offset))
        index = int(arrival_time_offset/3600) + 1
        if arrival_time_offset % 3600 == 0:
            index -= 1
        arrival_resource[index] += float(data_arrival_order[i][14])

    for i in range(0, len(arrival_resource) - 1):
        arrival_resource[i + 1] += arrival_resource[i]

    return started_resource, finished_resource, arrival_resource


def main():
    if len(sys.argv) != 2:
        print("Usage: python script.py <file_path>")
        sys.exit(1)

    file_path = sys.argv[1]
    header, data_started_order, data_finished_order, data_arrival_order = read_file(file_path)

    time_condition = 0
    job_id = int(data_finished_order[-1][0])
    last_element = int(data_finished_order[-1][6])
    last_element_offset = last_element + offset
    array_size = int(last_element_offset/3600) + 2
    if last_element_offset % 3600 == 0:
        array_size -= 1
    print("array_size: " + str(array_size))

    started_resource, finished_resource, arrival_resource = count_rows(data_started_order, data_finished_order, data_arrival_order, array_size)
    with open('output.csv', 'w', newline='') as csvfile:
        csvfile = csv.writer(csvfile, delimiter=',', quotechar='|', quoting=csv.QUOTE_MINIMAL)
        for i in range(int(array_size)):

            arrival_value = 0
            if i > 0:
                arrival_value = arrival_resource[i - 1]
            arrival_rsc = (arrival_resource[i] - arrival_value)
            arrival = arrival_rsc/SYS_SIZE
            executing = (started_resource[i] - finished_resource[i])/SYS_SIZE
            waiting = (arrival_resource[i] - started_resource[i])/SYS_SIZE
            csvfile.writerow([i, arrival, waiting, executing])


if __name__ == "__main__":
    main()

