#!/usr/bin/env python
import csv
import re
import sys

COL_NUM_NODENUM=13

def file_open(file_name):
    
    with open(file_name, encoding='utf-8', newline='') as f:
        tsv_reader = csv.reader(f, delimiter='\t')
        read_data = [row for row in tsv_reader]
    return read_data;

def parse(input, num_core):
    node_num = re.search('\d+', input)
    return int(node_num[0])

def compare(master, input, num_core):
    ret = True
    if len(master) != len(input):
        return False
    for r in range(len(master)):
        r_master = master[r]
        r_input = input[r]
        for c in range(len(r_master)):
          #print(r_master[c])
          if c < COL_NUM_NODENUM:
            #print(r_master[c])
            #print(i_master[c])
            if r_master[c] != r_input[c]:
              print("Diff at row:", r, ", col:", c)
              print("<", r_master)
              print(">", r_input)
              ret = False
          elif c >= COL_NUM_NODENUM and r != 0 and r_master[c] != '':
            node_num_master  = parse(r_master[c], num_core)
            node_num_input = parse(r_input[c], num_core)
            if node_num_master != node_num_input:
              print("Diff at row:", r, ", col:", c)
              print("<", r_master)
              print(">", r_input)
              ret = False
    return ret


def main(master_file, input_file, num_core):
    f_master = file_open(master_file)
    f_input = file_open(input_file)

    ret = compare(f_master, f_input, num_core)
    if ret:
      exit(0)
    else:
      exit(1)


if __name__ == "__main__" :
    argv = sys.argv
    argc = len(argv)
    if argc != 4:
      print("Must set the arguments")
      exit(1)
    else:
      master = argv[1]
      input = argv[2]
      num_core = int(argv[3])
    main(master, input, num_core)
    #main("master/FCFS/short1/n4c64/test.out", "master/FCFSOC/short1/n4c32/test.out", 32)

