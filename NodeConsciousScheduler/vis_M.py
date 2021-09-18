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
ROOM = WIDTH_UNIT*80
WIDTH_UNIT=WIDTH_UNIT/RED_R
OFFSET=10
NEWLINE_THRESHOLD=27
INF = 2<<30
OUTPUT_INIT="""
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 100" preserveAspectRatio="xMinYMin">
  <defs>
    <style>
      text {
        font-family:sans-serif;
      }
      line {
        stroke-dasharray: 1;
        stroke-width: 0.5;
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

       ret = "<text text-anchor=\"end\" transform=\"translate(" + str(x) + ", " + str(y) + ")\" font-size=\"" + str(HEIGHT_UNIT) + "\">n" + str(nodenum) + "c" + str(cnum) + "</text>"
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

def output_rect(x, y, w, color, multiplicity):
   ret = "<rect style=\"fill:#" + hex(color)[2:] + "\" x=\"" + str(x) + "\" y=\"" + str(y) + "\" width=\"" + str(w) + "\" height=\"" + str(float(HEIGHT_UNIT/multiplicity)) + "\"/>"
   return ret

def output_text(x, y, jobid, multiplicity):
   ret = "<text transform=\"translate(" + str(x) + ", " + str(y) + ")\" font-size=\"" + str(HEIGHT_UNIT/multiplicity) + "\">" +  str(jobid) + "</text>"
   return ret

class Xed:
  def __init__(self, value):
    self.value = value

def output_timestamp(start_time, end_time, Y, xeds, color):
   x_st = ORIGIN_X + start_time * WIDTH_UNIT
   x_ed = ORIGIN_X + end_time * WIDTH_UNIT

   cnt = 0
   for xed in xeds:
     if x_st - xed.value >= ROOM:
       xed.value = x_ed
       break
     cnt = cnt + 1

   if cnt == len(xeds):
     xeds.append(Xed(x_ed))

   y = ORIGIN_Y + (num_node*num_core + 3 + cnt) * HEIGHT_UNIT
   
   #ret = "<text text-anchor=\"end\" transform=\"translate(" + str(x_st) + ", " + str(y) + ")\">" +  str(start_time) + "</text>"
   ret  = "<text text-anchor=\"end\"   transform=\"translate(" + str(x_st) + ", " + str(y) + ")\" stroke=\"#" + hex(color)[2:] + "\">" +  str(start_time) + "</text>"
   ret += "<text text-anchor=\"start\" transform=\"translate(" + str(x_ed) + ", " + str(y) + ")\" stroke=\"#" + hex(color)[2:] + "\">" +  str(end_time)   + "</text>"
   ret += "<line x1=\"" + str(x_st) + "\" y1=\"" + str(Y) + "\" x2=\"" + str(x_st) + "\" y2=\"" + str(y) + "\" stroke=\"#" + hex(color)[2:] + "\"/>"
   ret += "<line x1=\"" + str(x_ed) + "\" y1=\"" + str(Y) + "\" x2=\"" + str(x_ed) + "\" y2=\"" + str(y) + "\" stroke=\"#" + hex(color)[2:] + "\"/>"
   return ret, xeds


def visualize(input, num_node, num_core, multiplicity):
    output_file = init_outputfile(num_node, num_core)
    init_rand()
    xeds =[]
    xeds.append(Xed(0))

    rsc_info = [[[0 for i in range(multiplicity)] for j in range(num_core)] for k in range(num_node)]
    for ncnt in range(num_node):
        for ccnt in range(num_core):
            for mcnt in range(multiplicity):
                rsc_info[ncnt][ccnt][mcnt] = 0

    for record in input:
        ret, jobid, start_time, end_time, jnum_node, jnum_core, nodes = parse_record(record)
        
        if ret == 0:
          continue

        color = get_color(jobid)
        ppn = jnum_core/jnum_node
        Y = INF
        for node in nodes:
          nodenum = node.split()
          cores = nodenum[1:]
          nodenum = int(nodenum[0])

          Y = INF
          x = ORIGIN_X + start_time * WIDTH_UNIT
          w = (end_time - start_time) * WIDTH_UNIT

          for cnum in cores:
            cnum = int(cnum)

            lane_num = -1
            for mnum in range(multiplicity):
              if rsc_info[nodenum][cnum][mnum] <= start_time:
                rsc_info[nodenum][cnum][mnum] = end_time
                lane_num = mnum
                break
            #assert lane_num != -1
            if lane_num == -1:
              print("j" + str(jobid) + ", n" + str(nodenum) + "c" + str(cnum) )

            y = ORIGIN_Y + (nodenum*num_core + cnum) * HEIGHT_UNIT + lane_num * HEIGHT_UNIT/multiplicity

            rect = output_rect(x, y, w, color, multiplicity)
            text = output_text(x, y + float(OFFSET/multiplicity), jobid, multiplicity)
            #text = output_text(x, y, jobid, multiplicity)
            output_file.write(rect + "\n")
            output_file.write(text + "\n")
            Y = min(Y, y)


        time_stamp, xeds = output_timestamp(start_time, end_time, Y, xeds, color)
        output_file.write(time_stamp + "\n")

    finalize_outputfile(output_file)
    return


def main(input_file, num_node, num_core, multiplicity):
    f_input = file_open(input_file)
    ret = visualize(f_input, num_node, num_core, multiplicity)


if __name__ == "__main__" :
    argv = sys.argv
    argc = len(argv)
    if argc != 5:
      print("Must set the arguments")
      exit(1)
    else:
      input = argv[1]
      num_node = int(argv[2])
      num_core = int(argv[3])
      multiplicity = int(argv[4])
    main(input, num_node, num_core, multiplicity)
    #main("master/FCFS/short1/n4c64/test.out", "master/FCFSOC/short1/n4c32/test.out", 32)

