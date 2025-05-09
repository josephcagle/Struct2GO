import os
import numpy as np
import pandas as pd
import networkx as nx
from itertools import islice
import random
import uuid
from pathlib import Path
import sys
sys.path.append('.')
from parallelize import pqdm_map

# Load edge list with flexible delimiter and column indices
def load_graph(edge_file, delimiter=' ', src_index=0, dst_index=1, weight_index=2, is_weighted=False):
    edges = pd.read_csv(edge_file, sep=delimiter, header=None, usecols=[src_index, dst_index] + ([weight_index] if is_weighted else []))
    G = nx.Graph()
    for _, row in edges.iterrows():
        src, dst = str(row[src_index]), str(row[dst_index])  # Convert nodes to strings
        if is_weighted:
            G.add_edge(src, dst, weight=row[weight_index])
        else:
            G.add_edge(src, dst)
    return G

# Node2Vec random walk implementation
def node2vec_walk(G, start_node, walk_length, p, q):
    walk = [start_node]
    for _ in range(walk_length - 1):
        curr = walk[-1]
        neighbors = list(G.neighbors(curr))
        if not neighbors:
            break
        
        # Calculate transition probabilities
        if len(walk) == 1:
            next_node = random.choice(neighbors)
        else:
            prev = walk[-2]
            probs = []
            for neighbor in neighbors:
                if neighbor == prev:
                    probs.append(1/p)  # Return to previous node
                elif G.has_edge(prev, neighbor):
                    probs.append(1)    # In-neighbor
                else:
                    probs.append(1/q)  # Out-neighbor
            probs = np.array(probs) / np.sum(probs)
            next_node = np.random.choice(neighbors, p=probs)
        
        walk.append(next_node)
    return walk

# Generate all random walks
def generate_walks(G, walk_length, p, q, num_walks_per_node=1):
    walks = []
    nodes = list(G.nodes())
    for _ in range(num_walks_per_node):
        random.shuffle(nodes)
        for node in nodes:
            walk = node2vec_walk(G, node, walk_length, p, q)
            walks.append([str(n) for n in walk])
    return walks

def parallel_process_edge_file(args_data):
    edge_file, args_data = args_data
    # print(f"Processing {edge_file.name}")

    walk_length = args_data["walk_length"]
    p = args_data["p"]
    q = args_data["q"]
    output_dir = args_data["output_dir"]

    # Load graph
    G = load_graph(edge_file)

    # Generate walks
    walks = generate_walks(G, walk_length, p, q)

    # Save random walks
    output_file = Path(output_dir) / edge_file.stem
    output_file = str(output_file) + ".txt"
    with open(output_file, 'w') as f:
        for walk in walks:
            f.write(' '.join(walk) + '\n')

    # print(f"Random walks saved to {output_file}")

# Process edge list directory and save random walks
def process_protein_graphs(input_dir, output_dir, walk_length=30, p=0.8, q=1.2, batch_size=128, epoch_num=1, is_weighted=False, delimiter=' ', src_index=0, dst_index=1, weight_index=2):
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    args_data = {
        "input_dir": input_dir,
        "output_dir": output_dir,
        "walk_length": walk_length,
        "p": p,
        "q": q,
        "batch_size": batch_size,
        "epoch_num": epoch_num,
        "is_weighted": is_weighted,
        "delimiter": delimiter,
        "src_index": src_index,
        "dst_index": dst_index,
        "weight_index": weight_index
    }
    
    edge_files = list(Path(input_dir).glob('*.txt'))

    # Filter out ids not in data/protein_list.csv
    protein_list = pd.read_csv("data/protein_list.csv", sep=" ")
    protein_list = set(protein_list["pdb_id"])
    edge_files = [edge_file for edge_file in edge_files if edge_file.stem in protein_list]

    edge_files = list(zip(edge_files, [args_data] * len(edge_files)))
    pqdm_map(parallel_process_edge_file, edge_files, n_jobs=18)
    # for edge_file in edge_files:
    #     parallel_process_edge_file(edge_file, args_data)

if __name__ == "__main__":
    # Example usage
    input_dir = "data/proteins_edgs"
    # output_dir = "data/node2vec"
    output_dir = "data/struct_feature"
    process_protein_graphs(
        input_dir=input_dir,
        output_dir=output_dir,
        walk_length=30,
        p=0.8,
        q=1.2,
        batch_size=128,
        epoch_num=1,
        is_weighted=False,
        delimiter=' ',
        src_index=0,
        dst_index=1,
        weight_index=2
    )