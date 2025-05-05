
import glob

file_paths = glob.glob("../APF/ALPHAFOLDpipeline/usable_*_graphs.tsv")

lines = []
for file_path in file_paths:
    with open(file_path, 'r') as file:
        for line in file:
            lines.append(line.strip())

protein_list = [line.split('\t')[0] for line in lines]
with open("data/protein_list.csv", "w") as output_file:
    for protein in protein_list:
        output_file.write(f"{protein}\n")
