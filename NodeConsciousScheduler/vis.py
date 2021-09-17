#!/usr/bin/env python
import csv
import re
import sys
import random
import datetime

CNUM_JOBID=0
CNUM_START_TIME=3
CNUM_END_TIME=4
CNUM_NUM_CORE=7
CNUM_NUM_NODE=8
CNUM_NODE_INFO=9
BIT_24 = 16777216
SEED = 100
ORIGIN_X = 20
ORIGIN_Y = 0
WIDTH_UNIT = 1
HEIGHT_UNIT = 10
RED_R = 100
WIDTH_UNIT=WIDTH_UNIT/RED_R
OFFSET=10
OUTPUT_INIT="""
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 10" preserveAspectRatio="xMinYMin">
  <defs>
    <style>
      text {
        font-size:10px;
        font-family:sans-serif;
      }
      line {
        stroke: black;
        stroke-dasharray: 1;
        stroke-width: 0.1;
      }
    </style>
  </defs>
  <title></title>
"""
OUTPUT_FINAL="</svg>"


def file_open(file_name):
    with open(file_name, encoding='utf-8', newline='') as f:
        tsv_reader = csv.reader(f, delimiter='\t')
        read_data = [row for row in tsv_reader]
    return read_data;

def parse_record(input):
    jobid = input[CNUM_JOBID]
    if jobid == "JobID":
      return 0, -1, -1, -1, -1, -1, -1
    start_time = int(input[CNUM_START_TIME])
    end_time = int(input[CNUM_END_TIME])
    num_node = int(input[CNUM_NUM_NODE])
    num_core = int(input[CNUM_NUM_CORE])
    cnt = 0
    nodes=[]
    for cnum in range(CNUM_NODE_INFO, CNUM_NODE_INFO + num_node):
      #print(cnum)
      tmp = input[cnum]
      tmp = tmp.replace('(', ' ')
      tmp = tmp.replace(')', ' ')
      tmp = tmp.replace(',', ' ')
      nodes.append(tmp)

    return 1, jobid, start_time, end_time, num_node, num_core, nodes

def init_outputfile(num_node, num_core):
   filename = "visualize_" + datetime.datetime.now().strftime('%Y%m%d%H%M%S') + ".svg"
   output_file = open(filename, "w", encoding="utf-8")
   output_file.write(OUTPUT_INIT + "\n")
   for nodenum in range(num_node):
     for cnum in range(num_core):
       y = ORIGIN_Y + (nodenum*num_core + cnum) * HEIGHT_UNIT - 1 + OFFSET
       x = ORIGIN_X 

       ret = "<text text-anchor=\"end\" transform=\"translate(" + str(x) + ", " + str(y) + ")\">n" + str(nodenum) + "c" + str(cnum) + "</text>"
       output_file.write(ret + "\n")
   
   return output_file

def finalize_outputfile(output_file):
   output_file.write(OUTPUT_FINAL + "\n")
   output_file.close()
   return

def init_rand():
   random.seed(SEED)
   return

def get_color(jobid):
   random.seed(jobid)
   return random.randint(0, BIT_24)

def output_rect(x, y, w, color):
   ret = "<rect style=\"fill:#" + hex(color)[2:] + "\" x=\"" + str(x) + "\" y=\"" + str(y) + "\" width=\"" + str(w) + "\" height=\"" + str(HEIGHT_UNIT) + "\"/>"
   return ret

def output_text(x, y, jobid):
   ret = "<text transform=\"translate(" + str(x) + ", " + str(y) + ")\">" +  str(jobid) + "</text>"
   return ret

def output_timestamp(start_time, end_time):
   y = ORIGIN_Y + (num_node*num_core + 3) * HEIGHT_UNIT
   x_st = ORIGIN_X + start_time * WIDTH_UNIT
   x_ed = ORIGIN_X + end_time * WIDTH_UNIT
   
   ret = "<text transform=\"translate(" + str(x_st) + ", " + str(y) + ")\">" +  str(start_time) + "</text>"
   ret += "<text text-anchor=\"end\" transform=\"translate(" + str(x_ed) + ", " + str(y) + ")\">" +  str(end_time) + "</text>"
   return ret

def visualize(input, num_node, num_core):
    output_file = init_outputfile(num_node, num_core)
    init_rand()

    for record in input:
        ret, jobid, start_time, end_time, jnum_node, jnum_core, nodes = parse_record(record)
        
        if ret == 0:
          continue

        color = get_color(jobid)
        ppn = jnum_core/jnum_node
        for node in nodes:
          nodenum = node.split()
          cores = nodenum[1:]
          nodenum = int(nodenum[0])

          for cnum in cores:
            cnum = int(cnum)
            y = ORIGIN_Y + (nodenum*num_core + cnum) * HEIGHT_UNIT
            x = ORIGIN_X + start_time * WIDTH_UNIT
            w = (end_time - start_time) * WIDTH_UNIT

            rect = output_rect(x, y, w, color)
            text = output_text(x, y + OFFSET, jobid)
            output_file.write(rect + "\n")
            output_file.write(text + "\n")

        time_stamp = output_timestamp(start_time, end_time)
        output_file.write(time_stamp + "\n")

    finalize_outputfile(output_file)
    return


def main(input_file, num_node, num_core):
    f_input = file_open(input_file)
    ret = visualize(f_input, num_node, num_core)


if __name__ == "__main__" :
    argv = sys.argv
    argc = len(argv)
    if argc != 4:
      print("Must set the arguments")
      exit(1)
    else:
      input = argv[1]
      num_node = int(argv[2])
      num_core = int(argv[3])
    main(input, num_node, num_core)
    #main("master/FCFS/short1/n4c64/test.out", "master/FCFSOC/short1/n4c32/test.out", 32)

