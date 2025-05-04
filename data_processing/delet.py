from asyncore import read
import os
import numpy as np
import pandas as pd 

protein_list = pd.read_csv('data/protein_list.csv', sep=' ')

for path,dir_list,file_list in os.walk("data/proteins_edgs"):
    for file in file_list:
        name = file.split('.')[0]
        if name in protein_list:
            os.remove(os.path.join(path, file))