from unicodedata import name
import numpy as np
import pandas as pd 
from Bio import SeqIO
from Bio.PDB.PDBParser import PDBParser
import os
import gzip

            # with gzip.open(pdb_filename, 'rt') as unzipped:
            #     structure = parser.get_structure(id_, unzipped)

def _load_cmap(filename, cmap_thresh=10.0):
        assert filename.endswith('pdb.gz')
        D, seq = load_predicted_PDB(filename, id=filename.split('/')[-1].split('.')[0])
        A = np.double(D < cmap_thresh)
            #print(A)
        S = seq2onehot(seq)
        S = S.reshape(1, *S.shape)
        A = A.reshape(1, *A.shape)

        return A, S, seq


def load_predicted_PDB(pdbfile, id=None):
    parser = PDBParser()
    with gzip.open(pdbfile, 'rt') as unzipped:
        structure = parser.get_structure(id, unzipped)
        # Extract residues
        residues = [r for r in structure.get_residues()]
        # Reset file pointer to beginning for SeqIO
        unzipped.seek(0)
        records = SeqIO.parse(unzipped, 'pdb-atom')
        seqs = [str(r.seq) for r in records]

    # Vectorized calculation of pairwise distances between CA atoms
    coords = np.array([r["CA"].get_coord() for r in residues])  # shape: (N, 3)
    diff = coords[:, np.newaxis, :] - coords[np.newaxis, :, :]
    distances = np.linalg.norm(diff, axis=-1)

    return distances, seqs[0]

def seq2onehot(seq):
    """Create 26-dim embedding"""
    chars = ['-', 'D', 'G', 'U', 'L', 'N', 'T', 'K', 'H', 'Y', 'W', 'C', 'P',
             'V', 'S', 'O', 'I', 'E', 'F', 'X', 'Q', 'A', 'B', 'Z', 'R', 'M']
    vocab_size = len(chars)
    vocab_embed = dict(zip(chars, range(vocab_size)))

    # Convert vocab to one-hot
    vocab_one_hot = np.zeros((vocab_size, vocab_size), int)
    for _, val in vocab_embed.items():
        vocab_one_hot[val, val] = 1

    embed_x = [vocab_embed[v] for v in seq]
    seqs_x = np.array([vocab_one_hot[j, :] for j in embed_x])

    return seqs_x

import sys
sys.path.append(".")
from parallelize import pqdm_map
def process_file(file_path):
    file_name = os.path.basename(file_path)
    A, S, seqres = _load_cmap(file_path, cmap_thresh=10.0)
    print(A.shape)
    print(len(A[0]))
    B = np.reshape(A, (-1, len(A[0])))
    result = []
    N = len(B)
    for i in range(N):
        for j in range(N):
            tmp1 = []
            if B[i][j] and i != j:
                tmp1.append(i)
                tmp1.append(j)
                result.append(tmp1)
    np.array(result)
    filename = file_name.split("-")
    name = filename[1]
    data = pd.DataFrame(result)
    data.to_csv(f"data/proteins_edgs/{name}.txt", sep=" ", index=False, header=False)

# Gather all pdb.gz files
file_paths = []
for path, dir_list, file_list in os.walk("../APF/AlphaFold Data/raw_data"):
    for file_name in file_list:
        if file_name.endswith('pdb.gz'):
            file_paths.append(os.path.join(path, file_name))

# Process files in parallel using pqdm_map
pqdm_map(process_file, file_paths, n_jobs=18)



    