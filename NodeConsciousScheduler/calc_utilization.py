#!/usr/bin/env python
import csv
import re
import sys
import random
import datetime
import operator
import copy

CNUM_JOBID=0
CNUM_START_TIME=3
CNUM_END_TIME=4
CNUM_NUM_CORE=7
CNUM_NUM_NODE=8
CNUM_NODE_INFO=9
INF = 2<<30




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
   filename = "utilization_" + datetime.datetime.now().strftime('%Y%m%d%H%M%S') + ".out"
   output_file = open(filename, "w", encoding="utf-8")
   
   return output_file

def finalize_outputfile(output_file):
   output_file.close()
   return

class Available_core:
  def __init__(self, value):
    self.value = value

def output_utilization(urpn, urocpn, ur, uroc):
   ret = "NodeId\tutilizationRatio\tutilizationRatioOC\n"

   for i in range(len(urpn)):
     ret += str(i) + "\t" + str(urpn[i]) + "\t" + str(urocpn[i]) + "\n"

   ret += "Total\t" + str(ur) + "\t" + str(uroc) + "\n"

   return ret

class Time_slice:
  def __init__(self, start_time, end_time, available_cores, num_node):
    self.start_time = start_time
    self.end_time = end_time
    self.duration = end_time - start_time
    self.available_cores = available_cores
    self.num_node = num_node

def add_time_slice(time_slices, new_time):
    new_index = -1

    for ts in time_slices:
     if ts.start_time == new_time or ts.end_time == new_time:
       return time_slices


    for i in range(len(time_slices)):
      ts = time_slices[i]
      if ts.start_time < new_time and new_time < ts.end_time:
        new_index = i
        break

    #if new_index == -1:
    #  print(new_time)

    assert new_index != -1


    ts1 = copy.copy(time_slices[new_index])
    ts2 = copy.copy(time_slices[new_index])

    ts1.end_time = new_time
    ts1.duration = new_time - ts1.start_time

    ts2.start_time = new_time
    ts2.duration = ts2.end_time - new_time
    ts2.available_cores = copy.copy(time_slices[new_index].available_cores)

    time_slices.pop(new_index)
    time_slices.insert(new_index, ts1)
    time_slices.insert(new_index + 1, ts2)

    return time_slices

def update_available_cores(time_slices, start_time, end_time, numnode, cnum):
    for ts in time_slices:
      #print(ts.start_time, ts.end_time, [ts.available_cores[i] for i in range(num_node)])
 
      if (ts.start_time <= start_time and start_time <  ts.end_time)   or \
         (ts.start_time <  end_time   and end_time   <= ts.end_time )  or \
         (start_time < ts.start_time and ts.end_time < end_time) :
        ts.available_cores[numnode] -= cnum

      #print(ts.start_time, ts.end_time, [ts.available_cores[i] for i in range(num_node)])

    return time_slices


def calc_utilization(input, num_node, num_core):
    output_file = init_outputfile(num_node, num_core)
    available_cores =[]
    for i in range(num_node):
      available_cores.append(num_core)

    time_slices = []
    time_slices.append(Time_slice(0, INF, available_cores, num_node))


    for record in input:
      ret, jobid, start_time, end_time, jnum_node, jnum_core, nodes = parse_record(record)
        
      if ret == 0:
        continue

      time_slices = add_time_slice(time_slices, start_time)
      time_slices = add_time_slice(time_slices, end_time)

      #print(len(nodes))
      for node in nodes:
        nodenum = node.split()
        cnum = nodenum[1:]
        cnum = len(cnum)
        nodenum = int(nodenum[0])

        #print(start_time, end_time, nodenum)
        time_slices = update_available_cores(time_slices, start_time, end_time, nodenum, cnum)

    #for ts in time_slices:
      #print(ts.start_time, ts.end_time, [ts.available_cores[i] for i in range(num_node)])


    utilization_ratio_per_node = [0.0 for i in range(num_node)]
    utilization_ratio_OC_per_node = [0.0 for i in range(num_node)]

    start_time = -1
    end_time = -1
    for ts in time_slices:
      if ts.end_time == INF:
        break
      if start_time == -1:
        start_time = ts.start_time
      end_time = max(end_time, ts.end_time)
      duration = ts.duration
      for i in range(num_node):
        num_running_cores = (num_core - ts.available_cores[i])
        utilization_ratio_per_node[i] += min(num_running_cores, num_core) * duration
        utilization_ratio_OC_per_node[i] += num_running_cores * duration

    #for i in range(num_node):
      #print(utilization_ratio_per_node[i], utilization_ratio_OC_per_node[i])


    utilization_ratio = 0.0
    utilization_ratio_OC = 0.0

    duration = end_time - start_time
    for i in range(num_node):
      utilization_ratio_per_node[i] = utilization_ratio_per_node[i]/duration/num_core*100
      utilization_ratio_OC_per_node[i] = utilization_ratio_OC_per_node[i]/duration/num_core*100
      utilization_ratio += utilization_ratio_per_node[i]
      utilization_ratio_OC += utilization_ratio_OC_per_node[i]

    utilization_ratio /= num_node
    utilization_ratio_OC /= num_node

    ret = output_utilization(utilization_ratio_per_node, utilization_ratio_OC_per_node, utilization_ratio, utilization_ratio_OC)
    output_file.write(ret)

    finalize_outputfile(output_file)
    return


def main(input_file, num_node, num_core):
    f_input = file_open(input_file)
    ret = calc_utilization(f_input, num_node, num_core)


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

